package no.nav.syfo

import com.google.cloud.storage.StorageOptions
import io.prometheus.client.hotspot.DefaultExports
import no.nav.syfo.application.ApplicationServer
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.createApplicationEngine
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val log: Logger = LoggerFactory.getLogger("no.nav.syfo.flex-bucket-uploader")

fun main() {
    val env = Environment()
    DefaultExports.initialize()
    val applicationState = ApplicationState()

    val storage = StorageOptions.getDefaultInstance().service
    val bucket = storage.get(env.bucketName) ?: error("Bucket $env.bucketName does not exist.")

    val applicationEngine = createApplicationEngine(
        env,
        applicationState,
        bucket
    )
    val applicationServer = ApplicationServer(applicationEngine, applicationState)
    applicationServer.start()
    applicationState.ready = true
}
