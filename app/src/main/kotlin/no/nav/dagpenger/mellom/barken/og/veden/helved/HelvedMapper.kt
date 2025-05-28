package no.nav.dagpenger.mellom.barken.og.veden.helved

import no.nav.dagpenger.mellom.barken.og.veden.domene.UtbetalingVedtak
import no.nav.dagpenger.mellom.barken.og.veden.objectMapper
import no.nav.helved.kontrakt.api.models.UtbetalingDTO
import no.nav.helved.kontrakt.api.models.UtbetalingsdagDTO

fun UtbetalingVedtak.mapToVedtakDTO(): UtbetalingDTO =
    UtbetalingDTO(
        behandlingId = UtbetalingId(behandlingId).toString(),
        vedtakstidspunktet = vedtakstidspunkt,
        sakId = sakId,
        ident = ident,
        utbetalinger =
            utbetalinger.map { dag ->
                UtbetalingsdagDTO(
                    meldeperiode = dag.meldeperiode,
                    dato = dag.dato,
                    sats = dag.sats,
                    utbetaltBeløp = dag.utbetaltBeløp,
                )
            },
    )

fun UtbetalingDTO.toJson(): String = objectMapper.writeValueAsString(this)
