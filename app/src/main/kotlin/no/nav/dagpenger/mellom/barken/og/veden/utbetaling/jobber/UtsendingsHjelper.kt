package no.nav.dagpenger.mellom.barken.og.veden.utbetaling.jobber

import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.Status
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.helved.HelvedUtsender
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.helved.mapToVedtakDTO
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.helved.tilBase64
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.helved.toJson
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
                        "behandlingId" to vedtak.behandlingId.toString(),
                        "helvedBehandlingId" to vedtak.behandlingId.tilBase64(),
                        "sakId" to vedtak.sakId.toString(),
                        "helvedSakId" to vedtak.sakId.tilBase64(),
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

                    repo.lagreMelding(
                        behandlingId = vedtak.behandlingId,
                        json = json,
                        type = "UTBETALING_SENDT_TIL_HELVED",
                    )

                    repo.oppdaterStatus(vedtak.behandlingId, Status.TilUtbetaling(Status.UtbetalingStatus.SENDT))
                }
            }
    }
}
