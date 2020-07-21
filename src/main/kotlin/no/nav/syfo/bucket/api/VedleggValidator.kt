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

class VedleggValidator(
    val maksFilStørrelse: Long = 1024 * 1024,
    val tillatteFiltyper: List<MediaType> = listOf(
        MediaType.image("jpeg"),
        MediaType.image("png")
    )
) {
    private val tika = TikaConfig()

    fun validerVedlegg(vedlegg: PartData.FileItem): Boolean {
        if (vedlegg.erForStort(maksFilStørrelse)) {
            log.warn("Vedlegg er for stort")
            return false
        }

        if (!vedlegg.erTillattFiltype(tillatteFiltyper)) {
            log.warn("Vedlegg er ikke av tillatt filtype")
            return false
        }
        return true
    }

    private fun PartData.FileItem.erForStort(maksFilStørrelse: Long): Boolean {
        val file = File(this.originalFileName)
        this.streamProvider().use { input -> file.outputStream().buffered().use { output -> input.copyToSuspend(output) } }
        val size = file.totalSpace
        log.info("Prøver å laste opp fil med $size når maksstørrelse er $maksFilStørrelse")
        return size > maksFilStørrelse
    }

    private fun PartData.FileItem.erTillattFiltype(tillatteFiltyper: List<MediaType>): Boolean {
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
