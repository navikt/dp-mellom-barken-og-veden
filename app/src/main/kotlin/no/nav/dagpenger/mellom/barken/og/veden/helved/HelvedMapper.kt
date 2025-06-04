package no.nav.dagpenger.mellom.barken.og.veden.helved

import no.nav.dagpenger.mellom.barken.og.veden.objectMapper
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.UtbetalingVedtak
import no.nav.helved.kontrakt.api.models.UtbetalingDTO
import no.nav.helved.kontrakt.api.models.UtbetalingsdagDTO
import no.nav.helved.kontrakt.api.models.UtbetalingsdagDTORettighetstypeDTO.ORDINÆR
import no.nav.helved.kontrakt.api.models.UtbetalingsdagDTOUtbetalingstypeDTO.DAGPENGER

fun UtbetalingVedtak.mapToVedtakDTO(): UtbetalingDTO =
    UtbetalingDTO(
        behandlingId = UtbetalingId(behandlingId).toString(),
        vedtakstidspunktet = vedtakstidspunkt,
        sakId = sakId,
        ident = ident.ident,
        utbetalinger =
            utbetalinger.map { dag ->
                UtbetalingsdagDTO(
                    meldeperiode = dag.meldeperiode,
                    dato = dag.dato,
                    sats = dag.sats,
                    utbetaltBeløp = dag.utbetaltBeløp,
                    rettighetstype = ORDINÆR,
                    utbetalingstype = DAGPENGER,
                )
            },
    )

fun UtbetalingDTO.toJson(): String = objectMapper.writeValueAsString(this)
