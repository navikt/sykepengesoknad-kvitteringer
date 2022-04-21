package no.nav.syfo

import io.ktor.http.*
import io.ktor.server.testing.*
import no.nav.syfo.utils.TestApp
import no.nav.syfo.utils.skapTestApplication
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

internal class SelftestTest {

    companion object {
        lateinit var testApp: TestApp

        @BeforeAll
        @JvmStatic
        internal fun beforeAll() {
            testApp = skapTestApplication()
        }
    }

    @Test
    fun `successfull liveness and readyness`() {
        with(testApp) {
            applicationState.ready = true
            applicationState.alive = true

            with(engine.handleRequest(HttpMethod.Get, "/is_alive")) {
                response.status() shouldBeEqualTo HttpStatusCode.OK
                response.content shouldBeEqualTo "I'm alive! :)"
            }

            with(engine.handleRequest(HttpMethod.Get, "/is_ready")) {
                response.status() shouldBeEqualTo HttpStatusCode.OK
                response.content shouldBeEqualTo "I'm ready! :)"
            }
        }
    }

    @Test
    fun `unsuccessful liveness and readyness`() {
        with(testApp) {
            applicationState.ready = false
            applicationState.alive = false

            with(engine.handleRequest(HttpMethod.Get, "/is_alive")) {
                response.status() shouldBeEqualTo HttpStatusCode.InternalServerError
                response.content shouldBeEqualTo "I'm dead x_x"
            }

            with(engine.handleRequest(HttpMethod.Get, "/is_ready")) {
                response.status() shouldBeEqualTo HttpStatusCode.InternalServerError
                response.content shouldBeEqualTo "Please wait! I'm not ready :("
            }
        }
    }
}
