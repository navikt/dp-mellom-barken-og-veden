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
            metadata.key?.let { UUID.fromString(it) }
                ?: throw IllegalStateException("Mangler nøkkel i metadata, kan ikke prosessere melding uten behandlingId")

        withLoggingContext(
            "behandlingId" to behandlingId.toString(),
        ) {
            logger.info { "Fått statusmelding: ${packet["status"].asText()}" }
            logger.sikkerlogg().info { "Fått statusmelding: ${packet.toJson()}, nøkkel: ${metadata.key}" }
            val utbetalingVedtak =
                utbetalingRepo.hentVedtak(behandlingId) ?: run {
                    logger.warn { "Ukjent behandlingId: $behandlingId ignorerer denne" }
                    return@withLoggingContext
                }

            val status =
                when (statusDto.status) {
                    StatusReply.Status.OK -> Status.Ferdig()
                    StatusReply.Status.MOTTATT -> Status.TilUtbetaling(Status.UtbetalingStatus.MOTTATT)
                    StatusReply.Status.HOS_OPPDRAG -> Status.TilUtbetaling(Status.UtbetalingStatus.HOS_OPPDRAG)
                    StatusReply.Status.FEILET -> Status.TilUtbetaling(Status.UtbetalingStatus.FEILET)
                }

            repo.lagreStatusFraHelved(
                behandlingId = behandlingId,
                status = status,
                svar = statusDto,
            )
            context.publish(
                utbetalingVedtak.person.ident,
                UtbetalingStatusHendelse(
                    behandlingId = behandlingId,
                    ident = utbetalingVedtak.person.ident,
                    sakId = utbetalingVedtak.sakId,
                    meldekortId = utbetalingVedtak.meldekortId,
                    status = status,
                ).tilHendelse(),
            )
        }
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
    }

    private fun KLogger.sikkerlogg() = KotlinLogging.logger("tjenestekall.$name")
}
