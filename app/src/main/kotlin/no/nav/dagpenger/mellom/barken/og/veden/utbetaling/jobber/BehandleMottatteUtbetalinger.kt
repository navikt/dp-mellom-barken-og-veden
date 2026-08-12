package no.nav.dagpenger.mellom.barken.og.veden.utbetaling.jobber

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.mellom.barken.og.veden.leaderelection.LeaderElectionClient
import kotlin.concurrent.fixedRateTimer
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toJavaDuration

class BehandleMottatteUtbetalinger(
    private val leaderElection: LeaderElectionClient,
    private val utsendingsHjelper: UtsendingsHjelper,
) {
    private val logger = KotlinLogging.logger { }

    companion object {
        // Hvor ofte behandleUtbetalingVedtak() kjøres. UtsendingsHjelper bruker denne til å vite hvor bredt
        // toleransevindu den trenger for å garantere å oppdage at ventetiden har passert et varslingspunkt.
        val KJØREINTERVALL = 1.minutes.toJavaDuration()
    }

    fun start() {
        fixedRateTimer(
            name = "Behandle mottatte utbetalinger",
            daemon = true,
            initialDelay = KJØREINTERVALL.toMillis(),
            period = KJØREINTERVALL.toMillis(),
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
