package no.nav.syfo.utils

import io.ktor.http.*
import io.ktor.server.testing.*
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

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
