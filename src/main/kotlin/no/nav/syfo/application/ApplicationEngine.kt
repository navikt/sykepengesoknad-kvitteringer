package no.nav.syfo.application

import com.auth0.jwk.JwkProvider
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.google.cloud.storage.Storage
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.jackson.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import no.nav.syfo.Environment
import no.nav.syfo.application.api.registerNaisApi
import no.nav.syfo.application.metrics.monitorHttpRequests
import no.nav.syfo.bucket.api.setupBucketApi
import no.nav.syfo.bucket.api.setupMachineBucketApi
import java.util.*

fun createApplicationEngine(
    env: Environment,
    applicationState: ApplicationState,
    storage: Storage,
    jwkProvider: JwkProvider,
    issuer: String,
    loginserviceIdportenAudience: String,
    aadProvider: JwkProvider,
    aadIssuer: String,
    aadClientId: String
): ApplicationEngine =
    embeddedServer(Netty, env.applicationPort) {
        configureApplication(
            env = env,
            applicationState = applicationState,
            storage = storage,
            jwkProvider = jwkProvider,
            issuer = issuer,
            loginserviceIdportenAudience = loginserviceIdportenAudience,
            aadProvider = aadProvider,
            aadIssuer = aadIssuer,
            aadClientId = aadClientId

        )
    }

fun Application.configureApplication(
    env: Environment,
    applicationState: ApplicationState,
    storage: Storage,
    jwkProvider: JwkProvider,
    issuer: String,
    loginserviceIdportenAudience: String,
    aadProvider: JwkProvider,
    aadIssuer: String,
    aadClientId: String
) {
    install(ContentNegotiation) {
        jackson {
            registerKotlinModule()
            registerModule(JavaTimeModule())
            configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }
    }
    setupAuth(
        jwkProvider = jwkProvider,
        issuer = issuer,
        loginserviceIdportenAudience = loginserviceIdportenAudience,
        aadProvider = aadProvider,
        aadIssuer = aadIssuer,
        aadClientId = aadClientId,
        aadPreauthorizedApps = env.azureAppPreAuthorizedApps
    )

    install(CallId) {
        generate { UUID.randomUUID().toString() }
        verify { callId: String -> callId.isNotEmpty() }
        header(HttpHeaders.XCorrelationId)
    }
    install(StatusPages) {
        exception<Throwable> { cause ->
            log.error("Caught exception", cause)
            call.respond(HttpStatusCode.InternalServerError, cause.message ?: "Unknown error")
        }
    }

    routing {
        registerNaisApi(applicationState)
        authenticate("jwt") {
            setupBucketApi(storage, env)
        }
        authenticate("aad") {
            setupMachineBucketApi(storage, env)
        }
    }
    intercept(ApplicationCallPipeline.Monitoring, monitorHttpRequests())
}
