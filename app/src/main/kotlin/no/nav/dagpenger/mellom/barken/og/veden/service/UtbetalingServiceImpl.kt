package no.nav.dagpenger.mellom.barken.og.veden.service

import mu.KotlinLogging
import no.nav.dagpenger.mellom.barken.og.veden.domene.UtbetalingStatus
import no.nav.dagpenger.mellom.barken.og.veden.domene.UtbetalingVedtak
import no.nav.dagpenger.mellom.barken.og.veden.repository.UtbetalingRepo

class UtbetalingServiceImpl(
    val repo: UtbetalingRepo,
) : UtbetalingService {
    companion object {
        private val logger = KotlinLogging.logger { }
    }

    override fun mottaUtbetalingVedtak(vedtak: UtbetalingVedtak) {
        repo.lagreVedtak(vedtak)
    }

    override fun behandleUtbetalingVedtak() {
        var vedtak = hentNesteVedtak()

        while (vedtak != null) {
            val dto = mapToVedtakDTO(vedtak)

            repo.oppdaterStatus(vedtak.behandlingId, UtbetalingStatus.SENDER_TIL_UTBETALING)
            // send dto til Kafka
            logger.info { "Sender vedtak til Kafka: $dto" }

            vedtak.status = UtbetalingStatus.SENDT
            repo.oppdaterStatus(vedtak.behandlingId, vedtak.status)

            vedtak = hentNesteVedtak()
        }
    }

    private fun hentNesteVedtak(): UtbetalingVedtak? =
        repo.hentNesteVedtakMedStatus(UtbetalingStatus.MOTTATT)?.let { vedtak ->
            vedtak.status = UtbetalingStatus.SENDER_TIL_UTBETALING
            repo.oppdaterStatus(vedtak.behandlingId, vedtak.status)
            vedtak
        }

    private fun mapToVedtakDTO(vedtak: UtbetalingVedtak): VedtakDTO =
        VedtakDTO(
            behandlingId = vedtak.behandlingId.toString(),
            meldekortId = vedtak.meldekortId,
            ident = vedtak.ident,
            utbetalinger =
                vedtak.utbetalinger.map { dag ->
                    UtbetalingsdagDTO(
                        meldeperiode = dag.meldeperiode,
                        dato = dag.dato.toString(),
                        sats = dag.sats,
                        utbetaltBeløp = dag.utbetaltBeløp,
                    )
                },
        )
}

data class VedtakDTO(
    val behandlingId: String,
    val meldekortId: String,
    val ident: String,
    val utbetalinger: List<UtbetalingsdagDTO>,
)

data class UtbetalingsdagDTO(
    val meldeperiode: String,
    val dato: String,
    val sats: Int,
    val utbetaltBeløp: Int,
)
