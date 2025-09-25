package no.nav.dagpenger.mellom.barken.og.veden.utbetaling.helved

import java.time.LocalDate

data class StatusReply(
    val status: Status,
    val error: ApiError? = null,
    val detaljer: Detaljer? = null,
) {
    enum class Status {
        OK, // er utbetalt
        FEILET, // noe gikk galt, se error
        MOTTATT, // er mottatt helved men ikke sendt til oppdrag enda
        HOS_OPPDRAG, // er sendt til oppdrag men ikke utbetalt enda
    }
}

data class Detaljer(
    val linjer: List<DetaljerLinje>,
)

data class DetaljerLinje(
    val behandlingId: String?,
    val fom: LocalDate,
    val tom: LocalDate,
    val beløp: UInt,
    val vedtakssats: UInt?,
    val klassekode: String,
)

data class ApiError(
    val behandlingId: String?,
    val statusCode: Int,
    val msg: String,
    val doc: String,
)
