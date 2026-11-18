package com.violinassistant.app.musicxml.sample

val SAMPLE_MORGH_E_SAHAR_XML = """
<?xml version="1.0" encoding="UTF-8"?>
<score-partwise version="3.1">
  <work><work-title>مرغ سحر</work-title></work>
  <identification><creator type="composer">مرتضی نی‌داوود</creator></identification>
  <part-list><score-part id="P1"><part-name>Violin</part-name></score-part></part-list>
  <part id="P1">
    <measure number="1">
      <attributes>
        <divisions>2</divisions>
        <key><fifths>0</fifths></key>
        <time><beats>3</beats><beat-type>4</beat-type></time>
        <clef><sign>G</sign><line>2</line></clef>
      </attributes>
      <direction placement="above"><sound tempo="76"/></direction>
      <note><pitch><step>A</step><octave>4</octave></pitch><duration>2</duration><type>quarter</type></note>
      <note><pitch><step>A</step><octave>4</octave></pitch><duration>2</duration><type>quarter</type></note>
      <note><pitch><step>A</step><octave>4</octave></pitch><duration>2</duration><type>quarter</type></note>
    </measure>
    <measure number="2">
      <note><pitch><step>B</step><octave>4</octave></pitch><duration>2</duration><type>quarter</type></note>
      <note><pitch><step>C</step><octave>5</octave></pitch><duration>2</duration><type>quarter</type></note>
      <note><pitch><step>B</step><octave>4</octave></pitch><duration>2</duration><type>quarter</type></note>
    </measure>
    <measure number="3">
      <note><pitch><step>A</step><octave>4</octave></pitch><duration>4</duration><type>half</type></note>
      <note><pitch><step>G</step><octave>4</octave></pitch><duration>2</duration><type>quarter</type></note>
    </measure>
    <measure number="4">
      <note><pitch><step>A</step><octave>4</octave></pitch><duration>6</duration><type>half</type><dot/></note>
    </measure>
  </part>
</score-partwise>
""".trimIndent()
