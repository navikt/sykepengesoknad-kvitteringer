package no.nav.syfo.utils

import com.auth0.jwk.JwkProviderBuilder
import com.google.cloud.storage.Storage
import io.ktor.http.*
import io.ktor.server.testing.*
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import no.nav.syfo.Environment
import no.nav.syfo.PreAuthorizedClient
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.configureApplication
import java.nio.file.Paths

private val selvbetjeningissuer = "TestIssuer"
private val selvbetjeningaudience = "AUD"

class TestApp(
    val engine: TestApplicationEngine,
    val applicationState: ApplicationState
)

fun skapTestApplication(): TestApp {
    val applicationState = ApplicationState()
    val env = mockk<Environment>()
    val storage = mockk<Storage>(relaxed = true)

    fun setupEnvMock() {
        clearAllMocks()
        every { env.cluster } returns "test"
        every { env.loginserviceIdportenAudience } returns "AUD"
        every { env.azureAppPreAuthorizedApps } returns listOf(PreAuthorizedClient("test", "id"))
        every { env.bucketName } returns "test_bucket"
    }
    setupEnvMock()

    val testApplicationEngine = TestApplicationEngine()
    with(testApplicationEngine) {

        val path = "src/test/resources/jwkset.json"
        val uri = Paths.get(path).toUri().toURL()
        val jwkProvider = JwkProviderBuilder(uri).build()
        start()

        application.configureApplication(
            env = env,
            applicationState = applicationState,
            jwkProvider = jwkProvider,
            issuer = selvbetjeningissuer,
            aadClientId = "",
            aadIssuer = "",
            aadProvider = jwkProvider,
            loginserviceIdportenAudience = selvbetjeningaudience,
            storage = storage
        )
    }

    return TestApp(
        engine = testApplicationEngine,
        applicationState = applicationState
    )
}

fun TestApplicationRequest.medSelvbetjeningToken(subject: String, level: String = "Level4") {
    addHeader(
        HttpHeaders.Authorization,
        "Bearer ${
        generateJWT(
            audience = selvbetjeningaudience,
            issuer = selvbetjeningissuer,
            subject = subject,
            level = level
        )
        }"
    )
}
