package no.nav.helse.flex.no.nav.helse.flex.bucket

import com.google.cloud.storage.Blob
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Storage
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.stereotype.Component

@Component
class BucketClient(
    @Value("\${BUCKET_NAME}")
    private val bucketName: String,
    private val storage: Storage,
) {

    fun lagreBlob(blobName: String, contentType: MediaType, metadata: Map<String, String>, content: ByteArray) {
        val contentTypeVerdi = contentType.toString()
        val blobInfo = BlobInfo.newBuilder(bucketName, blobName)
            .setContentType(contentTypeVerdi)
            .setMetadata(metadata + mapOf("content-type" to contentTypeVerdi))
            .build()

        storage.create(blobInfo, content)
    }

    fun hentBlob(blobName: String): BlobContent? {
        return storage.get(bucketName, blobName)?.let {
            return BlobContent(metadata = it.metadata, blob = it)
        }
    }

    fun slettBlob(blobName: String): Boolean {
        return storage.delete(bucketName, blobName)
    }

    data class BlobContent(
        val metadata: Map<String, String>,
        val blob: Blob,
    )
}
