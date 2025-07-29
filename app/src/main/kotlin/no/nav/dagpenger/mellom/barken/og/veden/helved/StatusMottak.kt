package no.nav.dagpenger.mellom.barken.og.veden.helved

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import mu.KLogger
import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.mellom.barken.og.veden.objectMapper
import no.nav.dagpenger.mellom.barken.og.veden.repository.Repo
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.Status
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.repository.UtbetalingRepo
import java.util.UUID

internal class StatusMottak(
    rapidsConnection: RapidsConnection,
    private val utbetalingRepo: UtbetalingRepo,
    private val repo: Repo,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                precondition { message -> message.requireAny("status", StatusReply.Status.entries.map { it.name }) }
            }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val statusDto: StatusReply =
            objectMapper.treeToValue(objectMapper.readTree(packet.toJson()), StatusReply::class.java)
        val behandlingId =
            metadata.key?.let { BehandlingId(UUID.fromString(it)) }
                ?: throw IllegalStateException("Mangler nøkkel i metadata, kan ikke prosessere melding uten behandlingId")

        withLoggingContext(
            "behandlingId" to behandlingId.uuid.toString(),
        ) {
            logger.info("Fått statusmelding: ${packet["status"].asText()} for behandlingId: $behandlingId")
            logger.sikkerlogg().info("Fått statusmelding: ${packet.toJson()}, nøkkel: ${metadata.key}")

            utbetalingRepo.hentVedtak(behandlingId) ?: run {
                logger.warn { "Ukjent behandlingId: $behandlingId ignorerer denne" }
                return@withLoggingContext
            }

            val status =
                when (statusDto.status) {
                    StatusReply.Status.OK -> Status.Ferdig
                    StatusReply.Status.FEILET -> Status.TilUtbetaling(Status.UtbetalingStatus.FEILET)
                    StatusReply.Status.HOS_OPPDRAG -> Status.TilUtbetaling(Status.UtbetalingStatus.HOS_OPPDRAG)
                    StatusReply.Status.MOTTATT -> Status.TilUtbetaling(Status.UtbetalingStatus.MOTTATT)
                }

            repo.lagreStatusFraHelved(
                behandlingId = behandlingId,
                status = status,
                svar = statusDto,
            )
        }
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
    }

    private fun KLogger.sikkerlogg() = KotlinLogging.logger("tjenestekall.$name")
}
