package no.nav.syfo.bucket.api

import com.google.cloud.storage.BlobId
import com.google.cloud.storage.BlobInfo
import com.google.cloud.storage.Bucket
import com.google.cloud.storage.Storage
import io.ktor.application.call
import io.ktor.auth.authentication
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.client.HttpClient
import io.ktor.client.request.post
import io.ktor.client.statement.readBytes
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.TextContent
import io.ktor.http.content.ByteArrayContent
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
import kotlinx.coroutines.runBlocking
import no.nav.syfo.Environment
import no.nav.syfo.log
import no.nav.syfo.models.VedleggRespons
import no.nav.syfo.models.toJson
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttp
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.InputStream
import java.io.OutputStream
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
        val fil = File(blobNavn)
        val bucket: Bucket? = storage.get(env.bucketName)
        if (bucket == null) {
            call.respond(
                TextContent(
                    VedleggRespons(id = null, melding = "bøtta finnes ikke").toJson(),
                    ContentType.Application.Json.withCharset(Charsets.UTF_8),
                    HttpStatusCode.NotFound
                )
            )
            return@post
        }

        val multipart = call.receiveMultipart()
        var contentType: ContentType = ContentType.Image.JPEG
        multipart.forEachPart { part ->
            if (part is PartData.FileItem) {
                contentType = part.contentType!!
                part.streamProvider()
                    .use { input -> fil.outputStream().buffered().use { output -> input.copyToSuspend(output) } }
                part.dispose()
            }
        }

        val client = OkHttpClient()
        val req = Request.Builder()
            .url(env.imageProcessingUrl)
            .post(fil.asRequestBody(contentType.toString().toMediaType()))
            .build()
        val processedFile = client.newCall(req).execute()
        if (!processedFile.isSuccessful) {
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
        storage.create(bInfo, processedFile.body!!.bytes())
        call.respond(
            TextContent(
                VedleggRespons(blobNavn, "opprettet").toJson(),
                ContentType.Application.Json.withCharset(Charsets.UTF_8),
                HttpStatusCode.Created
            )
        )
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
