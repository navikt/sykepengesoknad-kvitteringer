package no.nav.syfo.application

import com.auth0.jwk.JwkProvider
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.Principal
import io.ktor.auth.jwt.JWTCredential
import io.ktor.auth.jwt.JWTPrincipal
import io.ktor.auth.jwt.jwt
import net.logstash.logback.argument.StructuredArguments
import no.nav.syfo.PreAuthorizedClient
import no.nav.syfo.log

fun Application.setupAuth(
    jwkProvider: JwkProvider,
    issuer: String,
    loginserviceIdportenAudience: String,
    aadProvider: JwkProvider,
    aadIssuer: String,
    aadClientId: String,
    aadPreauthorizedApps: List<PreAuthorizedClient>
) {
    install(Authentication) {
        jwt(name = "jwt") {
            verifier(jwkProvider, issuer)
            validate { credentials: JWTCredential ->
                if (!hasExpectedAudience(credentials, loginserviceIdportenAudience)){
                    return@validate unauthorized(credentials)
                }
                if (!erNiva4(credentials)) {
                    return@validate unauthorized(credentials)
                }
                return@validate JWTPrincipal(credentials.payload)
            }
        }
        jwt(name = "aad") {
            verifier(aadProvider, aadIssuer)
            validate { credentials ->
                if (!hasExpectedAudience(credentials, aadClientId)) {
                    return@validate unauthorized(credentials)
                }
                if (!aadPreauthorizedApps.map { it.clientId }.contains(credentials.payload.getClaim("azp").asString())) {
                    return@validate unauthorized(credentials)
                }
                return@validate JWTPrincipal(credentials.payload)
            }
        }
    }
}

fun unauthorized(credentials: JWTCredential): Principal? {
    log.warn(
        "Auth: Unexpected audience for jwt {}, {}",
        StructuredArguments.keyValue("issuer", credentials.payload.issuer),
        StructuredArguments.keyValue("audience", credentials.payload.audience)
    )
    return null
}

fun hasExpectedAudience(credentials: JWTCredential, loginserviceIdportenAudience: String): Boolean {
    return credentials.payload.audience.contains(loginserviceIdportenAudience)
}

fun erNiva4(credentials: JWTCredential): Boolean {
    return "Level4" == credentials.payload.getClaim("acr").asString()
}
