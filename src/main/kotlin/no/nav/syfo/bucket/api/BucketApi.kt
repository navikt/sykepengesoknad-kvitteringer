package no.nav.syfo.bucket.api

import com.google.cloud.storage.Bucket
import com.google.cloud.storage.Storage
import io.ktor.application.call
import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.TextContent
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.http.withCharset
import io.ktor.request.receiveMultipart
import io.ktor.response.header
import io.ktor.response.respond
import io.ktor.response.respondBytes
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import no.nav.syfo.Environment
import no.nav.syfo.log
import no.nav.syfo.models.VedleggRespons
import no.nav.syfo.models.toJson
import java.io.File
import java.util.UUID

fun Route.setupBucketApi(storage: Storage, env: Environment) {
    get("/list") {
        val bucket: Bucket? = storage.get(env.bucketName)
        if (bucket != null) {
            call.respond(
                bucket.list().iterateAll().joinToString(separator = "\n") { blob ->
                    "${blob.name} (content-type: ${blob.contentType}, size: ${blob.size})"
                }
            )
        } else {
            call.respond("Bucket $env.bucketName does not exist.")
        }
    }

    get("/kvittering/{blobName}") {
        val bucket: Bucket? = storage.get(env.bucketName)
        if (bucket != null) {
            val blobName = call.parameters["blobName"]
            log.info("Attempting to query blob with name $blobName")
            val blob = bucket.get(blobName)
            log.info("Found blob $blobName (content-type: ${blob.contentType})")
            val filNavn = "kvittering-$blobName"
            val kvittering = File(filNavn)
            kvittering.writeBytes(blob.getContent())
            call.response.header(
                HttpHeaders.ContentDisposition,
                ContentDisposition.Attachment.withParameter(
                    ContentDisposition.Parameters.FileName,
                    filNavn
                ).toString()
            )

            call.respondBytes(kvittering.readBytes(), contentType = ContentType.parse(blob.contentType))
        } else {
            call.respond("Bucket $env.bucketName does not exist.")
        }
    }

    post("/opplasting") {
        val bucket: Bucket? = storage.get(env.bucketName)
        if (bucket != null) {
            val blobNavn = UUID.randomUUID().toString()

            val multipart = call.receiveMultipart()
            multipart.forEachPart { part ->
                if (part is PartData.FileItem) {
                    val validator = VedleggValidator()
                    val fil = File(part.originalFileName!!)
                    part.streamProvider().use { input -> fil.outputStream().buffered().use { output -> input.copyToSuspend(output) } }
                    if (validator.valider(fil)) {
                        val type = validator.filtype(fil)
                        val blob = fil.inputStream().buffered()
                        bucket.create(blobNavn, blob, type.toString())
                        val vedleggRespons = VedleggRespons(blobNavn, "opprettet")
                        call.respond(TextContent(vedleggRespons.toJson(), ContentType.Application.Json.withCharset(Charsets.UTF_8), HttpStatusCode.Created))
                    } else {
                        val vedleggRespons = VedleggRespons(blobNavn, "kunne ikke opprette")
                        call.respond(TextContent(vedleggRespons.toJson(), ContentType.Application.Json.withCharset(Charsets.UTF_8), HttpStatusCode.BadRequest))
                    }
                }
                part.dispose()
            }
        } else {
            call.respond(TextContent(VedleggRespons(id = null, melding = "bøtta finnes ikke").toJson(), ContentType.Application.Json.withCharset(Charsets.UTF_8), HttpStatusCode.NotFound))
        }
    }
}
