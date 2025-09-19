package no.nav.dagpenger.mellom.barken.og.veden.utbetaling

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.URLBuilder
import io.ktor.http.appendPathSegments
import no.nav.dagpenger.saksbehandling.api.models.HttpProblemDTO
import java.util.UUID

internal class SakIdHenter(
    private val baseUrl: String,
    private val tokenProvider: () -> String?,
    httpClientEngine: HttpClientEngine = CIO.create {},
) {
    private companion object {
        private val log = KotlinLogging.logger {}
    }

    private val client: HttpClient =
        HttpClient(httpClientEngine) {
            expectSuccess = true
            defaultRequest {
                header("Nav-Consumer-Id", "dp-mellom-barken-og-veden")
                header(HttpHeaders.Authorization, "Bearer ${tokenProvider.invoke()}")
            }
        }

    suspend fun hentSakId(behandlingId: UUID): UUID {
        val url = URLBuilder(baseUrl).appendPathSegments("behandling", behandlingId.toString(), "sakId").build()
        try {
            val sakId =
                client
                    .get(url)
                    .bodyAsText()
                    .let { body -> UUID.fromString(body) }
            log.info { "Hentet sakId=$sakId for behandlingId=$behandlingId" }
            return sakId
        } catch (error: Exception) {
            when (error) {
                is io.ktor.client.plugins.ServerResponseException -> {
                    val body = error.response.body<HttpProblemDTO>()
                    log.error(error) { "Feil ved kall mot $url. Feilmelding: $body" }
                }

                else -> log.error(error) { "Uventet feil ved henting av sakId for behandlingId=$behandlingId" }
            }
            throw error
        }
    }
}
