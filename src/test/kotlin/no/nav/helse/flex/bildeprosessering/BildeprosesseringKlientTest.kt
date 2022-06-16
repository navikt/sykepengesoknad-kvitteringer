package no.nav.helse.flex.bildeprosessering

import no.nav.helse.flex.FellesTestOppsett
import no.nav.helse.flex.no.nav.helse.flex.bildeprosessering.BildeprosesseringKlient
import org.amshove.kluent.`should be equal to`
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

internal class BildeprosesseringKlientTest : FellesTestOppsett() {

    @Autowired
    private lateinit var bildeprosesseringKlient: BildeprosesseringKlient

    @Test
    fun `Prosesser JPEG-bilde`() {
        val bilde = hentTestbilde("example.jpg")

        val prosessertBilde = bildeprosesseringKlient.prosesserBilde(bilde)

        prosessertBilde!!.contentType `should be equal to` MediaType.IMAGE_JPEG

        val bufferedImage = ImageIO.read(ByteArrayInputStream(prosessertBilde.bytes))
        bufferedImage.height `should be equal to` 450
        bufferedImage.width `should be equal to` 600
    }

    @Test
    fun `Prosesser HEIC-bilde`() {
        // Automatisk deteksjon av "image/heic" feiler i GibHub Actions.
        val bilde = hentTestbilde("example.heic", MediaType.parseMediaType("image/heic"))

        val prosessertBilde = bildeprosesseringKlient.prosesserBilde(bilde)

        prosessertBilde!!.contentType `should be equal to` MediaType.IMAGE_JPEG

        val bufferedImage = ImageIO.read(ByteArrayInputStream(prosessertBilde.bytes))
        bufferedImage.height `should be equal to` 400
        bufferedImage.width `should be equal to` 600
    }
}
