package no.nav.dagpenger.mellom.barken.og.veden.utbetaling

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headers
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.mellom.barken.og.veden.objectMapper
import no.nav.dagpenger.saksbehandling.api.models.HttpProblemDTOExtra
import org.junit.jupiter.api.Test
import java.util.UUID

class SakIdHenterTest {
    val sakId = UUID.randomUUID()
    val mockEngine =
        MockEngine { _ ->
            respond(
                content = "$sakId",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
            )
        }

    @Test
    fun `henter saksid`() {
        runBlocking {
            val sakIdHenter = SakIdHenter("http://localhost/", { "token " }, mockEngine)
            val behandlingId = UUID.randomUUID()
            val sakId = sakIdHenter.hentSakId(behandlingId)
            sakId shouldBe sakId
        }
    }

    @Test
    fun `henter sakId som feiler`() {
        runBlocking {
            val feilMockEngine =
                MockEngine { _ ->
                    respond(
                        content =
                            objectMapper.writeValueAsString(
                                HttpProblemDTOExtra(
                                    type = "about:blank",
                                    title = "Internal Server Error",
                                    status = 500,
                                    detail = "En uventet feil oppstod",
                                    instance = "/behandling/${UUID.randomUUID()}/sakId",
                                ),
                            ),
                        status = HttpStatusCode.InternalServerError,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                }
            val sakIdHenter = SakIdHenter("http://localhost/", { "token " }, feilMockEngine)
            val behandlingId = UUID.randomUUID()

            shouldThrow<RuntimeException> { sakIdHenter.hentSakId(behandlingId) }
        }
    }
}
