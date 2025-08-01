package no.nav.dagpenger.mellom.barken.og.veden.helved

import io.kotest.assertions.json.shouldEqualJson
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.Person
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.Status
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.UtbetalingVedtak
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.Utbetalingsdag
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test

class HelvedMapperTest {
    @Test
    fun `vi kan mappe utbetaling til json`() {
        val behandlingId = BehandlingId(UUID.randomUUID())
        val utbetalingId = behandlingId.tilBase64()
        val vedtakstidspunkt = LocalDateTime.now()
        val utbetaling =
            UtbetalingVedtak(
                behandlingId = behandlingId,
                basertPåBehandlingId = BehandlingId(UUID.randomUUID()),
                vedtakstidspunkt = vedtakstidspunkt,
                meldekortId = "meldekort1",
                sakId = "sakId",
                person = Person("12345678901"),
                saksbehandletAv = "saksbehandler",
                besluttetAv = "beslutter",
                utbetalinger =
                    listOf(
                        Utbetalingsdag(
                            meldeperiode = "1234567",
                            dato = LocalDate.of(2025, 5, 23),
                            sats = 1000,
                            utbetaltBeløp = 1000,
                        ),
                    ),
                status = Status.Mottatt,
                opprettet = LocalDateTime.now(),
            )

        utbetaling.mapToVedtakDTO().toJson() shouldEqualJson
            """
            {
              "sakId": "sakId",
              "behandlingId": "$utbetalingId",
              "vedtakstidspunktet": "$vedtakstidspunkt",
              "ident": "12345678901",
              "utbetalinger": [
                {
                  "meldeperiode": "1234567",
                  "dato": "2025-05-23",
                  "sats": 1000,
                  "utbetaltBeløp": 1000,
                  "utbetalingstype": "Dagpenger",
                  "rettighetstype": "Ordinær"

                }
              ]
            }
            """.trimIndent()
    }
}
