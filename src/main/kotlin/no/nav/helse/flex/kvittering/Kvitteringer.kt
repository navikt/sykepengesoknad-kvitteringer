package no.nav.helse.flex.kvittering

import no.nav.helse.flex.no.nav.helse.flex.bildeprosessering.Bilde
import no.nav.helse.flex.no.nav.helse.flex.bildeprosessering.Bildeprosessering
import no.nav.helse.flex.no.nav.helse.flex.bucket.BucketClient
import no.nav.helse.flex.no.nav.helse.flex.bucket.BucketClient.BlobContent
import org.springframework.http.MediaType
import org.springframework.stereotype.Service

@Service
class Kvitteringer(
    private val bucketClient: BucketClient,
    private val bildeprosessering: Bildeprosessering,
) {

    fun lagreKvittering(fnr: String, navn: String, mediaType: MediaType, blobContent: ByteArray) {
        val prosessertBilde = bildeprosessering.prosesserBilde(Bilde(mediaType, blobContent))

        bucketClient.lagreBlob(
            blobName = navn,
            contentType = prosessertBilde!!.contentType.toString(),
            metadata = mapOf("fnr" to fnr),
            content = prosessertBilde.bytes
        )
    }

    fun hentKvittering(fnr: String, blobNavn: String): Kvittering? {
        return bucketClient.hentBlob(blobNavn)?.let {
            validerBruker(fnr, it.metadata, blobNavn)

            return Kvittering(
                filNavn = "kvittering-$blobNavn.${it.filType()}",
                contentType = it.metadata["content-type"]!!,
                byteArray = it.blob.getContent(),
            )
        }
    }

    private fun BlobContent.filType(): String {
        return metadata["content-type"]!!.split("/")[1]
    }

    private fun validerBruker(fnr: String, blobMetadata: Map<String, String>, blobNavn: String) {
        if (fnr != blobMetadata["fnr"]!!) {
            throw IllegalAccessException("Kvittering $blobNavn er forsøkt hentet av feil bruker.")
        }
    }
}

class Kvittering(
    val filNavn: String,
    val contentType: String,
    val byteArray: ByteArray,
    val contentSize: Long = byteArray.size.toLong()
)
