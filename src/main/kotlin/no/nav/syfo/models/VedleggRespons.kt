package no.nav.syfo.models

import com.fasterxml.jackson.databind.ObjectMapper

data class VedleggRespons(val id: String? = null, val melding: String)


fun VedleggRespons.toJson(): String {
    val objectMapper = ObjectMapper()
    return objectMapper.writeValueAsString(this)
}
