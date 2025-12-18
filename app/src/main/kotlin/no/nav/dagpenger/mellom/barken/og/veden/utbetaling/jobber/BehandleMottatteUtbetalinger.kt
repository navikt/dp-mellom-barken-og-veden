package no.nav.dagpenger.mellom.barken.og.veden.utbetaling.jobber

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.mellom.barken.og.veden.leaderelection.LeaderElectionClient
import kotlin.concurrent.fixedRateTimer
import kotlin.time.Duration.Companion.minutes

class BehandleMottatteUtbetalinger(
    private val leaderElection: LeaderElectionClient,
    private val utsendingsHjelper: UtsendingsHjelper,
) {
    private val logger = KotlinLogging.logger { }

    fun start() {
        fixedRateTimer(
            name = "Behandle mottatte utbetalinger",
            daemon = true,
            initialDelay = 1.minutes.inWholeMilliseconds,
            period = 1.minutes.inWholeMilliseconds,
            action = {
                action()
            },
        )
    }

    private fun action() {
        val amILeader =
            runBlocking {
                leaderElection.amITheLeader()
            }
        if (amILeader) {
            try {
                logger.info { "Skal behandle mottatte utbetalinger" }
                utsendingsHjelper.behandleUtbetalingVedtak()
            } catch (e: Exception) {
                logger.error(e) { "Behandle utbetaling feilet" }
            }
        }
    }
}
