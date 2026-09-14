package no.nav.dagpenger.mellom.barken.og.veden.utbetaling.api

import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.testing.testApplication
import io.mockk.mockk
import no.nav.dagpenger.mellom.barken.og.veden.TestRapid
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.repository.UtbetalingRepo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Tester [utbetalingApiModule] isolert, uten hverken RapidApplication eller Kafka.
 * Samme modul-funksjon brukes av [no.nav.dagpenger.mellom.barken.og.veden.ApplicationBuilder],
 * så disse testene verifiserer den faktiske oppførselen produksjonsappen får.
 */
internal class UtbetalingApiModuleTest {
    private val repo = mockk<UtbetalingRepo>(relaxed = true)
    private val rapid = TestRapid()

    // Autentisering uten nettverkskall mot Azure AD - passer for tester som ikke sender token.
    private fun testModule(): Application.() -> Unit =
        {
            utbetalingApiModule(
                repo,
                rapid,
                authConfig = { jwt("azureAd") { validate { null } } },
            )
        }

    @Test
    fun `uautentisert kall til rot-endepunktet svarer OK`() =
        testApplication {
            application(testModule())

            val response = client.get("/")

            assertEquals(HttpStatusCode.OK, response.status)
        }

    @Test
    fun `swagger-endepunktet er tilgjengelig uten autentisering`() =
        testApplication {
            application(testModule())

            val response = client.get("/openapi")

            assertEquals(HttpStatusCode.OK, response.status)
        }

    @Test
    fun `kall til utbetaling uten token blir avvist`() =
        testApplication {
            application(testModule())

            val response = client.get("/utbetaling")

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }

    @Test
    fun `kall til utbetaling med ugyldig token blir avvist`() =
        testApplication {
            application(testModule())

            val response =
                client.get("/utbetaling") {
                    header(HttpHeaders.Authorization, "Bearer ikke-et-gyldig-jwt")
                }

            assertEquals(HttpStatusCode.Unauthorized, response.status)
        }
}
