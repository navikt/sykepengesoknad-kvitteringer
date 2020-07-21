package no.nav.syfo.bucket.api

import io.ktor.http.content.PartData
import io.ktor.http.content.streamProvider
import no.nav.syfo.log
import org.apache.tika.Tika
import org.apache.tika.metadata.Metadata
import org.apache.tika.mime.MediaType
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.text.CharacterIterator
import java.text.StringCharacterIterator

class VedleggValidator(
    val maksFilStørrelse: Long = 1024 * 1024 * 50,
    val tillatteFiltyper: List<MediaType> = listOf(
        MediaType.image("jpeg"),
        MediaType.image("png")
    )
) {
    private val tika = Tika()

    fun valider(part: PartData.FileItem): Boolean {
        val file = File(part.originalFileName)
        part.streamProvider().use { input -> file.outputStream().buffered().use { output -> input.copyToSuspend(output) } }

        if (!erTillattFilstørrelse(file)) {
            log.warn("Vedlegg er for stort")
            return false
        }

        if (!erTillattFiltype(file)) {
            log.warn("Vedlegg er ikke av tillatt filtype")
            return false
        }
        return true
    }

    fun erTillattFilstørrelse(file: File): Boolean {
        log.info(
            "Prøver å laste opp fil ${file.name} " +
                "med ${bytesTilFilstørrelse(file.length())} " +
                "når maksstørrelse er ${bytesTilFilstørrelse(maksFilStørrelse)}"
        )
        return file.length() <= maksFilStørrelse
    }

    fun erTillattFiltype(file: File): Boolean {
        val type = tika.detector.detect(file.inputStream().buffered(), Metadata())
        log.info("Tika detekterer typen til å være $type")
        log.info("Tillatte filtyper er $tillatteFiltyper")
        return tillatteFiltyper.contains(type)
    }
}

fun InputStream.copyToSuspend(
    out: OutputStream,
    bufferSize: Int = DEFAULT_BUFFER_SIZE
): Long {
    val buffer = ByteArray(bufferSize)
    var bytesCopied = 0L
    while (true) {
        val bytes = read(buffer).takeIf { it >= 0 } ?: break
        out.write(buffer, 0, bytes)
        bytesCopied += bytes
    }
    return bytesCopied
}

fun bytesTilFilstørrelse(bytes: Long): String? {
    val absB = if (bytes == Long.MIN_VALUE) Long.MAX_VALUE else Math.abs(bytes)
    if (absB < 1024) {
        return "$bytes B"
    }
    var value = absB
    val ci: CharacterIterator = StringCharacterIterator("KMGTPE")
    var i = 40
    while (i >= 0 && absB > 0xfffccccccccccccL shr i) {
        value = value shr 10
        ci.next()
        i -= 10
    }
    value *= java.lang.Long.signum(bytes).toLong()
    return String.format("%.1f %ciB", value / 1024.0, ci.current())
}
