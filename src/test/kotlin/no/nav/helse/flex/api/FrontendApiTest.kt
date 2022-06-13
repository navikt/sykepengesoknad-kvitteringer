package no.nav.helse.flex.api

import no.nav.helse.flex.FellesTestOppsett
import no.nav.helse.flex.no.nav.helse.flex.bucket.BucketClient
import org.amshove.kluent.`should be equal to`
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers

internal class FrontendApiTest : FellesTestOppsett() {

    @Autowired
    private lateinit var bucketClient: BucketClient

    @Test
    fun `Hent kvittering`() {

        bucketClient.lagreBlob(
            "blob-1",
            MediaType.IMAGE_JPEG,
            mapOf("fnr" to "fnr-1"),
            hentTestbilde("example.jpg").bytes
        )

        val response = mockMvc.perform(
            MockMvcRequestBuilders.get("/kvittering/blob-1")
                .header("Authorization", "Bearer ${loginserviceToken("fnr-1")}")
        ).andExpect(MockMvcResultMatchers.status().isOk).andReturn().response

        response.contentType `should be equal to` MediaType.IMAGE_JPEG_VALUE
        response.contentLength `should be equal to` 913449
    }
}
