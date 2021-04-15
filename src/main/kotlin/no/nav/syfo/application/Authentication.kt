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
                if (!hasExpectedAudience(credentials, loginserviceIdportenAudience)) {
                    log.warn("Auth: Unexpected audience: expected: $loginserviceIdportenAudience Actual: ${credentials.payload.audience}")
                    return@validate null
                }
                if (!erNiva4(credentials)) {
                    log.warn("Auth: Ikke nivå 4")
                    return@validate null
                }
                return@validate JWTPrincipal(credentials.payload)
            }
        }
        jwt(name = "aad") {
            verifier(aadProvider, aadIssuer)
            validate { credentials ->
                if (!hasExpectedAudience(credentials, aadClientId)) {
                    log.warn("Auth: Unexpected audience: expected: $aadClientId Actual: ${credentials.payload.audience}")
                    return@validate null
                }
                val azpClaim = credentials.payload.getClaim("azp").asString()
                val expectedClientIds = aadPreauthorizedApps.map { it.clientId }
                if (!expectedClientIds.contains(azpClaim)) {
                    log.warn("Auth: Unexpected azp: expected: $expectedClientIds Actual: $azpClaim")
                    return@validate null
                }
                return@validate JWTPrincipal(credentials.payload)
            }
        }
    }
}



fun hasExpectedAudience(credentials: JWTCredential, loginserviceIdportenAudience: String): Boolean {
    return credentials.payload.audience.contains(loginserviceIdportenAudience)
}

fun erNiva4(credentials: JWTCredential): Boolean {
    return "Level4" == credentials.payload.getClaim("acr").asString()
}
