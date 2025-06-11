package no.nav.dagpenger.mellom.barken.og.veden.helved

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.mellom.barken.og.veden.repository.Repo
import no.nav.dagpenger.mellom.barken.og.veden.repository.vedtak
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.repository.UtbetalingRepo
import kotlin.test.Test

class StatusMottakTest {
    private val rapid = TestRapid()

    @Test
    fun `lese status meldinger fra helved`() {
        val utbetalingRepo =
            mockk<UtbetalingRepo>().also {
                every { it.hentVedtak(any()) } returns vedtak()
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

    @Test
    fun `hopper over statusmeldinger som ikke har klassekode som starter med DP`() {
        val utbetalingRepo =
            mockk<UtbetalingRepo>().also {
                every { it.hentVedtak(any()) } returns vedtak()
            }
        val repo =
            mockk<Repo>().also {
                every { it.lagreStatusFraHelved(any(), any(), any()) } returns Unit
            }
        val statusMottak = StatusMottak(rapid, utbetalingRepo, repo)

        rapid.sendTestMessage(
            meldingViIgnorer,
        )

        verify(exactly = 0) { utbetalingRepo.hentVedtak(any()) }
        verify(exactly = 0) { repo.lagreStatusFraHelved(any(), any(), any()) }
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
        """.trimIndent()

    //language=JSON
    private val meldingViIgnorer =
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
                  "klassekode": "HEIOGHOPP"
                }
              ]
            },
          "error": null
        }
        """.trimIndent()
}
