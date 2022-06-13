package no.nav.helse.flex.api

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.AbstractApiError
import no.nav.helse.flex.LogLevel
import no.nav.helse.flex.kvittering.Kvitteringer
import no.nav.helse.flex.logger
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
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.multipart.MultipartFile

@Controller
class FrontendApi(
    @Value("\${BUCKET_NAME}") private val bucketName: String,
    @Value("\${AZURE_APP_PRE_AUTHORIZED_APPS}") private val azurePreAuthorizedApps: String,
    private val tokenValidationContextHolder: TokenValidationContextHolder,
    private val kvitteringer: Kvitteringer
) {

    private val log = logger()

    private val allowedClients: List<PreAuthorizedClient> = objectMapper.readValue(azurePreAuthorizedApps)

    @GetMapping("/kvittering/{blobName}")
    @ProtectedWithClaims(issuer = "loginservice", claimMap = ["acr=Level4"])
    fun hentKvittering(@PathVariable blobName: String): ResponseEntity<ByteArray> {
        val kvittering = kvitteringer.hentKvittering(hentFnr(), blobName) ?: return ResponseEntity.notFound().build()

        return ResponseEntity
            .ok()
            .contentType(MediaType.parseMediaType(kvittering.contentType))
            .body(kvittering.byteArray)
    }

    @PostMapping("/opplasting")
    @ProtectedWithClaims(issuer = "loginservice", claimMap = ["acr=Level4"])
    fun lagreKvittering(@RequestParam("file") file: MultipartFile): Any {
        val contentType = file.contentType
        val bytes = file.bytes
        log.info("Mottok fil av typen $contentType på størrelse ${bytes.size} tilhørende ${hentFnr()}")
        return ResponseEntity.accepted()
    }

    @GetMapping("/maskin/kvittering/{blobName}")
    @ResponseBody
    @ProtectedWithClaims(issuer = "azureator")
    fun hentBlob(@PathVariable blobName: String): VedleggRespons {

        log.info("Maskin henter kvittering med blobName $blobName")

        validateClientId(
            listOf(
                NamespaceOgApp(
                    namespace = "flex",
                    app = "sykepengesoknad-backend",
                )
            )
        )

        return VedleggRespons("1234", "GetMapping")
    }

    @GetMapping("/maskin/slett/{blobName}")
    @ResponseBody
    @ProtectedWithClaims(issuer = "azureator")
    fun slettBlob(@PathVariable blobName: String): VedleggRespons {

        log.info("Sletter kvittering med blobname $blobName")

        validateClientId(
            listOf(
                NamespaceOgApp(
                    namespace = "flex",
                    app = "sykepengesoknad-backend",
                )
            )
        )

        return VedleggRespons("1234", "GetMapping")
    }

    fun validateClientId(apps: List<NamespaceOgApp>) {
        val clientIds = allowedClients
            .filter { apps.contains(it.tilNamespaceOgApp()) }
            .map { it.clientId }

        val azp = tokenValidationContextHolder.hentAzpClaim()
        if (!clientIds.contains(azp)) {
            throw UkjentClientException("Client $azp hentet fra azp calim er ikke kjent.")
        }
    }

    private fun hentFnr(): String {
        fun TokenValidationContextHolder.hentFnr(): String {
            val claims = this.tokenValidationContext.getClaims("loginservice")
            return claims.getStringClaim("pid") ?: claims.subject
        }

        return tokenValidationContextHolder.hentFnr()
    }

    private fun TokenValidationContextHolder.hentAzpClaim(): String {
        try {
            return this.tokenValidationContext.getJwtToken("azureator").jwtTokenClaims.getStringClaim("azp")!!
        } catch (e: Exception) {
            throw UkjentClientException("Fant ikke azureator azp claim.", e)
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

data class VedleggRespons(val id: String? = null, val melding: String)

data class NamespaceOgApp(val namespace: String, val app: String)

data class PreAuthorizedClient(val name: String, val clientId: String)
