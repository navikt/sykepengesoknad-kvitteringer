package no.nav.helse.flex.api

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.FellesTestOppsett
import no.nav.helse.flex.PROSESSERT_JPEG_SIZE
import no.nav.helse.flex.objectMapper
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.shouldNotBeNullOrEmpty
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Order
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.http.MediaType
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
internal class FrontendApiTest : FellesTestOppsett() {

    private lateinit var kvitteringId: String

    @Test
    @Order(1)
    fun `Last opp kvittering som bruker`() {
        val bilde = hentTestbilde("example.jpg")
        val multipartFile = MockMultipartFile("file", null, bilde.contentType.toString(), bilde.bytes)

        val response = mockMvc.perform(
            multipart("/opplasting")
                .file(multipartFile)
                .header("Authorization", "Bearer ${loginserviceToken("fnr-1")}")
        ).andExpect(status().isCreated).andReturn().response

        val vedleggRespons: VedleggRespons = objectMapper.readValue(response.contentAsString)
        vedleggRespons.id.shouldNotBeNullOrEmpty()

        kvitteringId = vedleggRespons.id!!
    }

    @Test
    @Order(2)
    fun `Hent kvittering som bruker`() {
        val response = mockMvc.perform(
            get("/kvittering/$kvitteringId")
                .header("Authorization", "Bearer ${loginserviceToken("fnr-1")}")
        ).andExpect(status().isOk).andReturn().response

        response.contentType `should be equal to` MediaType.IMAGE_JPEG_VALUE
        response.contentLength `should be equal to` PROSESSERT_JPEG_SIZE
    }

    @Test
    @Order(3)
    fun `Hent kvittering med feil bruker`() {
        mockMvc.perform(
            get("/kvittering/$kvitteringId")
                .header("Authorization", "Bearer ${loginserviceToken("fnr-2")}")
        ).andExpect(status().isForbidden)
    }

    @Test
    @Order(4)
    fun `Hent kvittering som maskin`() {
        val azureToken = azureToken(subject = "sykepengesoknad-backend-client-id")

        val response = mockMvc.perform(
            get("/maskin/kvittering/$kvitteringId")
                .header("Authorization", "Bearer $azureToken")
        ).andExpect(status().isOk).andReturn().response

        response.contentType `should be equal to` MediaType.IMAGE_JPEG_VALUE
        response.contentLength `should be equal to` PROSESSERT_JPEG_SIZE
    }

    @Test
    @Order(5)
    fun `Hent kvittering som feil maskin`() {
        val azureToken = azureToken(subject = "ukjent-client-id")

        val response = mockMvc.perform(
            get("/maskin/kvittering/$kvitteringId")
                .header("Authorization", "Bearer $azureToken")
        ).andExpect(status().isForbidden)
    }

    @Test
    @Order(5)
    fun `Hent kvittering som ikke finnes som maskin`() {
        val azureToken = azureToken(subject = "sykepengesoknad-backend-client-id")

        mockMvc.perform(
            get("/maskin/kvittering/ukjent-kvittering")
                .header("Authorization", "Bearer $azureToken")
        ).andExpect(status().isNotFound)
    }
}
