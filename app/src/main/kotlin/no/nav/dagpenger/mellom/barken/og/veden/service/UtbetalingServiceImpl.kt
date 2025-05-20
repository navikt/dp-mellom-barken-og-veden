package no.nav.dagpenger.mellom.barken.og.veden.service

import no.nav.dagpenger.mellom.barken.og.veden.domene.UtbetalingVedtak
import no.nav.dagpenger.mellom.barken.og.veden.repository.UtbetalingRepo

class UtbetalingServiceImpl(
    val repo: UtbetalingRepo,
) : UtbetalingService {
    override fun mottaUtbetalingVedtak(vedtak: UtbetalingVedtak) {
        repo.lagreVedtak(vedtak)
    }
}
