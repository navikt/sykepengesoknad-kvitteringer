package no.nav.syfo.models

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.response.*

data class VedleggRespons(val id: String? = null, val melding: String)

fun VedleggRespons.toJson(): String {
    val objectMapper = ObjectMapper()
    return objectMapper.writeValueAsString(this)
}

suspend inline fun ApplicationCall.jsonStatus(statusCode: HttpStatusCode = HttpStatusCode.OK, id: String? = null, message: String) {
    respond(
        statusCode,
        TextContent(
            VedleggRespons(id, message).toJson(),
            ContentType.Application.Json.withCharset(Charsets.UTF_8)
        )
    )
}
