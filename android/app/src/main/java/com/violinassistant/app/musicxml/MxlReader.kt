package com.violinassistant.app.musicxml

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

object MxlReader {
    fun extractXml(inputStream: InputStream): String {
        val bytes = inputStream.readBytes()
        var rootFilePath: String? = null

        // Pass 1: Look for container.xml
        ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (entry.name == "META-INF/container.xml") {
                    val containerXml = zis.bufferedReader(Charsets.UTF_8).readText()
                    val match = Regex("""full-path="([^"]+)"""").find(containerXml)
                    if (match != null) {
                        rootFilePath = match.groupValues[1]
                    }
                    break
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        // Pass 2: Extract root file or first valid XML
        ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val name = entry.name
                val isTarget = if (rootFilePath != null) {
                    name == rootFilePath
                } else {
                    (name.endsWith(".xml") || name.endsWith(".musicxml")) && !name.startsWith("META-INF")
                }
                if (isTarget) {
                    return zis.bufferedReader(Charsets.UTF_8).readText()
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        throw IllegalArgumentException("فایل معتبر MusicXML در بسته MXL یافت نشد.")
    }
}
