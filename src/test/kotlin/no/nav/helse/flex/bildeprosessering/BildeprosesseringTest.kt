package no.nav.helse.flex.bildeprosessering

import no.nav.helse.flex.FellesTestOppsett
import no.nav.helse.flex.no.nav.helse.flex.bildeprosessering.Bildeprosessering
import org.amshove.kluent.`should be equal to`
import org.apache.tika.Tika
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

internal class BildeprosesseringTest : FellesTestOppsett() {

    private val tika = Tika()

    @Autowired
    private lateinit var bildeprosessering: Bildeprosessering

    @Test
    fun `JPEG-bilde blir skalert ned når er for bredt`() {
        val bilde = hentTestbilde("1200x800.jpeg")

        val prosessertBilde = bildeprosessering.prosesserBilde(bilde)
        tika.detect(prosessertBilde!!.bytes) `should be equal to` MediaType.IMAGE_JPEG_VALUE

        val bufferedImage = ImageIO.read(ByteArrayInputStream(prosessertBilde.bytes))
        bufferedImage.width `should be equal to` 600
        bufferedImage.height `should be equal to` 400
    }

    @Test
    fun `JPEG-bilde beholder størrelsen når det har riktig bredde`() {
        val bilde = hentTestbilde("600x400.jpeg")

        val prosessertBilde = bildeprosessering.prosesserBilde(bilde)
        tika.detect(prosessertBilde!!.bytes) `should be equal to` MediaType.IMAGE_JPEG_VALUE

        val bufferedImage = ImageIO.read(ByteArrayInputStream(prosessertBilde.bytes))
        bufferedImage.width `should be equal to` 600
        bufferedImage.height `should be equal to` 400
    }

    @Test
    fun `JPEG-bilde blir skalert opp når det er for smalt`() {
        val bilde = hentTestbilde("540x360.jpeg")

        val prosessertBilde = bildeprosessering.prosesserBilde(bilde)
        tika.detect(prosessertBilde!!.bytes) `should be equal to` MediaType.IMAGE_JPEG_VALUE

        val bufferedImage = ImageIO.read(ByteArrayInputStream(prosessertBilde.bytes))
        bufferedImage.width `should be equal to` 600
        bufferedImage.height `should be equal to` 400
    }

    @Test
    fun `JPEG-bilde i portrett blir skalert ned når er for bredt`() {
        val bilde = hentTestbilde("800x1200.jpeg")

        val prosessertBilde = bildeprosessering.prosesserBilde(bilde)
        tika.detect(prosessertBilde!!.bytes) `should be equal to` MediaType.IMAGE_JPEG_VALUE

        val bufferedImage = ImageIO.read(ByteArrayInputStream(prosessertBilde.bytes))
        bufferedImage.width `should be equal to` 600
        bufferedImage.height `should be equal to` 900
    }

    @Test
    fun `JPEG-bilde i portrett beholder størrelsen når det har riktig bredde`() {
        val bilde = hentTestbilde("600x900.jpeg")

        val prosessertBilde = bildeprosessering.prosesserBilde(bilde)
        tika.detect(prosessertBilde!!.bytes) `should be equal to` MediaType.IMAGE_JPEG_VALUE

        val bufferedImage = ImageIO.read(ByteArrayInputStream(prosessertBilde.bytes))
        bufferedImage.width `should be equal to` 600
        bufferedImage.height `should be equal to` 900
    }

    @Test
    fun `JPEG-bilde i portrett blir skalert opp når det er for smalt`() {
        val bilde = hentTestbilde("400x600.jpeg")

        val prosessertBilde = bildeprosessering.prosesserBilde(bilde)
        tika.detect(prosessertBilde!!.bytes) `should be equal to` MediaType.IMAGE_JPEG_VALUE

        val bufferedImage = ImageIO.read(ByteArrayInputStream(prosessertBilde.bytes))
        bufferedImage.width `should be equal to` 600
        bufferedImage.height `should be equal to` 900
    }

    @Test
    fun `PNG-bilde blir skalert ned når er for bredt`() {
        val bilde = hentTestbilde("1200x800.png", MediaType.parseMediaType("image/png"))

        val prosessertBilde = bildeprosessering.prosesserBilde(bilde)
        tika.detect(prosessertBilde!!.bytes) `should be equal to` MediaType.IMAGE_JPEG_VALUE

        val bufferedImage = ImageIO.read(ByteArrayInputStream(prosessertBilde.bytes))
        bufferedImage.width `should be equal to` 600
        bufferedImage.height `should be equal to` 400
    }

    @Test
    fun `PNG-bilde i portrett blir skalert ned når er for bredt`() {
        val bilde = hentTestbilde("800x1200.png", MediaType.parseMediaType("image/png"))

        val prosessertBilde = bildeprosessering.prosesserBilde(bilde)
        tika.detect(prosessertBilde!!.bytes) `should be equal to` MediaType.IMAGE_JPEG_VALUE

        val bufferedImage = ImageIO.read(ByteArrayInputStream(prosessertBilde.bytes))
        bufferedImage.width `should be equal to` 600
        bufferedImage.height `should be equal to` 900
    }

    @Test
    fun `HEIC-bilde blir skalert ned når er for bredt`() {
        val bilde = hentTestbilde("1200x800.heic", MediaType.parseMediaType("image/heic"))

        val prosessertBilde = bildeprosessering.prosesserBilde(bilde)
        tika.detect(prosessertBilde!!.bytes) `should be equal to` MediaType.IMAGE_JPEG_VALUE

        val bufferedImage = ImageIO.read(ByteArrayInputStream(prosessertBilde.bytes))
        bufferedImage.width `should be equal to` 600
        bufferedImage.height `should be equal to` 400
    }

    @Test
    fun `HEIC-bilde i portrett blir skalert ned når er for bredt`() {

        val bilde = hentTestbilde("800x1200.heic", MediaType.parseMediaType("image/heic"))

        val prosessertBilde = bildeprosessering.prosesserBilde(bilde)
        tika.detect(prosessertBilde!!.bytes) `should be equal to` MediaType.IMAGE_JPEG_VALUE

        val bufferedImage = ImageIO.read(ByteArrayInputStream(prosessertBilde.bytes))
        bufferedImage.width `should be equal to` 600
        bufferedImage.height `should be equal to` 900
    }
}
