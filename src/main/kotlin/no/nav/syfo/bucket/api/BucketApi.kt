package no.nav.syfo.bucket.api

import com.google.cloud.storage.Bucket
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
import java.io.InputStream

fun Route.setupBucketApi(bucket: Bucket) {
    get("/list") {
        call.respond(
            bucket.list().iterateAll().joinToString(separator = "\n") { blob ->
                "${blob.name} (content-type: ${blob.contentType}, size: ${blob.size})"
            }
        )
    }

    post("/opplasting") {
        var blobNavn: String? = null
        var blob: InputStream? = null

        val multipart = call.receiveMultipart()
        multipart.forEachPart { part ->
            if (part is PartData.FormItem) {
                if (part.name == "title") {
                    blobNavn = part.value
                }
            } else if (part is PartData.FileItem) {
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
