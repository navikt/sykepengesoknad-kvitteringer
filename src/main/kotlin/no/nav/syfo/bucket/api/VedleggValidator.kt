package no.nav.syfo.bucket.api

import io.ktor.http.content.PartData
import io.ktor.http.content.streamProvider
import no.nav.syfo.log
import org.apache.tika.config.TikaConfig
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
    private val tika = TikaConfig()

    fun validerVedlegg(vedlegg: PartData.FileItem): Boolean {
        if (vedlegg.erForStort()) {
            log.warn("Vedlegg er for stort")
            return false
        }

        if (!vedlegg.erTillattFiltype()) {
            log.warn("Vedlegg er ikke av tillatt filtype")
            return false
        }
        return true
    }

    private fun PartData.FileItem.erForStort(): Boolean {
        val file = File(this.originalFileName)
        this.streamProvider().use { input -> file.outputStream().buffered().use { output -> input.copyToSuspend(output) } }
        val size = file.length()
        log.info("Prøver å laste opp fil ${this.originalFileName} med ${bytesTilFilstørrelse(size)} når maksstørrelse er ${bytesTilFilstørrelse(maksFilStørrelse)}")

        val file2 = File(this.originalFileName)
        file2.writeBytes(this.streamProvider().buffered().readAllBytes())
        val size2 = file.totalSpace
        log.info("Prøver å laste opp fil2 ${this.originalFileName} med ${bytesTilFilstørrelse(size2)} når maksstørrelse er ${bytesTilFilstørrelse(maksFilStørrelse)}")
        return size > maksFilStørrelse
    }

    private fun PartData.FileItem.erTillattFiltype(): Boolean {
        val type = tika.detector.detect(this.streamProvider().buffered(), Metadata())
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
