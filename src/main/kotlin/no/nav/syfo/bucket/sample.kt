package no.nav.syfo.bucket

import com.google.cloud.storage.StorageOptions
import io.ktor.application.call
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.get
import no.nav.syfo.Environment

fun Route.setupSample(env: Environment) {
    val storage = StorageOptions.getDefaultInstance().service
    val bucket = storage.get(env.bucketName) ?: error("Bucket $env.bucketName does not exist.")

    get("/sample") {
        call.respond(
            bucket.list().iterateAll().joinToString(separator = "\n") { blob ->
                "${blob.name} (content-type: ${blob.contentType}, size: ${blob.size})"
            }
        )
    }
}
