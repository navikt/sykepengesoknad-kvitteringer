package no.nav.syfo

import com.auth0.jwk.JwkProviderBuilder
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.google.api.gax.retrying.RetrySettings
import com.google.cloud.storage.StorageOptions
import io.ktor.util.KtorExperimentalAPI
import io.prometheus.client.hotspot.DefaultExports
import no.nav.syfo.application.ApplicationServer
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.createApplicationEngine
import no.nav.syfo.application.getWellKnown
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.threeten.bp.Duration
import java.net.URL
import java.nio.file.Paths
import java.util.concurrent.TimeUnit

val log: Logger = LoggerFactory.getLogger("no.nav.syfo.flex-bucket-uploader")

val objectMapper: ObjectMapper = ObjectMapper().apply {
    registerKotlinModule()
    registerModule(JavaTimeModule())
    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
}

@KtorExperimentalAPI
fun main() {
    val env = Environment()
    val vaultSecrets =
        objectMapper.readValue<VaultSecrets>(Paths.get("/secrets/credentials.json").toFile())
    val wellKnown = getWellKnown(vaultSecrets.oidcWellKnownUri)
    val jwkProvider = JwkProviderBuilder(URL(wellKnown.jwks_uri))
        .cached(10, 24, TimeUnit.HOURS)
        .rateLimited(10, 1, TimeUnit.MINUTES)
        .build()

    val jwkProviderInternal = JwkProviderBuilder(URL(vaultSecrets.internalJwtWellKnownUri))
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
        vaultSecrets = vaultSecrets,
        jwkProvider = jwkProvider,
        issuer = wellKnown.issuer,
        jwkProviderInternal = jwkProviderInternal,
        issuerServiceuser = env.jwtIssuer,
        clientId = env.clientId,
        appIds = env.appIds
    )
    val applicationServer = ApplicationServer(applicationEngine, applicationState)
    applicationServer.start()
    applicationState.ready = true
}
