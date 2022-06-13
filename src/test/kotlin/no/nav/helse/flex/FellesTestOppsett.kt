package no.nav.helse.flex

import no.nav.helse.flex.no.nav.helse.flex.bildeprosessering.Bilde
import org.junit.jupiter.api.TestInstance
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName
import java.nio.file.Files
import java.nio.file.Paths

private const val TESTBILDER = "src/test/resources/bilder/"

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SpringBootTest
abstract class FellesTestOppsett() {
    companion object {

        init {
            GenericContainer(DockerImageName.parse("docker.pkg.github.com/navikt/flex-bildeprosessering/flex-bildeprosessering:latest"))
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
        val bildeFil = Paths.get("$TESTBILDER/$filnavn")

        return Bilde(
            MediaType.parseMediaType(Files.probeContentType(bildeFil)),
            Files.readAllBytes(bildeFil)
        )
    }
}
