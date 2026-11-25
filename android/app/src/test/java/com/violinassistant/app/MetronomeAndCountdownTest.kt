package com.violinassistant.app

import com.violinassistant.app.audio.CountdownPhase
import com.violinassistant.app.audio.MetronomeAudioSink
import com.violinassistant.app.audio.MetronomeEngine
import com.violinassistant.app.musicxml.MusicXmlParser
import com.violinassistant.app.practice.PracticeAudioCoordinator
import org.junit.Assert.*
import org.junit.Test
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

private class FakeMetronomeSink(
    override val sampleRate: Int = 44100,
    private val capturePcm: Boolean = false
) : MetronomeAudioSink {
    var totalWrittenFrames: Long = 0L
    var nonZeroSamplesWritten: Long = 0L
    var closed = false
    val recorded = mutableListOf<Short>()

    override fun open(): Boolean = true

    override fun writeSamples(buffer: ShortArray, count: Int) {
        totalWrittenFrames += count
        for (i in 0 until count) {
            if (buffer[i].toInt() != 0) nonZeroSamplesWritten++
            if (capturePcm) recorded.add(buffer[i])
        }
        Thread.sleep(1)
    }

    override fun playbackHeadFrames(): Long = totalWrittenFrames

    override fun close() {
        closed = true
    }
}

class MetronomeAndCountdownTest {

    @Test
    fun `parser reads sound tempo and metronome per-minute fallback`() {
        val xmlMetronomeOnly = """
            <?xml version="1.0" encoding="UTF-8"?>
            <score-partwise version="3.1">
              <part-list><score-part id="P1"><part-name>Vln</part-name></score-part></part-list>
              <part id="P1">
                <measure number="1">
                  <attributes><divisions>1</divisions><time><beats>3</beats><beat-type>4</beat-type></time></attributes>
                  <direction><direction-type><metronome><beat-unit>quarter</beat-unit><per-minute>66</per-minute></metronome></direction-type></direction>
                  <note><pitch><step>A</step><octave>4</octave></pitch><duration>1</duration><voice>1</voice></note>
                </measure>
              </part>
            </score-partwise>
        """.trimIndent()

        val parsed = MusicXmlParser.parse(xmlMetronomeOnly)
        assertEquals(66f, parsed.tempo, 0.01f)
        assertEquals(3, parsed.timeSignature.beats)
    }

    @Test
    fun `metronome steps 3-2-1-Go at score tempo before invoking countdown Go callback`() {
        val sink = FakeMetronomeSink(sampleRate = 44100)
        val engine = MetronomeEngine { sink }
        val phases = Collections.synchronizedList(mutableListOf<CountdownPhase>())
        val goLatch = CountDownLatch(1)

        engine.start(tempoBpm = 120f, beatsPerMeasure = 4, withCountdown = true) {
            phases.add(CountdownPhase.GO)
            goLatch.countDown()
        }

        // Collect transitions until Go triggers
        val deadline = System.currentTimeMillis() + 3000L
        while (goLatch.count > 0 && System.currentTimeMillis() < deadline) {
            val cur = engine.state.value.countdownPhase
            if (phases.lastOrNull() != cur && cur != CountdownPhase.IDLE) {
                phases.add(cur)
            }
            Thread.sleep(2)
        }

        assertTrue("Expected onCountdownGo callback to fire", goLatch.await(1, TimeUnit.SECONDS))
        // At 120 BPM (22050 frames/beat), 3 countdown beats span ~66150 frames before Go click on beat 3
        assertTrue(
            "Expected >= 66000 frames (3 beats at 120 BPM) before Go, wrote ${sink.totalWrittenFrames}",
            sink.totalWrittenFrames >= 65500L
        )
        assertTrue("Expected audible woodblock PCM samples written", sink.nonZeroSamplesWritten > 1000L)
        assertTrue(phases.contains(CountdownPhase.COUNT_3))

        engine.stop()
        assertTrue(sink.closed)
        assertEquals(CountdownPhase.IDLE, engine.state.value.countdownPhase)
    }

    @Test
    fun `stopping during countdown cancels Go callback cleanly`() {
        val sink = FakeMetronomeSink(sampleRate = 44100)
        val engine = MetronomeEngine { sink }
        var goFired = false

        engine.start(tempoBpm = 40f, beatsPerMeasure = 4, withCountdown = true) {
            goFired = true
        }
        Thread.sleep(20)
        engine.stop()

        assertFalse("Go must not fire if paused during 3-2-1 countdown", goFired)
        assertFalse(engine.state.value.isPlaying)
        assertEquals(CountdownPhase.IDLE, engine.state.value.countdownPhase)
    }

    @Test
    fun `coordinator enforces single active audio feature across practice and tuner`() {
        val fakeSink = FakeMetronomeSink(sampleRate = 44100)
        var listeningStarted = 0
        var listeningStopped = 0
        val coordinator = PracticeAudioCoordinator(
            metronomeEngine = MetronomeEngine { fakeSink },
            onStartListening = { listeningStarted++ },
            onStopListening = { listeningStopped++ }
        )

        coordinator.startPracticeWithCountdown(tempoBpm = 180f, beatsPerMeasure = 4)
        assertTrue(coordinator.isPracticing.value)

        // Switching to Tuner tab must stop practice & metronome
        coordinator.setTunerActive(active = true, hasMicPermission = false)
        assertFalse(coordinator.isPracticing.value)
        assertTrue(listeningStopped >= 1)
        coordinator.releaseAll()
    }

    @Test
    fun `every beat renders complete untruncated waveform regardless of chunk phase`() {
        val sink = FakeMetronomeSink(sampleRate = 44100, capturePcm = true)
        val engine = MetronomeEngine(audioSinkFactory = { sink })
        // 113 BPM -> 23416 frames/beat (not divisible by 512 chunk size)
        engine.start(tempoBpm = 113f, beatsPerMeasure = 4, withCountdown = false) {}
        val deadline = System.currentTimeMillis() + 3000L
        while (sink.totalWrittenFrames < 105_000L && System.currentTimeMillis() < deadline) {
            Thread.sleep(5)
        }
        engine.stop()

        val framesPerBeat = 23416 // (44100 * 60 / 113)
        val waveLen = 794
        val beat0Accent = sink.recorded.subList(0, waveLen)
        val beat1Reg = sink.recorded.subList(framesPerBeat, framesPerBeat + waveLen)
        val beat2Reg = sink.recorded.subList(framesPerBeat * 2, framesPerBeat * 2 + waveLen)
        val beat4Accent = sink.recorded.subList(framesPerBeat * 4, framesPerBeat * 4 + waveLen)

        assertEquals("Regular beat 1 & 2 must be bit-for-bit identical", beat1Reg, beat2Reg)
        assertEquals("Accent beat 0 & 4 must be bit-for-bit identical", beat0Accent, beat4Accent)
        assertNotEquals("Accent & regular click must remain distinct", beat0Accent, beat1Reg)
    }
}
