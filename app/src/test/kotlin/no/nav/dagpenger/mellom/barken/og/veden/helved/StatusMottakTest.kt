package no.nav.dagpenger.mellom.barken.og.veden.helved

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.mellom.barken.og.veden.repository.vedtak
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.helved.StatusMottak
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.helved.repository.Repo
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.repository.UtbetalingRepo
import java.util.UUID
import kotlin.test.Test

class StatusMottakTest {
    private val rapid = TestRapid()

    @Test
    fun `lese status meldinger fra helved`() {
        val behandlingId = UUID.randomUUID()
        val sakId = UUID.randomUUID()
        val utbetalingRepo =
            mockk<UtbetalingRepo>().also {
                every { it.hentVedtak(behandlingId) } returns vedtak(sakId = sakId, behandlingId = behandlingId)
            }
        val repo =
            mockk<Repo>().also {
                every { it.lagreStatusFraHelved(any(), any(), any()) } returns Unit
            }
        val statusMottak = StatusMottak(rapid, utbetalingRepo, repo)

        rapid.sendTestMessage(
            statusMelding,
            behandlingId.toString(),
        )

        verify(exactly = 1) { utbetalingRepo.hentVedtak(behandlingId) }
        verify(exactly = 1) { repo.lagreStatusFraHelved(any(), any(), any()) }

        with(rapid.inspektør) {
            size shouldBe 1
            key(0) shouldBe "12345678910"
            val hendelse = message(0)
            hendelse["@event_name"].asText() shouldBe "utbetaling_utført"
            hendelse["behandlingId"].asText() shouldBe behandlingId.toString()
            hendelse["sakId"].asText() shouldBe sakId.toString()
        }
    }

    @Test
    fun `hopper over statusmeldinger som ikke har klassekode som starter med DP`() {
        val behandlingId = UUID.randomUUID()
        val utbetalingRepo =
            mockk<UtbetalingRepo>().also {
                every { it.hentVedtak(any()) } returns null
            }
        val repo =
            mockk<Repo>().also {
                every { it.lagreStatusFraHelved(any(), any(), any()) } returns Unit
            }
        val statusMottak = StatusMottak(rapid, utbetalingRepo, mockk())

        rapid.sendTestMessage(
            meldingViIgnorer,
            behandlingId.toString(),
        )

        verify(exactly = 1) { utbetalingRepo.hentVedtak(behandlingId) }
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
