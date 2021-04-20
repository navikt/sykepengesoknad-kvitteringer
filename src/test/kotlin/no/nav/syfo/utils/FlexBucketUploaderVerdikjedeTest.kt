package no.nav.syfo.utils

import io.ktor.util.KtorExperimentalAPI
import io.ktor.server.testing.*
import io.ktor.http.*
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeAll

@KtorExperimentalAPI
internal class FlexBucketUploaderVerdikjedeTest {

    companion object {
        lateinit var testApp: TestApp
        val fnr = "12345678901"

        @BeforeAll
        @JvmStatic
        internal fun beforeAll() {
            testApp = skapTestApplication()
        }
    }
    @Test
    fun `Nivå 3 token returnerer 401`() {
        with(testApp) {
            with(
                engine.handleRequest(HttpMethod.Get, "/kvittering/123") {
                    medSelvbetjeningToken(fnr, level = "level3")
                }
            ) {
                response.status() shouldBeEqualTo HttpStatusCode.Unauthorized
            }
        }
    }
}
