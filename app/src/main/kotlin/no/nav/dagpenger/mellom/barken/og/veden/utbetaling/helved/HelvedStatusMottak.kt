package no.nav.dagpenger.mellom.barken.og.veden.utbetaling.helved

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KLogger
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.mellom.barken.og.veden.objectMapper
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.Status
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.UtbetalingStatusHendelse
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.helved.repository.Repo
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.repository.UtbetalingRepo
import java.util.UUID

internal class HelvedStatusMottak(
    rapidsConnection: RapidsConnection,
    private val utbetalingRepo: UtbetalingRepo,
    private val repo: Repo,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                precondition { message -> message.requireAny("status", StatusReply.Status.entries.map { it.name }) }
                // unngå kollisjon med andre eventer sendt på rapiden med samme "status" felt
                precondition { message -> message.forbid("@event_name") }
                validate { it.interestedIn("detaljer") }
            }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val fagsystem = metadata.headers["fagsystem"]?.let { String(it) }
        if (fagsystem != "DAGPENGER") {
            logger.info { "Mottok statusmelding for annet fagsystem ($fagsystem) ignorere denne, nøkkel ${metadata.key}" }
            return
        }
        // logger meldinger midlertidig for å se hva som er feil med melding i dev
        logger.sikkerlogg().info { "Mottok statusmelding med nøkkel ${metadata.key}, melding: ${packet.toJson()}" }

        val behandlingId =
            metadata.key?.let {
                UUID.fromString(it)
            } ?: throw IllegalStateException("Mangler nøkkel i metadata, kan ikke prosessere melding uten behandlingId")

        val statusDto: StatusReply =
            objectMapper.treeToValue(objectMapper.readTree(packet.toJson()), StatusReply::class.java)

        withLoggingContext(
            "behandlingId" to behandlingId.toString(),
        ) {
            val utbetalingVedtak =
                utbetalingRepo.hentVedtak(behandlingId) ?: run {
                    logger.warn { "Ukjent behandlingId: $behandlingId ignorerer denne" }
                    return@withLoggingContext
                }

            val json = packet.toJson()
            logger.info { "Fått statusmelding: ${packet["status"].asText()}" }
            logger.sikkerlogg().info { "Fått statusmelding: $json, nøkkel: ${metadata.key}" }

            val status =
                when (statusDto.status) {
                    StatusReply.Status.OK -> Status.Ferdig(Status.UtbetalingStatus.OK)
                    StatusReply.Status.MOTTATT -> Status.TilUtbetaling(Status.UtbetalingStatus.MOTTATT)
                    StatusReply.Status.HOS_OPPDRAG -> Status.TilUtbetaling(Status.UtbetalingStatus.HOS_OPPDRAG)
                    StatusReply.Status.FEILET -> Status.TilUtbetaling(Status.UtbetalingStatus.FEILET)
                }

            meterRegistry
                .counter(
                    "dp_utbetaling_helved_status",
                    "status",
                    statusDto.status.name,
                ).increment()

            val lagret =
                repo.lagreStatusFraHelved(
                    behandlingId = behandlingId,
                    status = status,
                    svar = statusDto,
                    json = json,
                )
            if (lagret) {
                context.publish(
                    utbetalingVedtak.person.ident,
                    UtbetalingStatusHendelse(
                        behandlingId = behandlingId,
                        ident = utbetalingVedtak.person.ident,
                        sakId = utbetalingVedtak.sakId,
                        behandletHendelseId = utbetalingVedtak.behandletHendelseId,
                        behandletHendelseType = utbetalingVedtak.behandletHendelseType,
                        status = status,
                    ).tilHendelse(),
                )
            }
        }
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
    }

    private fun KLogger.sikkerlogg() = KotlinLogging.logger("tjenestekall.$name")
}
