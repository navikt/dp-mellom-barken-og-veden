package no.nav.dagpenger.mellom.barken.og.veden.repository

import no.nav.dagpenger.mellom.barken.og.veden.domene.UtbetalingStatus
import no.nav.dagpenger.mellom.barken.og.veden.domene.UtbetalingVedtak
import java.util.UUID

interface UtbetalingRepo {
    fun lagreVedtak(vedtak: UtbetalingVedtak)

    fun hentNesteVedtakMedStatus(status: UtbetalingStatus): UtbetalingVedtak?

    fun hentVedtak(behandlingId: UUID): UtbetalingVedtak?

    fun oppdaterStatus(
        behandlingId: UUID,
        status: UtbetalingStatus,
    )
}
