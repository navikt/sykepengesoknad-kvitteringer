package no.nav.syfo

import no.nav.syfo.models.VedleggRespons
import no.nav.syfo.models.toJson
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

internal class VedleggResponsTest {

    @Test
    fun `Vedlegg med ID abc returnerer gyldig Json`() {
        val vedleggRespons = VedleggRespons("abc", "opprettet")
        vedleggRespons.toJson() shouldBeEqualTo "{\"id\":\"abc\",\"melding\":\"opprettet\"}"
    }
}
