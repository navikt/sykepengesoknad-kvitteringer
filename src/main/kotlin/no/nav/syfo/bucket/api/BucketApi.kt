package no.nav.syfo.bucket.api

import com.google.cloud.storage.Bucket
import com.google.cloud.storage.Storage
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.http.content.streamProvider
import io.ktor.request.receiveMultipart
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import io.ktor.routing.post
import no.nav.syfo.Environment
import java.io.InputStream
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

    post("/opplasting") {
        val bucket = storage.get(env.bucketName)
        var blob: InputStream? = null
        val blobNavn = UUID.randomUUID().toString()

        val multipart = call.receiveMultipart()
        multipart.forEachPart { part ->
            if (part is PartData.FileItem) {
                blob = part.streamProvider().buffered()
            }
            part.dispose()
        }

        if (blob != null && ValiderBlob(blob!!)) {
            bucket.create(blobNavn, blob)
            call.respond(HttpStatusCode.Created, "$blobNavn ble lastet opp")
        } else {
            call.respond(HttpStatusCode.BadRequest, "Fikk ikke lastet opp $blobNavn")
        }
    }
}

fun ValiderBlob(blob: InputStream): Boolean {
    return true
}
