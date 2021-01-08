package no.nav.syfo.bucket.api

import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Bucket
import com.google.cloud.storage.Storage
import io.ktor.application.call
import io.ktor.auth.authentication
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.readBytes
import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.*
import io.ktor.http.withCharset
import io.ktor.request.receiveMultipart
import io.ktor.response.header
import io.ktor.response.respond
import io.ktor.response.respondBytes
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.syfo.Environment
import no.nav.syfo.log
import no.nav.syfo.models.VedleggRespons
import no.nav.syfo.models.toJson
import java.io.File
import java.lang.RuntimeException
import java.util.UUID

fun Route.setupBucketApi(storage: Storage, env: Environment) {
    get("/list") {
        val principal: JWTPrincipal = call.authentication.principal()!!
        val fnr = principal.payload.subject
        val bucket: Bucket? = storage.get(env.bucketName)
        if (bucket == null) {
            call.respond(
                TextContent(
                    VedleggRespons(id = null, melding = "bøtta finnes ikke").toJson(),
                    ContentType.Application.Json.withCharset(Charsets.UTF_8),
                    HttpStatusCode.NotFound
                )
            )
            return@get
        }
        call.respond(
            bucket.list().iterateAll().filter { it.metadata?.get("fnr") == fnr }
                .joinToString(separator = "\n") { blob ->
                    "${blob.name} (content-type: ${blob.metadata?.get("content-type")}, size: ${blob.size})"
                }
        )
    }

    get("/kvittering/{blobName}") {
        val principal: JWTPrincipal = call.authentication.principal()!!
        val fnr = principal.payload.subject
        val bucket: Bucket? = storage.get(env.bucketName)
        if (bucket == null) {
            call.respond(
                TextContent(
                    VedleggRespons(id = null, melding = "bøtta finnes ikke").toJson(),
                    ContentType.Application.Json.withCharset(Charsets.UTF_8),
                    HttpStatusCode.NotFound
                )
            )
            return@get
        }
        val blobName = call.parameters["blobName"]!!
        val blob = bucket.get(blobName)
        if (blob.metadata?.get("fnr") != fnr) {
            call.respond("fant ikke dokument")
            return@get
        }

        val kvittering = File(blobName)
        kvittering.writeBytes(blob.getContent())
        val kvitteringNavn = "kvittering-$blobName.${blob.metadata?.get("content-type")!!.split("/")[1]}"
        call.response.header(
            HttpHeaders.ContentDisposition,
            ContentDisposition.Attachment.withParameter(
                ContentDisposition.Parameters.FileName,
                kvitteringNavn
            ).toString()
        )
        call.respondBytes(
            kvittering.readBytes(),
            contentType = ContentType.parse(blob.metadata?.get("content-type")!!)
        )
        log.info("Returnerer $kvitteringNavn (content-type: ${blob.metadata?.get("content-type")})")
    }

    post("/opplasting") {
        val principal: JWTPrincipal = call.authentication.principal()!!
        val fnr = principal.payload.subject
        val blobNavn = UUID.randomUUID().toString()

        val multipart = call.receiveMultipart()
        val filpart = multipart.readAllParts().find { part -> part is PartData.FileItem } as PartData.FileItem?
            ?: throw RuntimeException("Fant ikke fil")
        val contentType = filpart.contentType
        val fil = withContext(Dispatchers.IO) { filpart.streamProvider().readBytes() }

        val client = HttpClient(Apache)

        val processedFile = client.post<HttpResponse>(env.imageProcessingUrl) {
            body = ByteArrayContent(fil, contentType)
        }

        if (processedFile.status != HttpStatusCode.OK) {
            call.respond(
                TextContent(
                    VedleggRespons(null, "kunne ikke opprette vedlegg").toJson(),
                    ContentType.Application.Json.withCharset(Charsets.UTF_8),
                    HttpStatusCode.BadRequest
                )
            )
            return@post
        }

        val bId = BlobId.of(env.bucketName, blobNavn)
        val bInfo = BlobInfo.newBuilder(bId)
            .setMetadata(mapOf("fnr" to fnr, "content-type" to "image/jpg"))
            .build()
        storage.create(bInfo, processedFile.readBytes())
        call.respond(
            TextContent(
                VedleggRespons(blobNavn, "opprettet").toJson(),
                ContentType.Application.Json.withCharset(Charsets.UTF_8),
                HttpStatusCode.Created
            )
        )
    }
}
