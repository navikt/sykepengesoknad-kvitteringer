package no.nav.syfo.bucket.api

import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Bucket
import com.google.cloud.storage.Storage
import io.ktor.application.call
import io.ktor.auth.authentication
import io.ktor.auth.jwt.JWTPrincipal
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
import java.io.File
import java.util.UUID
import no.nav.syfo.Environment
import no.nav.syfo.log
import no.nav.syfo.models.VedleggRespons
import no.nav.syfo.models.toJson

fun Route.setupBucketApi(storage: Storage, env: Environment) {
    get("/list") {
        val principal: JWTPrincipal = call.authentication.principal()!!
        val fnr = principal.payload.subject
        val bucket: Bucket? = storage.get(env.bucketName)
        if (bucket != null) {
            call.respond(
                bucket.list().iterateAll().filter{ it.metadata?.get("fnr") == fnr }
                    .joinToString(separator = "\n") { blob ->
                        "${blob.name} (content-type: ${blob.contentType}, size: ${blob.size})"
                }
            )
        } else {
            call.respond("Bucket $env.bucketName does not exist.")
        }
    }

    get("/kvittering/{blobName}") {
        val principal: JWTPrincipal = call.authentication.principal()!!
        val fnr = principal.payload.subject

        val bucket: Bucket? = storage.get(env.bucketName)
        if (bucket != null) {
            val blobName = call.parameters["blobName"]!!
            val blob = bucket.get(blobName)
            if (blob.metadata?.get("fnr") == fnr) {
                val kvittering = File(blobName)
                kvittering.writeBytes(blob.getContent())
                val kvitteringNavn = "kvittering-$blobName.${blob.contentType.split("/")[1]}"
                call.response.header(
                    HttpHeaders.ContentDisposition,
                    ContentDisposition.Attachment.withParameter(
                        ContentDisposition.Parameters.FileName,
                        kvitteringNavn
                    ).toString()
                )
                call.respondBytes(kvittering.readBytes(), contentType = ContentType.parse(blob.contentType))
                log.info("Returnerer $kvitteringNavn (content-type: ${blob.contentType})")
            } else {
                call.respond("fant ikke dokument")
            }
        } else {
            call.respond("Bucket $env.bucketName does not exist.")
        }
    }

    post("/opplasting") {
        val principal: JWTPrincipal = call.authentication.principal()!!
        val fnr = principal.payload.subject

        val bucket: Bucket? = storage.get(env.bucketName)
        val validator = VedleggValidator()

        if (bucket != null) {
            val multipart = call.receiveMultipart()
            multipart.forEachPart { part ->
                val blobNavn = UUID.randomUUID().toString()
                if (part is PartData.FileItem) {
                    val fil = File(part.originalFileName!!)
                    part.streamProvider().use { input -> fil.outputStream().buffered().use { output -> input.copyToSuspend(output) } }
                    if (validator.valider(fil)) {
                        val type = validator.filtype(fil)
                        val meta = mapOf("fnr" to fnr, "content-type" to type.toString())
                        val blob = fil.inputStream().buffered().readBytes()

                        val bId = BlobId.of(env.bucketName, blobNavn)
                        val bInfo = BlobInfo.newBuilder(bId)
                            .setMetadata(meta)
                            .build()
                        storage.create(bInfo, blob)
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
