package no.nav.dagpenger.mellom.barken.og.veden.repository

import no.nav.dagpenger.mellom.barken.og.veden.domene.UtbetalingVedtak

interface UtbetalingRepo {
    fun lagreVedtak(vedtak: UtbetalingVedtak)
}
