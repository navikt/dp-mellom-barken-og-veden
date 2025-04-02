package no.nav.dagpenger.mellom.barken.og.veden.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import mu.KotlinLogging.logger
import mu.withLoggingContext
import no.nav.dagpenger.mellom.barken.og.veden.asUUID

internal class MeldingOmUtbetalingVedtakMottak(
    rapidsConnection: RapidsConnection,
//    private val hendelseRepository: HendelseRepository,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                precondition {
                    it.requireValue("@event_name", "vedtak_fattet")
                    it.requireValue("behandletHendelse.type", "Meldekort")
                }
                validate {
                    it.requireKey(
                        "behandlingId",
                        "fagsakId",
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
        }
    }

    private companion object {
        private val logger = logger { }
    }
}
