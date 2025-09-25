package no.nav.dagpenger.mellom.barken.og.veden.utbetaling

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import java.util.UUID

data class UtbetalingStatusHendelse(
    val behandlingId: UUID,
    val sakId: UUID,
    val meldekortId: String,
    val status: Status,
) {
    fun tilHendelse() =
        JsonMessage
            .newMessage(
                mapOf(
                    "@event_name" to
                        when (status.type) {
                            Status.Type.MOTTATT -> "utbetaling_mottatt"
                            Status.Type.TIL_UTBETALING -> {
                                when ((status as Status.TilUtbetaling).eksternStatus) {
                                    Status.UtbetalingStatus.FEILET -> "utbetaling_feilet"
                                    else -> "utbetaling_sendt"
                                }
                            }
                            Status.Type.FERDIG -> "utbetaling_utført"
                        },
                    "behandlingId" to behandlingId,
                    "sakId" to sakId,
                    "meldekortId" to meldekortId,
                    "status" to status.type.name,
                ),
            ).toJson()
}
