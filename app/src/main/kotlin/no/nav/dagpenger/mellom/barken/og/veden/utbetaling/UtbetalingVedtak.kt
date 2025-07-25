package no.nav.dagpenger.mellom.barken.og.veden.utbetaling

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class UtbetalingVedtak(
    val behandlingId: UUID,
    val basertPåBehandlingId: UUID?,
    val vedtakstidspunkt: LocalDateTime,
    val meldekortId: String,
    val sakId: String,
    val ident: Person,
    val saksbehandletAv: String,
    val besluttetAv: String,
    val utbetalinger: List<Utbetalingsdag>,
    var status: Status,
    val opprettet: LocalDateTime,
)

data class Utbetalingsdag(
    val meldeperiode: String,
    val dato: LocalDate,
    val sats: Int,
    val utbetaltBeløp: Int,
)

sealed class Status {
    object Mottatt : Status()

    data class TilUtbetaling(
        // skal vi bare ta med staus her eller hele statusReply?
        val eksternStatus: UtbetalingStatus,
    ) : Status()

    object Ferdig : Status()

    enum class UtbetalingStatus {
        SENDT,
        FEILET,
        MOTTATT,
        HOS_OPPDRAG,
    }
}
