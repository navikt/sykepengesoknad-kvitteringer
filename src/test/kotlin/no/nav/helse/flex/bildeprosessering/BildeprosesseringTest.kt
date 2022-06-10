package no.nav.helse.flex.bildeprosessering

import no.nav.helse.flex.FellesTestOppsett
import no.nav.helse.flex.no.nav.helse.flex.bildeprosessering.Bilde
import no.nav.helse.flex.no.nav.helse.flex.bildeprosessering.Bildeprosessering
import org.amshove.kluent.`should be equal to`
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.Paths
import javax.imageio.ImageIO

private const val TESTBILDER = "src/test/resources/bilder/"

internal class BildeprosesseringTest : FellesTestOppsett() {

    @Autowired
    private lateinit var bildeprosessering: Bildeprosessering

    @Test
    fun `Prosesser JPEG-bilde`() {
        val bilde = hentBilde("${TESTBILDER}example.jpg")

        val prosessertBilde = bildeprosessering.prosesserBilde(bilde)

        prosessertBilde!!.contentType `should be equal to` MediaType.IMAGE_JPEG

        val bufferedImage = ImageIO.read(ByteArrayInputStream(prosessertBilde.bytes))
        bufferedImage.height `should be equal to` 450
        bufferedImage.width `should be equal to` 600
    }

    @Test
    fun `Prosesser HEIC-bilde`() {
        val bilde = hentBilde("${TESTBILDER}example.heic")

        val prosessertBilde = bildeprosessering.prosesserBilde(bilde)

        prosessertBilde!!.contentType `should be equal to` MediaType.IMAGE_JPEG

        val bufferedImage = ImageIO.read(ByteArrayInputStream(prosessertBilde.bytes))
        bufferedImage.height `should be equal to` 400
        bufferedImage.width `should be equal to` 600
    }

    private fun hentBilde(path: String): Bilde {
        val bildeFil = Paths.get(path)

        return Bilde(
            MediaType.parseMediaType(Files.probeContentType(bildeFil)),
            Files.readAllBytes(bildeFil)
        )
    }
}
