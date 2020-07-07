package no.nav.syfo.bucket

import com.google.cloud.storage.Bucket
import io.ktor.application.call
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get

fun Route.setupSample(bucket: Bucket) {

    get("/sample") {
        call.respond(
            bucket.list().iterateAll().joinToString(separator = "\n") { blob ->
                "${blob.name} (content-type: ${blob.contentType}, size: ${blob.size})"
            }
        )
    }
}
