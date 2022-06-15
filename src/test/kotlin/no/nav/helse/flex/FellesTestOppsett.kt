package no.nav.helse.flex

import no.nav.helse.flex.no.nav.helse.flex.bildeprosessering.Bilde
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*

private const val TESTBILDER = "src/test/resources/bilder/"
const val ORIGINALT_BILDE_BYTE_SIZE = 913449
const val PROSESSERT_BILDE_BYTE_SIZE = 64742

@SpringBootTest
@AutoConfigureMockMvc
@EnableMockOAuth2Server
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class FellesTestOppsett() {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var mockOAuth2Server: MockOAuth2Server

    companion object {

        init {
            GenericContainer(DockerImageName.parse("ghcr.io/navikt/flex-bildeprosessering/flex-bildeprosessering:latest"))
                .also {
                    it.withExposedPorts(8080)
                }
                .also {
                    it.start()
                    System.setProperty("FLEX_BILDEPROSESSERING_URL", "http://localhost:${it.firstMappedPort}")
                }
        }
    }

    fun hentTestbilde(filnavn: String): Bilde {
        val bildefil = Paths.get("$TESTBILDER/$filnavn")

        return Bilde(
            MediaType.parseMediaType(Files.probeContentType(bildefil)),
            Files.readAllBytes(bildefil)
        )
    }

    fun hentTestbilde(filnavn: String, contentType: MediaType): Bilde {
        val bildefil = Paths.get("$TESTBILDER/$filnavn")

        return Bilde(
            contentType,
            Files.readAllBytes(bildefil)
        )
    }

    fun loginserviceToken(fnr: String) = mockOAuth2Server.lagToken(subject = fnr)

    fun azureToken(subject: String) = mockOAuth2Server.lagToken(
        subject = subject,
        issuerId = "azureator",
        clientId = subject,
        audience = "flex-bucket-uploader-client-id",
        claims = HashMap<String, String>()
    )

    private fun MockOAuth2Server.lagToken(
        subject: String,
        issuerId: String = "loginservice",
        clientId: String = UUID.randomUUID().toString(),
        audience: String = "loginservice-client-id",
        claims: Map<String, Any> = mapOf("acr" to "Level4"),
    ): String {
        return this.issueToken(
            issuerId,
            clientId,
            DefaultOAuth2TokenCallback(
                issuerId = issuerId,
                subject = subject,
                audience = listOf(audience),
                claims = claims,
                expiry = 3600
            )
        ).serialize()
    }
}
