package no.nav.dagpenger.mellom.barken.og.veden.utbetaling.jobber

import mu.KLogger
import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.mellom.barken.og.veden.helved.HelvedUtsender
import no.nav.dagpenger.mellom.barken.og.veden.helved.mapToVedtakDTO
import no.nav.dagpenger.mellom.barken.og.veden.helved.toJson
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.UtbetalingStatus
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.repository.UtbetalingRepo

class UtsendingsHjelper(
    val repo: UtbetalingRepo,
    val producer: HelvedUtsender,
) {
    companion object {
        private val logger = KotlinLogging.logger { }
    }

    private fun KLogger.sikkerlogg() = KotlinLogging.logger("tjenestekall.$name")

    fun behandleUtbetalingVedtak() {
        repo.hentAlleVedtakMedStatus(UtbetalingStatus.MOTTATT).forEach { vedtak ->
            withLoggingContext(
                mapOf(
                    "behandlingId" to vedtak.behandlingId.toString(),
                    "sakId" to vedtak.sakId,
                ),
            ) {
                val json = vedtak.mapToVedtakDTO().toJson()
                logger.info { "Sender utbetaling til helved" }
                producer.send(vedtak.ident, json)
                logger.info { "Har sendt utbetaling til helved" }
                logger.sikkerlogg().info { "Utbetaling som er sendt til helved: $json" }

                repo.oppdaterStatus(vedtak.behandlingId, UtbetalingStatus.SENDT)
            }
        }
    }
}
