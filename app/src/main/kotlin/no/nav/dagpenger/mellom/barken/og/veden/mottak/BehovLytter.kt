package no.nav.dagpenger.mellom.barken.og.veden.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import mu.KotlinLogging.logger

internal class BehovLytter(
    rapidsConnection: RapidsConnection,
//    private val hendelseRepository: HendelseRepository,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                precondition {
                    it.requireValue("@event_name", "behov")
                    it.requireValue("@opplysningsbehov", true)
                }
                validate {
                    it.requireKey(
                        "@opprettet",
                        "@behov",
                        "@behovId",
                        "behandlingId",
                    )
                    it.interestedIn("@final", "@løsning")
                }
            }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        logger.info { "Mottok melding" }
    }

    private companion object {
        private val logger = logger { }
    }
}
