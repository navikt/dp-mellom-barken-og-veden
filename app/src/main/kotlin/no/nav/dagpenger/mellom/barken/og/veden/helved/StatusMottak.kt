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
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.UtbetalingStatus
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.repository.UtbetalingRepo

internal class StatusMottak(
    rapidsConnection: RapidsConnection,
    private val utbetalingRepo: UtbetalingRepo,
    private val repo: Repo,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                precondition { message -> message.requireAllOrAny("status", StatusReply.Status.entries.map { it.name }) }
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

        val kortBehandlingId =
            statusDto.detaljer
                ?.linjer
                ?.first()
                ?.behandlingId

        val behandlingId = UtbetalingId.fromString(kortBehandlingId!!).uuid

        withLoggingContext(
            "behandlingId" to behandlingId.toString(),
        ) {
            logger.info("Fått statusmelding: ${packet["status"].asText()}")
            logger.sikkerlogg().info("Fått statusmelding: ${packet.toJson()}, nøkkel: ${metadata.key}")

            utbetalingRepo.hentVedtak(behandlingId) ?: run {
                logger.warn { "Ukjent behandlingId: $behandlingId ignorerer denne" }
                return@withLoggingContext
            }

            val status =
                when (statusDto.status) {
                    StatusReply.Status.OK -> UtbetalingStatus.UTBETALT
                    StatusReply.Status.FEILET -> UtbetalingStatus.FEIL
                    StatusReply.Status.HOS_OPPDRAG -> UtbetalingStatus.SENDT_TIL_OPPDRAG
                    StatusReply.Status.MOTTATT -> UtbetalingStatus.MOTTATT_HELVED
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
