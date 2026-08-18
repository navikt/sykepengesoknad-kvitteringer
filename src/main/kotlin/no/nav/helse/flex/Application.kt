package no.nav.helse.flex

import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.restclient.RestTemplateBuilder
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.web.client.RestTemplate
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.kotlinModule

@SpringBootApplication
@EnableJwtTokenValidation
class Application {
    @Bean
    fun restTemplate(restTemplateBuilder: RestTemplateBuilder): RestTemplate = restTemplateBuilder.build()
}

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}

val objectMapper: tools.jackson.databind.ObjectMapper =
    JsonMapper
        .builder()
        .addModule(kotlinModule())
        .build()
