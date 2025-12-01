package no.nav.dagpenger.mellom.barken.og.veden.leaderelection

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.header
import no.nav.dagpenger.mellom.barken.og.veden.Configuration
import no.nav.dagpenger.mellom.barken.og.veden.objectMapper

class LeaderElectionClient(
    private val electorPath: String,
    engine: HttpClientEngine = CIO.create { },
) {
    private val logger = KotlinLogging.logger { }
    private val localHostName = Configuration.localHostname
    private val httpClient =
        HttpClient(engine) {
            expectSuccess = true
        }

    suspend fun amITheLeader(): Boolean {
        try {
            val response =
                httpClient
                    .get(electorPath) {
                        header("Content-Type", "application/json")
                        header("Accept", "application/json")
                    }

            if (response.status.value in 200..299) {
                val leaderElection = objectMapper.readTree(response.body<String>()).get("name").asText(null)
                val leader = leaderElection == localHostName
                logger.debug { "$localHostName fant ElectionLeader : $leaderElection. Leaderelection er $leader" }
                return leader
            } else {
                return false
            }
        } catch (e: Exception) {
            logger.error(e) { "Feil ved henting av leader election" }
            return false
        }
    }
}
