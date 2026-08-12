package no.nav.dagpenger.mellom.barken.og.veden.utbetaling.jobber

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.Status
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.helved.HelvedUtsender
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.helved.mapToVedtakDTO
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.helved.tilBase64
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.helved.toJson
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.repository.UtbetalingRepo
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.repository.VedtakSomVenterPåSvar
import no.nav.dagpenger.mellom.barken.og.veden.varsling.GjentagendeVarsling
import java.time.Duration

class UtsendingsHjelper(
    private val repo: UtbetalingRepo,
    private val utsender: HelvedUtsender,
    private val rapidsConnection: RapidsConnection,
    private val varslingOmHengendeUtbetaling: GjentagendeVarsling =
        GjentagendeVarsling(
            // Hvor lenge en utbetaling må ha ventet på svar fra oppdrag før vi varsler om at den henger.
            minsteVentetidFørVarsel = Duration.ofHours(1),
            // Hvor ofte vi gjentar varselet for en utbetaling som fortsatt henger (for å unngå spam).
            tidMellomGjentatteVarsler = Duration.ofHours(1),
            // MÅ være like stor eller større enn hvor ofte behandleUtbetalingVedtak() faktisk kjøres
            // (se BehandleMottatteUtbetalinger.KJØREINTERVALL), ellers kan et varslingspunkt bli
            // hoppet over mellom to kjøringer.
            toleransevindu = BehandleMottatteUtbetalinger.KJØREINTERVALL,
        ),
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
                    val ventendeUtbetalinger = repo.hentUtbetalingerSomVenterPåSvar(vedtak.sakId)
                    if (ventendeUtbetalinger.isNotEmpty()) {
                        logger.info { "Det finnes allerede en utbetaling som er sendt til oppdrag for denne saken, hopper over" }
                        ventendeUtbetalinger.forEach { varsleOmDenHengerHvisNødvendig(it) }
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

    private fun varsleOmDenHengerHvisNødvendig(ventende: VedtakSomVenterPåSvar) {
        val vedtak = ventende.vedtak
        val vurdering = varslingOmHengendeUtbetaling.vurder(ventende.sistEndretTilstand)
        if (!vurdering.skalVarsleNå) return

        logger.warn {
            "Utbetaling har ventet på svar fra oppdrag i ${vurdering.ventetid.toMinutes()} minutter, " +
                "varsler for ${vurdering.antallVarslerSåLangt}. gang"
        }
        rapidsConnection.publish(
            vedtak.person.ident,
            JsonMessage
                .newMessage(
                    mapOf(
                        "@event_name" to "utbetaling_henger",
                        "behandlingId" to vedtak.behandlingId,
                        "sakId" to vedtak.sakId,
                        "ventetidMinutter" to vurdering.ventetid.toMinutes(),
                        "antallVarslerSåLangt" to vurdering.antallVarslerSåLangt,
                    ),
                ).toJson(),
        )
    }
}
