package no.nav.syfo.utils

import io.ktor.util.KtorExperimentalAPI
import io.ktor.server.testing.*
import io.ktor.http.*
import org.amshove.kluent.shouldEqual
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
                engine.handleRequest(HttpMethod.Get, "/list") {
                    medSelvbetjeningToken(fnr, level = "level3")
                }
            ) {
                response.status() shouldEqual HttpStatusCode.Unauthorized
            }
        }
    }

    @Test
    fun `Nivå 4 token returnerer 200`() {
        with(testApp) {
            with(
                engine.handleRequest(HttpMethod.Get, "/list") {
                    medSelvbetjeningToken(fnr)
                }
            ) {
                response.status() shouldEqual HttpStatusCode.OK
            }
        }
    }
}
