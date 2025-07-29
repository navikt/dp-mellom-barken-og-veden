package no.nav.dagpenger.mellom.barken.og.veden.utbetaling.repository

import kotliquery.TransactionalSession
import no.nav.dagpenger.mellom.barken.og.veden.helved.BehandlingId
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.Status
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.UtbetalingVedtak

interface UtbetalingRepo {
    fun lagreVedtak(vedtak: UtbetalingVedtak)

    fun hentAlleVedtakMedStatus(status: Status): List<UtbetalingVedtak>

    fun hentAlleMottatte(): List<UtbetalingVedtak>

    fun hentVedtak(behandlingId: BehandlingId): UtbetalingVedtak?

    fun oppdaterStatus(
        behandlingId: BehandlingId,
        status: Status,
    )

    fun oppdaterStatus(
        behandlingId: BehandlingId,
        status: Status,
        tx: TransactionalSession,
    )

    fun harUtbetalingerSomVenterPåSvar(sakId: String): Boolean

    fun hentAlleUtbetalingerForSak(sakId: String): List<UtbetalingVedtak>
}
