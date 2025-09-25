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
import no.nav.dagpenger.behandling.api.models.VedtakDTO
import no.nav.dagpenger.mellom.barken.og.veden.asUUID
import no.nav.dagpenger.mellom.barken.og.veden.objectMapper
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.repository.UtbetalingRepo
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
                        "vilkår",
                        "utbetalinger",
                        "opplysninger",
                        "behandletHendelse",
                    )
                }
                validate { it.interestedIn("@id", "@opprettet", "behandletAv") }
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
            // her kan vi kalle dp-behandling for å hente utbetalinger
            val vedtakDto: VedtakDTO =
                objectMapper.treeToValue(objectMapper.readTree(packet.toJson()), VedtakDTO::class.java)

            val sakId: UUID = runBlocking { sakIdHenter.hentSakId(behandlingId) }
            val utbetalingVedtak =
                UtbetalingVedtak(
                    behandlingId = behandlingId,
                    basertPåBehandlingId = vedtakDto.basertPåBehandlinger?.lastOrNull(),
                    meldekortId = vedtakDto.behandletHendelse.id,
                    vedtakstidspunkt = vedtakDto.vedtakstidspunkt,
                    sakId = sakId,
                    person = Person(vedtakDto.ident),
                    saksbehandletAv =
                        vedtakDto.behandletAv
                            .singleOrNull { it.rolle == BehandletAvDTORolleDTO.SAKSBEHANDLER }
                            ?.behandler
                            ?.ident
                            ?: "dp-behandling",
                    besluttetAv =
                        vedtakDto.behandletAv
                            .singleOrNull { it.rolle == BehandletAvDTORolleDTO.BESLUTTER }
                            ?.behandler
                            ?.ident
                            ?: "dp-behandling",
                    utbetalinger =
                        vedtakDto.utbetalinger.map { utbetaling ->
                            Utbetalingsdag(
                                meldeperiode = utbetaling.meldeperiode,
                                dato = utbetaling.dato,
                                sats = utbetaling.sats,
                                utbetaltBeløp = utbetaling.utbetaling,
                            )
                        },
                    status =
                        Status.Mottatt(
                            opprettet = packet["@opprettet"].asLocalDateTime(),
                        ),
                    opprettet = packet["@opprettet"].asLocalDateTime(),
                )

            repo.lagreVedtak(utbetalingVedtak)
            context.publish(
                utbetalingVedtak.person.ident,
                UtbetalingStatusHendelse(
                    behandlingId = behandlingId,
                    sakId = utbetalingVedtak.sakId,
                    meldekortId = utbetalingVedtak.meldekortId,
                    status = utbetalingVedtak.status,
                ).tilHendelse(),
            )
            logger.info { "Utbetalingsvedtak lagret" }
        }
    }

    private companion object {
        private val logger = KotlinLogging.logger { }
    }
}
