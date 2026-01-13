package no.nav.dagpenger.mellom.barken.og.veden.utbetaling

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.behandling.api.models.BehandletAvDTORolleDTO
import no.nav.dagpenger.behandling.api.models.BehandlingsresultatDTO
import no.nav.dagpenger.mellom.barken.og.veden.asUUID
import no.nav.dagpenger.mellom.barken.og.veden.objectMapper
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.repository.UtbetalingRepo
import java.time.LocalDateTime
import java.util.UUID

internal class MeldingOmUtbetalingVedtakMottak(
    rapidsConnection: RapidsConnection,
    private val sakIdHenter: SakIdHenter,
    private val repo: UtbetalingRepo,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                precondition {
                    it.requireValue("@event_name", "behandlingsresultat")
                    // kanskje vi også vil ta vare på "rammevedtaket"?
//                    it.requireValue("behandletHendelse.type", "Meldekort")
                }
                validate {
                    it.requireKey(
                        "behandlingId",
                        "basertPå",
                        "ident",
                        "opplysninger",
                        "behandletHendelse",
                    )
                }
                validate { it.interestedIn("@id", "@opprettet", "behandletAv", "basertPå") }
            }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val behandlingId = packet["behandlingId"].asUUID()
        val hendelseId = packet["behandletHendelse"]["id"].asText()

        withLoggingContext(
            "behandlingId" to behandlingId.toString(),
            "hendelseId" to hendelseId.toString(),
        ) {
            logger.info { "Mottok melding om utbetaling for meldekort" }
            // her kan vi kalle dp-behandling for å hente utbetalinger
            val behandlingsresultatDTO: BehandlingsresultatDTO =
                objectMapper.treeToValue(objectMapper.readTree(packet.toJson()), BehandlingsresultatDTO::class.java)

            if (behandlingsresultatDTO.utbetalinger.isEmpty()) {
                logger.info { "Ingen utbetalinger å lagre for behandling=$behandlingId" }
                return@withLoggingContext
            }

            val sakId: UUID =
                try {
                    runBlocking { sakIdHenter.hentSakId(behandlingId) }
                } catch (e: Exception) {
                    logger.error(e) { "Klarte ikke hente sakId for behandling=$behandlingId" }
                    if (System.getenv("NAIS_CLUSTER_NAME") == "prod-gcp") throw e else return@withLoggingContext
                }

            val utbetalingVedtak =
                UtbetalingVedtak(
                    behandlingId = behandlingId,
                    basertPåBehandlingId = behandlingsresultatDTO.basertPå,
                    vedtakstidspunkt = packet["@opprettet"].asLocalDateTime(),
                    behandletHendelseId = behandlingsresultatDTO.behandletHendelse.id,
                    sakId = sakId,
                    person = Person(behandlingsresultatDTO.ident),
                    saksbehandletAv =
                        behandlingsresultatDTO.behandletAv
                            .singleOrNull { it.rolle == BehandletAvDTORolleDTO.SAKSBEHANDLER }
                            ?.behandler
                            ?.ident
                            ?: "dp-behandling",
                    besluttetAv =
                        behandlingsresultatDTO.behandletAv
                            .singleOrNull { it.rolle == BehandletAvDTORolleDTO.BESLUTTER }
                            ?.behandler
                            ?.ident
                            ?: "dp-behandling",
                    utbetalinger =
                        behandlingsresultatDTO.utbetalinger.map { utbetaling ->
                            Utbetalingsdag(
                                meldeperiode = utbetaling.meldeperiode,
                                dato = utbetaling.dato,
                                sats = utbetaling.sats,
                                utbetaltBeløp = utbetaling.utbetaling,
                                opprinnelse = utbetaling.opprinnelse?.let { Opprinnelse.valueOf(it.value) } ?: Opprinnelse.Ukjent,
                            )
                        },
                    status =
                        Status.Mottatt(
                            opprettet = LocalDateTime.now(),
                        ),
                    opprettet = packet["@opprettet"].asLocalDateTime(),
                )

            sikkerlogger.info {
                "Skal lagre dager for utbetaling=${utbetalingVedtak.utbetalinger}"
            }
            repo.lagreVedtak(utbetalingVedtak)
            context.publish(
                utbetalingVedtak.person.ident,
                UtbetalingStatusHendelse(
                    behandlingId = behandlingId,
                    ident = utbetalingVedtak.person.ident,
                    sakId = utbetalingVedtak.sakId,
                    behandletHendelseId = utbetalingVedtak.behandletHendelseId,
                    status = utbetalingVedtak.status,
                ).tilHendelse(),
            )
            logger.info { "Utbetalingsvedtak lagret" }
        }
    }

    private companion object {
        private val logger = KotlinLogging.logger { }
        private val sikkerlogger = KotlinLogging.logger("tjenestekall.MeldingOmUtbetalingVedtakMottak")
    }
}
