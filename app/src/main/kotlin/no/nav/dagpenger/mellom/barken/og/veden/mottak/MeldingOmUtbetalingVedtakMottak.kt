package no.nav.dagpenger.mellom.barken.og.veden.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import mu.KotlinLogging.logger
import mu.withLoggingContext
import no.nav.dagpenger.mellom.barken.og.veden.asUUID
import no.nav.dagpenger.mellom.barken.og.veden.domene.UtbetalingStatus
import no.nav.dagpenger.mellom.barken.og.veden.domene.UtbetalingVedtak
import no.nav.dagpenger.mellom.barken.og.veden.domene.Utbetalingsdag
import no.nav.dagpenger.mellom.barken.og.veden.service.UtbetalingService

internal class MeldingOmUtbetalingVedtakMottak(
    rapidsConnection: RapidsConnection,
    private val service: UtbetalingService,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                precondition {
                    it.requireValue("@event_name", "vedtak_fattet")
                    // kanskje vi også vil ta vare på "rammevedtaket"?
                    it.requireValue("behandletHendelse.type", "Meldekort")
                }
                validate {
                    it.requireKey(
                        "behandlingId",
                        "basertPåBehandlinger",
                        "vedtakstidspunkt",
                        "virkningsdato",
                        "fastsatt",
                        "ident",
                        "behandletAv",
                        "vilkår",
                        "utbetalinger",
                        "opplysninger",
                        "behandletHendelse",
                    )
                }
                validate {
                    it.interestedIn(
                        "gjenstående",
                        "automatisk",
                    )
                }
                validate { it.interestedIn("@id", "@opprettet") }
            }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val behandlingId = packet["behandlingId"].asUUID()
        val meldekortId = packet["behandletHendelse"]["id"].asLong()

        withLoggingContext(
            "behandlingId" to behandlingId.toString(),
            "meldekortId" to meldekortId.toString(),
        ) {
            logger.info { "Mottok melding om utbetaling for meldekort" }
            sikkerlogg.info { "Mottok utbetaling vedtak ${packet.toJson()} " }

            // her kan vi kalle dp-behandling for å hente utbetalinger

            val utbetalingVedtak =
                UtbetalingVedtak(
                    behandlingId = behandlingId,
                    basertPåBehandlingId =
                        packet["basertPåBehandlinger"]
                            .map { it.asUUID() }
                            .lastOrNull(),
                    meldekortId = meldekortId.toString(),
                    ident = packet["ident"].asText(),
                    behandletAv = packet["behandletAv"].asText(),
                    utbetalinger =
                        packet["utbetalinger"].map {
                            Utbetalingsdag(
                                meldeperiode = it["meldeperiode"].asText(),
                                dato = it["dato"].asLocalDate(),
                                sats = it["sats"].asInt(),
                                utbetaltBeløp = it["utbetaling"].asInt(),
                            )
                        },
                    status = UtbetalingStatus.MOTTATT,
                    opprettet = packet["vedtakstidspunkt"].asLocalDateTime(),
                )

            service.mottaUtbetalingVedtak(utbetalingVedtak)
            logger.info { "Utbetaling vedtak lagret" }
        }
    }

    private companion object {
        private val logger = logger { }
        private val sikkerlogg = logger("tjenestekall.MeldingOmUtbetalingVedtakMottak")
    }
}
