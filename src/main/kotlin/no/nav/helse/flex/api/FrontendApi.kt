package no.nav.helse.flex.api

import no.nav.helse.flex.logger
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/api")
class FrontendApi {

    private val log = logger()

    @GetMapping("/verify")
    @Unprotected
    fun getBooks(): ResponseEntity<String> {
        log.info("Mottok GET-request på /api/verify")
        return ResponseEntity.ok("Called")
    }
}
