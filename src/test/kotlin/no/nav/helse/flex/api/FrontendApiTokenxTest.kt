package no.nav.helse.flex.api

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.helse.flex.FellesTestOppsett
import no.nav.helse.flex.PROSESSERT_BILDE_BYTE_SIZE
import no.nav.helse.flex.no.nav.helse.flex.api.VedleggRespons
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
internal class FrontendApiTokenxTest : FellesTestOppsett() {

    private lateinit var kvitteringId: String

    @Test
    @Order(1)
    fun `Last opp kvittering som bruker`() {
        val bilde = hentTestbilde("example.jpg")
        val multipartFile = MockMultipartFile("file", null, bilde.contentType.toString(), bilde.bytes)

        val response = mockMvc.perform(
            multipart("/api/v2/opplasting")
                .file(multipartFile)
                .header("Authorization", "Bearer ${tokenxToken("fnr-1")}")
        ).andExpect(status().isCreated).andReturn().response

        val vedleggRespons: VedleggRespons = objectMapper.readValue(response.contentAsString)
        vedleggRespons.id.shouldNotBeNullOrEmpty()

        kvitteringId = vedleggRespons.id!!
    }

    @Test
    @Order(2)
    fun `Hent kvittering som bruker`() {
        val response = mockMvc.perform(
            get("/api/v2/kvittering/$kvitteringId")
                .header("Authorization", "Bearer ${tokenxToken("fnr-1")}")
        ).andExpect(status().isOk).andReturn().response

        response.contentType `should be equal to` MediaType.IMAGE_JPEG_VALUE
        response.contentLength `should be equal to` PROSESSERT_BILDE_BYTE_SIZE
    }

    @Test
    @Order(3)
    fun `Hent kvittering med feil bruker`() {
        mockMvc.perform(
            get("/api/v2/kvittering/$kvitteringId")
                .header("Authorization", "Bearer ${tokenxToken("fnr-2")}")
        ).andExpect(status().isForbidden)
    }
}
