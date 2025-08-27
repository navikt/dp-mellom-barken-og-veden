package no.nav.dagpenger.mellom.barken.og.veden.utbetaling.jobber

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import no.nav.dagpenger.mellom.barken.og.veden.helved.HelvedUtsender
import no.nav.dagpenger.mellom.barken.og.veden.helved.mapToVedtakDTO
import no.nav.dagpenger.mellom.barken.og.veden.helved.toJson
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.Status
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.repository.UtbetalingRepo

class UtsendingsHjelper(
    private val repo: UtbetalingRepo,
    private val utsender: HelvedUtsender,
) {
    companion object {
        private val logger = KotlinLogging.logger { }
    }

    fun behandleUtbetalingVedtak() {
        repo
            .hentAlleMottatte()
            .sortedBy { it.opprettet }
            .distinctBy { it.sakId }
            .forEach { vedtak ->
                withLoggingContext(
                    mapOf(
                        "behandlingId" to vedtak.behandlingId.uuid.toString(),
                        "sakId" to vedtak.sakId,
                    ),
                ) {
                    if (repo.harUtbetalingerSomVenterPåSvar(vedtak.sakId)) {
                        logger.info { "Det finnes allerede en utbetaling som er sendt til oppdrag for denne saken, hopper over" }
                        return@withLoggingContext
                    }

                    val json = vedtak.mapToVedtakDTO().toJson()
                    logger.info { "Sender utbetaling til helved" }
                    utsender.send(vedtak.behandlingId, json)
                    logger.info { "Har sendt utbetaling til helved" }

                    repo.oppdaterStatus(vedtak.behandlingId, Status.TilUtbetaling(Status.UtbetalingStatus.SENDT))
                }
            }
    }
}
