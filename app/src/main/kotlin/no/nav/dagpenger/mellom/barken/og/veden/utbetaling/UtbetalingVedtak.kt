package no.nav.dagpenger.mellom.barken.og.veden.utbetaling

import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

data class UtbetalingVedtak(
    val behandlingId: UUID,
    val basertPåBehandlingId: UUID?,
    val vedtakstidspunkt: LocalDateTime,
    val meldekortId: String,
    val sakId: UUID,
    val person: Person,
    val saksbehandletAv: String,
    val besluttetAv: String,
    val utbetalinger: List<Utbetalingsdag>,
    val status: Status,
    val opprettet: LocalDateTime,
)

data class Utbetalingsdag(
    val meldeperiode: String,
    val dato: LocalDate,
    val sats: Int,
    val utbetaltBeløp: Int,
)

sealed class Status {
    abstract val opprettet: LocalDateTime
    abstract val type: Type

    enum class Type {
        MOTTATT,
        TIL_UTBETALING,
        FERDIG,
    }

    data class Mottatt(
        override val opprettet: LocalDateTime = LocalDateTime.now(),
    ) : Status() {
        override val type: Type = Type.MOTTATT
    }

    data class TilUtbetaling(
        val eksternStatus: UtbetalingStatus,
        override val opprettet: LocalDateTime = LocalDateTime.now(),
    ) : Status() {
        override val type: Type = Type.TIL_UTBETALING
    }

    data class Ferdig(
        override val opprettet: LocalDateTime = LocalDateTime.now(),
    ) : Status() {
        override val type: Type = Type.FERDIG
    }

    enum class UtbetalingStatus {
        SENDT,
        FEILET,
        MOTTATT,
        HOS_OPPDRAG,
    }
}
