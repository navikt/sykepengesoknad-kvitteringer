package no.nav.syfo

import com.auth0.jwk.JwkProviderBuilder
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.google.api.gax.retrying.RetrySettings
import com.google.cloud.storage.StorageOptions
import io.prometheus.client.hotspot.DefaultExports
import kotlinx.coroutines.DEBUG_PROPERTY_NAME
import kotlinx.coroutines.DEBUG_PROPERTY_VALUE_ON
import no.nav.syfo.application.ApplicationServer
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.createApplicationEngine
import no.nav.syfo.application.getWellKnown
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.threeten.bp.Duration
import java.net.URL
import java.util.concurrent.TimeUnit

val log: Logger = LoggerFactory.getLogger("no.nav.syfo.flex-bucket-uploader")

val objectMapper: ObjectMapper = ObjectMapper().apply {
    registerKotlinModule()
    registerModule(JavaTimeModule())
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
}

fun main() {
    System.setProperty(DEBUG_PROPERTY_NAME, DEBUG_PROPERTY_VALUE_ON)
    val env = Environment()

    log.info("Sover i ${env.sidecarInitialDelay} ms i håp om at sidecars er klare")
    Thread.sleep(env.sidecarInitialDelay)

    val wellKnown = getWellKnown(env.loginserviceIdportenDiscoveryUrl)
    val jwkProvider = JwkProviderBuilder(URL(wellKnown.jwks_uri))
        .cached(10, 24, TimeUnit.HOURS)
        .rateLimited(10, 1, TimeUnit.MINUTES)
        .build()

    val aadWellKnown = getWellKnown(env.azureAppWellKnownUrl)
    val aadProvider = JwkProviderBuilder(URL(aadWellKnown.jwks_uri))
        .cached(10, 24, TimeUnit.HOURS)
        .rateLimited(10, 1, TimeUnit.MINUTES)
        .build()

    DefaultExports.initialize()
    val applicationState = ApplicationState()

    val retrySettings = RetrySettings.newBuilder().setTotalTimeout(Duration.ofMillis(3000)).build()
    val storage = StorageOptions.newBuilder().setRetrySettings(retrySettings).build().service

    val applicationEngine = createApplicationEngine(
        env,
        applicationState,
        storage,
        jwkProvider = jwkProvider,
        issuer = wellKnown.issuer,
        loginserviceIdportenAudience = env.loginserviceIdportenAudience,
        aadProvider = aadProvider,
        aadIssuer = aadWellKnown.issuer,
        aadClientId = env.azureAppClientId
    )
    val applicationServer = ApplicationServer(applicationEngine, applicationState)
    applicationServer.start()
    applicationState.ready = true
}
