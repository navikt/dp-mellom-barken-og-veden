package no.nav.dagpenger.mellom.barken.og.veden.jobber

import mu.KotlinLogging
import no.nav.dagpenger.mellom.barken.og.veden.domene.UtbetalingStatus
import no.nav.dagpenger.mellom.barken.og.veden.domene.UtbetalingVedtak
import no.nav.dagpenger.mellom.barken.og.veden.repository.UtbetalingRepo
import no.nav.helved.kontrakt.api.models.UtbetalingVedtakDTO
import no.nav.helved.kontrakt.api.models.UtbetalingsdagDTO

class UtsendingsHjelper(
    val repo: UtbetalingRepo,
) {
    companion object {
        private val logger = KotlinLogging.logger { }
    }

    fun behandleUtbetalingVedtak() {
        val vedtakDTO =
            repo.hentAlleVedtakMedStatus(UtbetalingStatus.MOTTATT).map {
                mapToVedtakDTO(it)
            }

        vedtakDTO.forEach { vedtak ->
            // send dto til Kafka
            logger.info { "Sender vedtak til Kafka: $vedtak" }

            repo.oppdaterStatus(vedtak.behandlingId, UtbetalingStatus.SENDT)
        }
    }

    private fun mapToVedtakDTO(vedtak: UtbetalingVedtak): UtbetalingVedtakDTO =
        UtbetalingVedtakDTO(
            behandlingId = vedtak.behandlingId,
            meldekortId = vedtak.meldekortId,
            ident = vedtak.ident,
            utbetalinger =
                vedtak.utbetalinger.map { dag ->
                    UtbetalingsdagDTO(
                        meldeperiode = dag.meldeperiode,
                        dato = dag.dato,
                        sats = dag.sats,
                        utbetaltBeløp = dag.utbetaltBeløp,
                    )
                },
        )
}
