package no.nav.dagpenger.mellom.barken.og.veden.utbetaling.repository

import kotliquery.TransactionalSession
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.Status
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.UtbetalingVedtak
import java.time.LocalDate
import java.time.Year
import java.util.UUID

interface UtbetalingRepo {
    fun lagreVedtak(vedtak: UtbetalingVedtak)

    fun hentAlleVedtakMedStatus(status: Status.Type): List<UtbetalingVedtak>

    fun hentAlleMottatte(): List<UtbetalingVedtak>

    fun hentAlleIkkeFerdige(): List<UtbetalingVedtak>

    fun hentVedtak(behandlingId: UUID): UtbetalingVedtak?

    fun oppdaterStatus(
        behandlingId: UUID,
        status: Status,
    )

    fun oppdaterStatus(
        behandlingId: UUID,
        status: Status,
        tx: TransactionalSession,
    )

    fun harUtbetalingerSomVenterPåSvar(sakId: UUID): Boolean

    fun hentAlleUtbetalingerForSak(sakId: UUID): List<UtbetalingVedtak>

    fun hentAlleFerdigeUtenFerietilleggForIdent(
        ident: String,
        opptjeningsår: Year,
    ): List<UtbetalingVedtak>

    fun hentAlleFerdige(
        fom: LocalDate,
        tom: LocalDate,
    ): List<UtbetalingVedtak>

    fun lagreMelding(
        behandlingId: UUID,
        type: String,
        json: String,
    )

    fun lagreMelding(
        behandlingId: UUID,
        type: String,
        json: String,
        tx: TransactionalSession,
    )
}
