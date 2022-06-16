package no.nav.helse.flex.no.nav.helse.flex.bildeprosessering

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate

@Component
class BildeprosesseringKlient(
    @Value("\${FLEX_BILDEPROSESSERING_URL}")
    private val bildeprosesseringUrl: String,
    private val restTemplate: RestTemplate
) {

    fun prosesserBilde(bilde: Bilde): Bilde? {
        val headers = HttpHeaders()
        headers.contentType = bilde.contentType

        val response = restTemplate.postForEntity(
            "$bildeprosesseringUrl/prosesser",
            HttpEntity(bilde.bytes, headers),
            ByteArray::class.java
        )

        return Bilde(MediaType.IMAGE_JPEG, response.body!!)
    }
}

class Bilde(
    val contentType: MediaType,
    val bytes: ByteArray
)
