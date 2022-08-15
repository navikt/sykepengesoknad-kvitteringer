package no.nav.helse.flex.api

import no.nav.helse.flex.kvittering.Kvittering
import no.nav.helse.flex.kvittering.Kvitteringer
import no.nav.helse.flex.no.nav.helse.flex.api.UkjentClientException
import no.nav.helse.flex.no.nav.helse.flex.api.VedleggRespons
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.core.context.TokenValidationContextHolder
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
import java.util.*

@Controller
class FrontendApiLoginservice(
    private val tokenValidationContextHolder: TokenValidationContextHolder,
    private val kvitteringer: Kvitteringer
) {

    @PostMapping("/opplasting")
    @ProtectedWithClaims(issuer = "loginservice", claimMap = ["acr=Level4"])
    @ResponseBody
    fun lagreKvittering(@RequestParam("file") file: MultipartFile): ResponseEntity<VedleggRespons> {
        val id = UUID.randomUUID().toString()

        kvitteringer.lagreKvittering(hentFnrFraClaim(), id, MediaType.parseMediaType(file.contentType!!), file.bytes)
        return ResponseEntity.status(HttpStatus.CREATED).body(VedleggRespons(id, "Lagret kvittering med id: $id."))
    }

    @GetMapping("/kvittering/{blobNavn}")
    @ProtectedWithClaims(issuer = "loginservice", claimMap = ["acr=Level4"])
    fun hentKvittering(@PathVariable blobNavn: String): ResponseEntity<ByteArray> {
        val kvittering = kvitteringer.hentKvittering(blobNavn) ?: return ResponseEntity.notFound().build()

        if (!kvitteringEiesAvBruker(kvittering)) {
            throw UkjentClientException("Kvittering $blobNavn er forsøkt hentet av feil bruker.")
        }

        return ResponseEntity
            .ok()
            .contentType(MediaType.parseMediaType(kvittering.contentType))
            .body(kvittering.bytes)
    }

    private fun kvitteringEiesAvBruker(kvittering: Kvittering): Boolean {
        return hentFnrFraClaim() == kvittering.fnr
    }

    private fun hentFnrFraClaim(): String {
        fun TokenValidationContextHolder.hentFnr(): String {
            val claims = this.tokenValidationContext.getClaims("loginservice")
            return claims.getStringClaim("pid") ?: claims.subject
        }

        return tokenValidationContextHolder.hentFnr()
    }
}
