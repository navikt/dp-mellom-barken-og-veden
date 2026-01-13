package no.nav.dagpenger.mellom.barken.og.veden.utbetaling.helved

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
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
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.UtbetalingVedtak
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.helved.repository.Repo
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.repository.UtbetalingRepo
import java.time.LocalDate
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
            logger.info { "Mottok statusmelding for annet fagsystem ($fagsystem), skal ignorere denne" }
        }
        val behandlingId =
            metadata.key?.let { UUID.fromString(it) }
                ?: throw IllegalStateException("Mangler nøkkel i metadata, kan ikke prosessere melding uten behandlingId")

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

            // Verifiser at grensedatoene for utbetalingen til Helved stemmer med behandlingen
            validerGrensedatoer(context, packet, utbetalingVedtak, behandlingId)

            val status =
                when (statusDto.status) {
                    StatusReply.Status.OK -> Status.Ferdig(Status.UtbetalingStatus.OK)
                    StatusReply.Status.MOTTATT -> Status.TilUtbetaling(Status.UtbetalingStatus.MOTTATT)
                    StatusReply.Status.HOS_OPPDRAG -> Status.TilUtbetaling(Status.UtbetalingStatus.HOS_OPPDRAG)
                    StatusReply.Status.FEILET -> Status.TilUtbetaling(Status.UtbetalingStatus.FEILET)
                }

            repo.lagreStatusFraHelved(
                behandlingId = behandlingId,
                status = status,
                svar = statusDto,
                json = json,
            )
            context.publish(
                utbetalingVedtak.person.ident,
                UtbetalingStatusHendelse(
                    behandlingId = behandlingId,
                    ident = utbetalingVedtak.person.ident,
                    sakId = utbetalingVedtak.sakId,
                    behandletHendelseId = utbetalingVedtak.behandletHendelseId,
                    status = status,
                ).tilHendelse(),
            )
        }
    }

    private fun validerGrensedatoer(
        context: MessageContext,
        packet: JsonMessage,
        utbetalingVedtak: UtbetalingVedtak,
        behandlingId: UUID,
    ) {
        val førsteDagFraHelVed = packet["detaljer"]["linjer"].minOf { it["fom"].asLocalDate() }
        // Default for eksisterende behandlinger er LocalDate.MIN, hopp over de
        if (utbetalingVedtak.førsteUtbetalingsdag.isEqual(LocalDate.MIN)) return
        if (førsteDagFraHelVed.isEqual(utbetalingVedtak.førsteUtbetalingsdag)) return

        logger.error {
            """Første utbetalingsdag ($førsteDagFraHelVed) fra Hel Ved er ikke lik første 
            |utbetalingsdag (${utbetalingVedtak.førsteUtbetalingsdag}) for behandling $behandlingId
            """.trimMargin()
        }

        context.publish(
            utbetalingVedtak.person.ident,
            JsonMessage.newMessage(
                "utbetaling_feil_grensedato",
                mapOf(
                    "behandlingId" to behandlingId,
                    "sakId" to utbetalingVedtak.sakId,
                    "eksternBehandlingId" to behandlingId.tilBase64(),
                    "eksternSakId" to utbetalingVedtak.sakId.tilBase64(),
                    "førsteUtbetalingsdag" to utbetalingVedtak.førsteUtbetalingsdag,
                    "førsteDagFraHelVed" to førsteDagFraHelVed,
                ),
            ).toJson(),
        )
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
    }

    private fun KLogger.sikkerlogg() = KotlinLogging.logger("tjenestekall.$name")
}
