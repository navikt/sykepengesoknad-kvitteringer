package no.nav.syfo

import com.google.api.gax.retrying.RetrySettings
import com.google.cloud.storage.StorageOptions
import io.ktor.util.KtorExperimentalAPI
import io.prometheus.client.hotspot.DefaultExports
import kotlinx.coroutines.withTimeout
import no.nav.syfo.application.ApplicationServer
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.createApplicationEngine
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.threeten.bp.Duration

val log: Logger = LoggerFactory.getLogger("no.nav.syfo.flex-bucket-uploader")

@KtorExperimentalAPI
fun main() {
    val env = Environment()
    DefaultExports.initialize()
    val applicationState = ApplicationState()

    val retrySettings = RetrySettings.newBuilder().setTotalTimeout(Duration.ofMillis(3000)).build()
    val storage = StorageOptions.newBuilder().setRetrySettings(retrySettings).build().service

    val applicationEngine = createApplicationEngine(
        env,
        applicationState,
        storage
    )
    val applicationServer = ApplicationServer(applicationEngine, applicationState)
    applicationServer.start()
    applicationState.ready = true
}
