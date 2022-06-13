package no.nav.helse.flex.kvittering

import no.nav.helse.flex.FellesTestOppsett
import no.nav.helse.flex.no.nav.helse.flex.bucket.BucketClient
import org.amshove.kluent.invoking
import org.amshove.kluent.`should be`
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.shouldThrow
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType

internal class KvitteringerTest : FellesTestOppsett() {

    @Autowired
    private lateinit var bucketClient: BucketClient

    @Autowired
    private lateinit var kvitteringer: Kvitteringer

    @Test
    fun `Henter lagret kvittering`() {
        bucketClient.lagreBlob(
            "blob-1",
            MediaType.IMAGE_JPEG.toString(),
            mapOf("fnr" to "fnr-1"),
            "blob-content-1".toByteArray()
        )

        kvitteringer.hentKvittering("fnr-1", "blob-1")?.let {
            it.contentType `should be equal to` MediaType.IMAGE_JPEG.toString()
            it.filNavn `should be equal to` "kvittering-blob-1.jpeg"
            it.contentSize `should be equal to` 14
            String(it.byteArray) `should be equal to` "blob-content-1"
        }
    }

    @Test
    fun `Kvitteringer som tilhører en annen bruker kaster exception`() {
        bucketClient.lagreBlob(
            "blob-2",
            MediaType.IMAGE_JPEG.toString(),
            mapOf("fnr" to "fnr-2"),
            "blob-content-2".toByteArray()
        )

        invoking { kvitteringer.hentKvittering("fnr-1", "blob-2") } shouldThrow IllegalAccessException::class
    }

    @Test
    fun `Kvittering som ikke finnes returnerer null`() {
        kvitteringer.hentKvittering("fnr-1", "blob-0") `should be` null
    }
}
