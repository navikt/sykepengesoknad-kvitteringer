package no.nav.syfo

import io.ktor.util.KtorExperimentalAPI
import no.nav.syfo.models.VedleggRespons
import no.nav.syfo.models.toJson
import org.amshove.kluent.shouldEqual
import org.junit.jupiter.api.Test

@KtorExperimentalAPI
internal class VedleggResponsTest {

    @Test
    fun `Vedlegg med ID abc returnerer gyldig Json`() {
        val vedleggRespons = VedleggRespons("abc", "opprettet")
        vedleggRespons.toJson() shouldEqual "{\"id\":\"abc\",\"melding\":\"opprettet\"}"
    }
}
