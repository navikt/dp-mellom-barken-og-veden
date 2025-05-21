package no.nav.dagpenger.mellom.barken.og.veden.domene

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class UtbetalingVedtak(
    val behandlingId: UUID,
    val basertPåBehandlingId: UUID?,
    val meldekortId: String,
    val ident: String,
    val behandletAv: String?,
    val utbetalinger: List<Utbetalingsdag>,
    var status: UtbetalingStatus,
    val opprettet: LocalDateTime,
)

data class Utbetalingsdag(
    val meldeperiode: String,
    val dato: LocalDate,
    val sats: Int,
    val utbetaltBeløp: Int,
)

enum class UtbetalingStatus {
    MOTTATT,
    SENDER_TIL_UTBETALING,
    SENDT,
    UTBETALT,
    FEIL,
}
