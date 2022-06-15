package no.nav.helse.flex.api

import no.nav.helse.flex.FellesTestOppsett
import no.nav.helse.flex.ORIGINALT_BILDE_BYTE_SIZE
import no.nav.helse.flex.no.nav.helse.flex.bucket.BucketClient
import org.amshove.kluent.`should be equal to`
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.*

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
internal class MaskinApiTest : FellesTestOppsett() {

    private lateinit var kvitteringId: String

    @Autowired
    private lateinit var bucketClient: BucketClient

    @BeforeAll
    fun lagreKvittering() {
        kvitteringId = UUID.randomUUID().toString()
        val bilde = hentTestbilde("example.jpg")
        bucketClient.lagreBlob(kvitteringId, bilde.contentType, mapOf("fnr" to "fnr-1"), bilde.bytes)
    }

    @Test
    @Order(1)
    fun `Hent kvittering`() {
        val azureToken = azureToken(subject = "sykepengesoknad-backend-client-id")

        val response = mockMvc.perform(
            get("/maskin/kvittering/$kvitteringId")
                .header("Authorization", "Bearer $azureToken")
        ).andExpect(status().isOk).andReturn().response

        response.contentType `should be equal to` MediaType.IMAGE_JPEG_VALUE
        response.contentLength `should be equal to` ORIGINALT_BILDE_BYTE_SIZE
    }

    @Test
    @Order(2)
    fun `Hent kvittering med ukjent clientId`() {
        val azureToken = azureToken(subject = "ukjent-client-id")

        mockMvc.perform(
            get("/maskin/kvittering/$kvitteringId")
                .header("Authorization", "Bearer $azureToken")
        ).andExpect(status().isForbidden)
    }

    @Test
    @Order(3)
    fun `Hent kvittering som ikke finnes`() {
        val azureToken = azureToken(subject = "sykepengesoknad-backend-client-id")

        mockMvc.perform(
            get("/maskin/kvittering/ukjent-kvittering")
                .header("Authorization", "Bearer $azureToken")
        ).andExpect(status().isNotFound)
    }

    @Test
    @Order(4)
    fun `Slett kvittering`() {
        val azureToken = azureToken(subject = "sykepengesoknad-backend-client-id")

        mockMvc.perform(
            get("/maskin/slett/$kvitteringId")
                .header("Authorization", "Bearer $azureToken")
        ).andExpect(status().isOk)
    }

    @Test
    @Order(5)
    fun `Slett kvittering som ikke finnes`() {
        val azureToken = azureToken(subject = "sykepengesoknad-backend-client-id")

        mockMvc.perform(
            get("/maskin/slett/$kvitteringId")
                .header("Authorization", "Bearer $azureToken")
        ).andExpect(status().isOk)
    }

    @Test
    @Order(6)
    fun `Hent slettet kvittering`() {
        val azureToken = azureToken(subject = "sykepengesoknad-backend-client-id")

        mockMvc.perform(
            get("/maskin/kvittering/$kvitteringId")
                .header("Authorization", "Bearer $azureToken")
        ).andExpect(status().isNotFound)
    }
}