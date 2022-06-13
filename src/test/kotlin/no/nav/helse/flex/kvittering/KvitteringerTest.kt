package no.nav.helse.flex.kvittering

import no.nav.helse.flex.FellesTestOppsett
import no.nav.helse.flex.PROSESSERT_JPEG_SIZE
import org.amshove.kluent.invoking
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.shouldThrow
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
internal class KvitteringerTest : FellesTestOppsett() {

    @Autowired
    private lateinit var kvitteringer: Kvitteringer

    @Test
    @Order(1)
    fun `Lagrer kvittering`() {
        val bilde = hentTestbilde("example.jpg")

        kvitteringer.lagreKvittering("fnr-1", "blob-1", bilde.contentType, bilde.bytes)
    }

    @Test
    @Order(2)
    fun `Henter lagret kvittering`() {
        kvitteringer.hentKvittering("fnr-1", "blob-1")?.let {
            it.contentType `should be equal to` MediaType.IMAGE_JPEG.toString()
            it.filNavn `should be equal to` "kvittering-blob-1.jpeg"
            it.contentSize `should be equal to` PROSESSERT_JPEG_SIZE.toLong()
        }
    }

    @Test
    @Order(3)
    fun `Kvitteringer som tilhører en annen bruker kaster exception`() {
        invoking { kvitteringer.hentKvittering("fnr-2", "blob-1") } shouldThrow IllegalAccessException::class
    }

    @Test
    @Order(4)
    fun `Sletter kvittering som finnes`() {
        kvitteringer.slettKvittering("blob-1")

        kvitteringer.hentKvittering("fnr-1", "blob-1") `should be` null
    }

    @Test
    @Order(5)
    fun `Sletter kvittering som allerede er slettet`() {
        kvitteringer.slettKvittering("blob-1")
    }

    @Test
    @Order(6)
    fun `Kvittering som ikke finnes returnerer null`() {
        kvitteringer.hentKvittering("fnr-1", "blob-1") `should be` null
    }
}
