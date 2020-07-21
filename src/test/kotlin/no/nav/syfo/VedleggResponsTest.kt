package no.nav.syfo

import io.ktor.util.KtorExperimentalAPI
import no.nav.syfo.models.VedleggRespons
import no.nav.syfo.models.toJson
import org.amshove.kluent.shouldEqual
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe

@KtorExperimentalAPI
object VedleggResponsTest : Spek({

    describe("Vedlegg med ID abc returnerer gyldig Json") {
        val vedleggRespons = VedleggRespons("abc", "opprettet")
        vedleggRespons.toJson() shouldEqual "{\"id\":\"abc\",\"melding\":\"opprettet\"}"
    }
})
