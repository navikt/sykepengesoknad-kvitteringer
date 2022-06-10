package no.nav.helse.flex

import org.junit.jupiter.api.TestInstance
import org.springframework.boot.test.context.SpringBootTest
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.DockerImageName

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
}
