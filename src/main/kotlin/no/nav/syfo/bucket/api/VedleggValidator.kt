package no.nav.syfo.bucket.api

import io.ktor.http.content.PartData
import io.ktor.http.content.streamProvider
import no.nav.syfo.log
import org.apache.tika.config.TikaConfig
import org.apache.tika.metadata.Metadata
import org.apache.tika.mime.MediaType

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

    private fun PartData.FileItem.erForStort(maksFilStørrelse: Long): Boolean = false

    private fun PartData.FileItem.erTillattFiltype(tillatteFiltyper: List<MediaType>): Boolean {
        val type = tika.detector.detect(this.streamProvider().buffered(), Metadata())
        log.info("Tika detekterer typen til å være $type")
        log.info("Tillatte filtyper er $tillatteFiltyper")
        return tillatteFiltyper.contains(type)
    }
}
