package no.nav.dagpenger.mellom.barken.og.veden.jobber

import mu.KotlinLogging
import no.nav.dagpenger.mellom.barken.og.veden.domene.UtbetalingId
import no.nav.dagpenger.mellom.barken.og.veden.domene.UtbetalingStatus
import no.nav.dagpenger.mellom.barken.og.veden.domene.UtbetalingVedtak
import no.nav.dagpenger.mellom.barken.og.veden.repository.UtbetalingRepo
import no.nav.helved.kontrakt.api.models.UtbetalingDTO
import no.nav.helved.kontrakt.api.models.UtbetalingsdagDTO

class UtsendingsHjelper(
    val repo: UtbetalingRepo,
) {
    companion object {
        private val logger = KotlinLogging.logger { }
    }

    fun behandleUtbetalingVedtak() {
        repo.hentAlleVedtakMedStatus(UtbetalingStatus.MOTTATT).forEach { vedtak ->
            val dto = mapToVedtakDTO(vedtak)
            // send dto til Kafka
            logger.info { "Sender vedtak til Kafka: $dto" }

            repo.oppdaterStatus(vedtak.behandlingId, UtbetalingStatus.SENDT)
        }
    }

    private fun mapToVedtakDTO(vedtak: UtbetalingVedtak): UtbetalingDTO =
        UtbetalingDTO(
            behandlingId = UtbetalingId(vedtak.behandlingId).toString(),
            sakId = vedtak.sakId,
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
