package no.nav.dagpenger.mellom.barken.og.veden.service

import no.nav.dagpenger.mellom.barken.og.veden.domene.UtbetalingVedtak

interface UtbetalingService {
    fun mottaUtbetalingVedtak(vedtak: UtbetalingVedtak)

    fun behandleUtbetalingVedtak()
}
