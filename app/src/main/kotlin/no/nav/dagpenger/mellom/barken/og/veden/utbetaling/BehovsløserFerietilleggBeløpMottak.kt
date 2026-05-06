package no.nav.dagpenger.mellom.barken.og.veden.utbetaling

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.repository.UtbetalingRepo
import java.time.LocalDate
import java.time.Year

internal class BehovsløserFerietilleggBeløpMottak(
    rapidsConnection: RapidsConnection,
    private val repo: UtbetalingRepo,
) : River.PacketListener {
    private companion object {
        private val sikkerLogg = KotlinLogging.logger("tjenestekall.BehovMediator")
        private val log = KotlinLogging.logger {}
        const val BEHOV = "FerietilleggBeløp"
    }

    init {
        River(rapidsConnection)
            .apply {
                precondition {
                    it.requireValue("@event_name", "behov")
                    it.requireAllOrAny("@behov", listOf(BEHOV))
                    it.forbid("@løsning")
                }
                validate { it.requireKey("ident", "opptjeningsår") }
                validate { it.interestedIn("@id", "@opprettet", "@behovId") }
            }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val ident = packet["ident"].asText()
        val opptjeningsår = packet["opptjeningsår"].asInt()

        withLoggingContext(
            "behovId" to packet["@behovId"].asText(),
        ) {
            log.info { "Skal løse behov '$BEHOV" }
            sikkerLogg.info { "Skal løse behov '$BEHOV' for ident '$ident' og opptjeningsår '$opptjeningsår'" }

            val vedtak = repo.hentAlleFerdigeUtenFerietilleggForIdent(ident, Year.of(opptjeningsår))

            // Finner alle utbetalinger for alle dager i opptjeningsåret det spørres om, for alle saker
            val sumForAlleDagene =
                vedtak
                    .flatMap { it.utbetalinger }
                    .filter { it.dato.year == opptjeningsår }
                    .sumOf { it.utbetaltBeløp }

            val løsning =
                mapOf(
                    "verdi" to sumForAlleDagene,
                    "gyldigFraOgMed" to LocalDate.of(opptjeningsår, 1, 1),
                )

            packet["@løsning"] =
                mapOf(
                    BEHOV to løsning,
                )

            sikkerLogg.info {
                "Har løst behov '$BEHOV' for ident '$ident' og opptjeningsår '$opptjeningsår' med verdi '${løsning["verdi"]}'"
            }
            log.info { "har løst behov '$BEHOV'" }
            context.publish(packet.toJson())
        }
    }
}
