package no.nav.helse.flex.api

import no.nav.helse.flex.FellesTestOppsett
import no.nav.helse.flex.no.nav.helse.flex.bucket.BucketKlient
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.*

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
internal class DeleteTest : FellesTestOppsett() {

    private lateinit var kvitteringId: String

    @Autowired
    private lateinit var bucketKlient: BucketKlient

    @BeforeAll
    fun lagreKvittering() {
        kvitteringId = UUID.randomUUID().toString()
        val bilde = hentTestbilde("example.jpg")
        bucketKlient.lagreBlob(kvitteringId, bilde.contentType, mapOf("fnr" to "fnr-1"), bilde.bytes)
    }

    @Test
    @Order(1)
    fun `Slett kvittering`() {
        val azureToken = azureToken(subject = "sykepengesoknad-backend-client-id")

        mockMvc.perform(
            delete("/maskin/slett/$kvitteringId")
                .header("Authorization", "Bearer $azureToken")
        ).andExpect(status().isNoContent)
    }

    @Test
    @Order(2)
    fun `Slett kvittering som ikke finnes`() {
        val azureToken = azureToken(subject = "sykepengesoknad-backend-client-id")

        mockMvc.perform(
            delete("/maskin/slett/$kvitteringId")
                .header("Authorization", "Bearer $azureToken")
        ).andExpect(status().isNoContent)
    }

    @Test
    @Order(3)
    fun `Hent slettet kvittering`() {
        val azureToken = azureToken(subject = "sykepengesoknad-backend-client-id")

        mockMvc.perform(
            get("/maskin/kvittering/$kvitteringId")
                .header("Authorization", "Bearer $azureToken")
        ).andExpect(status().isNotFound)
    }
}
