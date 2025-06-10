package no.nav.dagpenger.mellom.barken.og.veden.helved

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.mellom.barken.og.veden.repository.Repo
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.Person
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.UtbetalingStatus
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.UtbetalingVedtak
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.Utbetalingsdag
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.repository.UtbetalingRepo
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test

class StatusMottakTest {
    private val rapid = TestRapid()

    @Test
    fun `lese status meldinger fra helved`() {
        val utbetalingRepo =
            mockk<UtbetalingRepo>().also {
                every { it.hentVedtak(any()) } returns
                    UtbetalingVedtak(
                        behandlingId = UUID.randomUUID(),
                        basertPåBehandlingId = UUID.randomUUID(),
                        vedtakstidspunkt = LocalDateTime.now(),
                        meldekortId = "meldekort1",
                        sakId = "sakId",
                        ident = Person("12345678901"),
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
                        status = UtbetalingStatus.SENDT,
                        opprettet = LocalDateTime.now(),
                    )
            }
        val repo =
            mockk<Repo>().also {
                every { it.lagreStatusFraHelved(any(), any(), any()) } returns Unit
            }
        val statusMottak = StatusMottak(rapid, utbetalingRepo, repo)

        rapid.sendTestMessage(
            statusMelding,
        )

        verify(exactly = 1) { utbetalingRepo.hentVedtak(any()) }
        verify(exactly = 1) { repo.lagreStatusFraHelved(any(), any(), any()) }
    }

    //language=JSON
    private val statusMelding =
        """
               {
          "status": "OK",
          "detaljer": {
              "linjer": [
                {
                  "behandlingId": "AZdZCcAJcESkSYskqsOKKw==",
                  "fom": "2025-05-22",
                  "tom": "2025-05-23",
                  "vedtakssats": 467,
                  "beløp": 469,
                  "klassekode": "DPORAS"
                },
                {
                  "behandlingId": "AZdZCcAJcESkSYskqsOKKw==",
                  "fom": "2025-05-26",
                  "tom": "2025-06-06",
                  "vedtakssats": 467,
                  "beløp": 324,
                  "klassekode": "DPORAS"
                }
              ]
            },
          "error": null
        }
                }
        """.trimIndent()
}
