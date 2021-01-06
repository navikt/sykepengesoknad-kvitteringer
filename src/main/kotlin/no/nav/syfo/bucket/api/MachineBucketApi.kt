package no.nav.syfo.bucket.api

import com.google.cloud.storage.Bucket
import com.google.cloud.storage.Storage
import io.ktor.application.call
import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.response.header
import io.ktor.response.respond
import io.ktor.response.respondBytes
import io.ktor.routing.Route
import io.ktor.routing.get
import no.nav.syfo.Environment
import no.nav.syfo.log
import java.io.File

fun Route.setupMachineBucketApi(storage: Storage, env: Environment) {
    get("/maskin/kvittering/{blobname}") {
        val bucket: Bucket? = storage.get(env.bucketName)
        if (bucket == null) {
            call.respond("Bucket $env.bucketName does not exist.")
            return@get
        }

        val blobName = call.parameters["blobName"]!!
        val blob = bucket.get(blobName)
        val kvittering = File(blobName)
        kvittering.writeBytes(blob.getContent())
        val kvitteringNavn = "$blobName.${blob.metadata?.get("content-type")!!.split("/")[1]}"
        call.response.header(
            HttpHeaders.ContentDisposition,
            ContentDisposition.Attachment.withParameter(
                ContentDisposition.Parameters.FileName,
                kvitteringNavn
            ).toString()
        )
        call.respondBytes(kvittering.readBytes(), contentType = ContentType.parse(blob.metadata?.get("content-type")!!))
        log.info("Returnerer $kvitteringNavn (content-type: ${blob.metadata?.get("content-type")})")
    }
}
