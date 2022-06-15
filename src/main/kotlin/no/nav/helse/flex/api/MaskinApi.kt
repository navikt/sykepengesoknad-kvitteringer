package no.nav.helse.flex.no.nav.helse.flex.api

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.AbstractApiError
import no.nav.helse.flex.LogLevel
import no.nav.helse.flex.api.VedleggRespons
import no.nav.helse.flex.kvittering.Kvitteringer
import no.nav.helse.flex.objectMapper
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.ResponseBody

@Controller
class MaskinApi(
    @Value("\${AZURE_APP_PRE_AUTHORIZED_APPS}")
    private val azurePreAuthorizedApps: String,
    private val tokenValidationContextHolder: TokenValidationContextHolder,
    private val kvitteringer: Kvitteringer
) {

    private val allowedClients: List<PreAuthorizedClient> = objectMapper.readValue(azurePreAuthorizedApps)

    @GetMapping("/maskin/kvittering/{blobName}")
    @ProtectedWithClaims(issuer = "azureator")
    @ResponseBody
    fun hentMaskinKvittering(@PathVariable blobName: String): ResponseEntity<ByteArray> {
        validateClientId(validClients())
        val kvittering = kvitteringer.hentKvittering(blobName) ?: return ResponseEntity.notFound().build()
        return ResponseEntity
            .ok()
            .contentType(MediaType.parseMediaType(kvittering.contentType))
            .body(kvittering.byteArray)
    }

    @GetMapping("/maskin/slett/{blobName}")
    @ResponseBody
    @ProtectedWithClaims(issuer = "azureator")
    fun slettKvittering(@PathVariable blobName: String): ResponseEntity<VedleggRespons> {
        validateClientId(validClients())
        kvitteringer.slettKvittering(blobName)
        return ResponseEntity.ok().body(VedleggRespons(blobName, "Slettet kvittering med id: $blobName."))
    }

    private fun validClients() = listOf(
        NamespaceOgApp(namespace = "flex", app = "sykepengesoknad-backend")
    )

    fun validateClientId(apps: List<NamespaceOgApp>) {
        val clientIds = allowedClients
            .filter { apps.contains(it.tilNamespaceOgApp()) }
            .map { it.clientId }

        val azp = tokenValidationContextHolder.hentAzpClaim()
        if (!clientIds.contains(azp)) {
            throw UkjentClientException("Ukjent azp claim $azp.")
        }
    }

    private fun TokenValidationContextHolder.hentAzpClaim(): String {
        try {
            return this.tokenValidationContext.getJwtToken("azureator").jwtTokenClaims.getStringClaim("azp")!!
        } catch (e: Exception) {
            throw UkjentClientException("Fant ikke azp claim.", e)
        }
    }

    private fun PreAuthorizedClient.tilNamespaceOgApp(): NamespaceOgApp {
        val splitt = name.split(":")
        return NamespaceOgApp(namespace = splitt[1], app = splitt[2])
    }
}

class UkjentClientException(message: String, grunn: Throwable? = null) : AbstractApiError(
    message = message,
    httpStatus = HttpStatus.FORBIDDEN,
    reason = "UKJENT_CLIENT",
    loglevel = LogLevel.WARN,
    grunn = grunn
)

data class NamespaceOgApp(val namespace: String, val app: String)

data class PreAuthorizedClient(val name: String, val clientId: String)
