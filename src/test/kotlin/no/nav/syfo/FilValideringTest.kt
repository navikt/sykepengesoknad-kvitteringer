package no.nav.syfo

import io.ktor.util.KtorExperimentalAPI
import no.nav.syfo.bucket.api.VedleggValidator
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.io.File

@KtorExperimentalAPI
object FilValideringTest : Spek({

    describe("Fil med riktig type er gyldig") {
        val filnavn = this::class.java.getResource("/bilder/example.jpg").toURI()
        val fil = File(filnavn)
        val validator = VedleggValidator()
        validator.erTillattFiltype(fil) shouldEqual true
    }
    describe("Fil med HEIC-endelse er gyldig") {
        val filnavn = this::class.java.getResource("/bilder/example.heic").toURI()
        val fil = File(filnavn)
        val validator = VedleggValidator()
        validator.erTillattFiltype(fil) shouldEqual true
    }
    describe("Fil med PDF-endelse er gyldig") {
        val filnavn = this::class.java.getResource("/bilder/example.pdf").toURI()
        val fil = File(filnavn)
        val validator = VedleggValidator()
        validator.erTillattFiltype(fil) shouldEqual true
    }

    describe("Fil med PDF-endelse returnerer application/pdf") {
        val filnavn = this::class.java.getResource("/bilder/example.pdf").toURI()
        val fil = File(filnavn)
        val validator = VedleggValidator()
        validator.filtype(fil).toString() shouldEqual "application/pdf"
    }

    describe("Fil med størrelse under 50Mb er gyldig") {
        val filnavn = this::class.java.getResource("/bilder/example.jpg").toURI()
        val fil = File(filnavn)
        val validator = VedleggValidator()
        validator.erTillattFilstørrelse(fil) shouldEqual true
    }

    describe("Fil med feil type er ugyldig") {
        val filnavn = this::class.java.getResource("/jwkset.json").toURI()
        val fil = File(filnavn)
        val validator = VedleggValidator()
        validator.erTillattFiltype(fil) shouldEqual false
    }

    describe("For stor fil er ugyldig") {
        val filnavn = this::class.java.getResource("/bilder/example.jpg").toURI()
        val fil = File(filnavn)
        val validator = VedleggValidator(maksFilStørrelse = 0)
        validator.erTillattFilstørrelse(fil) shouldEqual false
    }
})
