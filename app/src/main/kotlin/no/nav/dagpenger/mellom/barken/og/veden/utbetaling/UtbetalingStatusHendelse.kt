package no.nav.dagpenger.mellom.barken.og.veden.utbetaling

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.helved.tilBase64
import java.util.UUID

data class UtbetalingStatusHendelse(
    val behandlingId: UUID,
    val ident: String,
    val sakId: UUID,
    val behandletHendelseId: String,
    val behandletHendelseType: String,
    val status: Status,
) {
    fun tilHendelse(suffiks: String = ""): String {
        val eventnavn =
            when (status.type) {
                Status.Type.MOTTATT -> {
                    "utbetaling_mottatt"
                }

                Status.Type.TIL_UTBETALING -> {
                    when ((status as Status.TilUtbetaling).eksternStatus) {
                        Status.UtbetalingStatus.FEILET -> "utbetaling_feilet"
                        else -> "utbetaling_sendt"
                    }
                }

                Status.Type.FERDIG -> {
                    "utbetaling_utført"
                }
            }
        return JsonMessage
            .newMessage(
                mapOf(
                    "@event_name" to "$eventnavn$suffiks",
                    "ident" to ident,
                    "behandlingId" to behandlingId,
                    "eksternBehandlingId" to behandlingId.tilBase64(),
                    "sakId" to sakId,
                    "eksternSakId" to sakId.tilBase64(),
                    "behandletHendelseId" to behandletHendelseId,
                    "behandletHendelseType" to behandletHendelseType,
                    "meldekortId" to behandletHendelseId,
                    "status" to status.type.name,
                ),
            ).toJson()
    }
}
