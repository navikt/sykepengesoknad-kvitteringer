package no.nav.syfo.bucket.api

import com.google.cloud.storage.Bucket
import com.google.cloud.storage.Storage
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.header
import io.ktor.response.respondBytes
import io.ktor.routing.Route
import io.ktor.routing.get
import no.nav.syfo.Environment
import no.nav.syfo.log
import no.nav.syfo.models.jsonStatus

fun Route.setupMachineBucketApi(storage: Storage, env: Environment) {
    get("/maskin/kvittering/{blobname}") {
        val bucket: Bucket = storage.get(env.bucketName) ?: throw RuntimeException("Bucket $env.bucketName finnes ikke")

        val blobName = call.parameters["blobName"]!!
        val blob = bucket.get(blobName)
        val bytes = blob.getContent()
        val kvitteringNavn = "$blobName.${blob.metadata?.get("content-type")!!.split("/")[1]}"

        log.info("Returnerer $kvitteringNavn (content-type: ${blob.metadata?.get("content-type")})")
        call.response.header(
            HttpHeaders.ContentDisposition,
            ContentDisposition.Attachment.withParameter(
                ContentDisposition.Parameters.FileName,
                kvitteringNavn
            ).toString()
        )
        call.respondBytes(bytes, contentType = ContentType.parse(blob.metadata?.get("content-type")!!))
    }

    get("/maskin/slett/{blobname}") {
        val blobName = call.parameters["blobName"]!!
        val ret = storage.delete(env.bucketName, blobName)
        if (!ret) {
            log.error("$blobName ble ikke funnet, og kan dermed ikke slettes")
            call.jsonStatus(HttpStatusCode.NotFound, blobName, "ble ikke funnet, og kan dermed ikke slettes")
            return@get
        }
        call.jsonStatus(id = blobName, message = "slettet")
    }
}
