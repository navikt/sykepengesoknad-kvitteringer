package no.nav.helse.flex.no.nav.helse.flex.api

import no.nav.helse.flex.AbstractApiError
import no.nav.helse.flex.LogLevel
import no.nav.helse.flex.kvittering.Kvittering
import no.nav.helse.flex.kvittering.Kvitteringer
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.context.TokenValidationContextHolder
import no.nav.security.token.support.core.jwt.JwtTokenClaims
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.multipart.MultipartFile
import java.util.*

@Controller
class BrukerApi(
    private val tokenValidationContextHolder: TokenValidationContextHolder,
    private val kvitteringer: Kvitteringer,

    @Value("\${SYKEPENGESOKNAD_FRONTEND_CLIENT_ID}")
    val sykepengesoknadFrontendClientId: String,

    @Value("\${SYKEPENGESOKNAD_BACKEND_CLIENT_ID}")
    val sykepengesoknadBackendClientId: String,

    @Value("\${TOKENX_IDPORTEN_IDP}")
    val tokenxIdportenIdp: String
) {

    @PostMapping("/api/v2/opplasting")
    @ProtectedWithClaims(issuer = "tokenx", claimMap = ["acr=Level4"])
    @ResponseBody
    fun lagreKvittering(@RequestParam("file") file: MultipartFile): ResponseEntity<VedleggRespons> {
        val id = UUID.randomUUID().toString()
        val fnr = validerTokenXClaims(sykepengesoknadFrontendClientId).hentFnr()

        kvitteringer.lagreKvittering(fnr, id, MediaType.parseMediaType(file.contentType!!), file.bytes)
        return ResponseEntity.status(HttpStatus.CREATED).body(VedleggRespons(id, "Lagret kvittering med id: $id."))
    }

    @GetMapping("/api/v2/kvittering/{blobNavn}")
    @ProtectedWithClaims(issuer = "tokenx", claimMap = ["acr=Level4"])
    fun hentKvittering(@PathVariable blobNavn: String): ResponseEntity<ByteArray> {
        val fnr = validerTokenXClaims(sykepengesoknadFrontendClientId).hentFnr()

        val kvittering = kvitteringer.hentKvittering(blobNavn) ?: return ResponseEntity.notFound().build()

        if (!kvitteringEiesAvBruker(kvittering, fnr)) {
            throw UkjentClientException("Kvittering $blobNavn er forsøkt hentet av feil bruker.")
        }

        return ResponseEntity
            .ok()
            .contentType(MediaType.parseMediaType(kvittering.contentType))
            .body(kvittering.bytes)
    }

    @DeleteMapping("/api/v2/kvittering/{blobNavn}")
    @ResponseBody
    @ProtectedWithClaims(issuer = "tokenx", claimMap = ["acr=Level4"])
    fun slettKvittering(@PathVariable blobNavn: String): ResponseEntity<Any> {
        val fnr = validerTokenXClaims(sykepengesoknadBackendClientId).hentFnr()
        val kvittering = kvitteringer.hentKvittering(blobNavn) ?: return ResponseEntity.noContent().build()

        if (!kvitteringEiesAvBruker(kvittering, fnr)) {
            throw UkjentClientException("Kvittering $blobNavn er forsøkt slettet av feil bruker.")
        }
        kvitteringer.slettKvittering(blobNavn)
        return ResponseEntity.noContent().build()
    }

    private fun kvitteringEiesAvBruker(kvittering: Kvittering, fnr: String): Boolean {
        return fnr == kvittering.fnr
    }

    private fun JwtTokenClaims.hentFnr(): String {
        return this.getStringClaim("pid")
    }

    private fun validerTokenXClaims(vararg tillattClient: String): JwtTokenClaims {
        val context = tokenValidationContextHolder.tokenValidationContext
        val claims = context.getClaims("tokenx")
        val clientId = claims.getStringClaim("client_id")

        if (!tillattClient.toList().contains(clientId)) {
            throw UkjentClientException("Uventet client id $clientId")
        }
        val idp = claims.getStringClaim("idp")
        if (idp != tokenxIdportenIdp) {
            // Sjekker at det var idporten som er IDP for tokenX tokenet
            throw UkjentClientException("Uventet idp $idp")
        }
        return claims
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
