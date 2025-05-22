package no.nav.dagpenger.mellom.barken.og.veden.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.dagpenger.mellom.barken.og.veden.domene.UtbetalingStatus
import no.nav.dagpenger.mellom.barken.og.veden.domene.UtbetalingVedtak
import no.nav.dagpenger.mellom.barken.og.veden.domene.Utbetalingsdag
import no.nav.dagpenger.mellom.barken.og.veden.repository.UtbetalingRepo
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class MeldingOmUtbetalingVedtakMottakTest {
    private val repo = mockk<UtbetalingRepo>(relaxed = true)

    private val rapid =
        TestRapid().apply {
            MeldingOmUtbetalingVedtakMottak(
                rapidsConnection = this,
                repo = repo,
            )
        }

    @Test
    fun `mottar melding om utbetaling vedtak`() {
        val json = javaClass.getResource("/test-data/Vedtak_fattet_innvilget.json")!!.readText()

        val capturedVedtak = slot<UtbetalingVedtak>()
        every { repo.lagreVedtak(capture(capturedVedtak)) } returns Unit

        rapid.sendTestMessage(json)

        capturedVedtak.captured.behandlingId shouldBe UUID.fromString("0196d806-87f2-73dd-84a3-afb2dcd7f05e")
        capturedVedtak.captured.basertPåBehandlingId shouldBe UUID.fromString("0196d806-86aa-74f1-be56-00c952d40eb3")
        capturedVedtak.captured.meldekortId shouldBe "5"
        capturedVedtak.captured.ident shouldBe "11109233444"
        capturedVedtak.captured.saksbehandletAv shouldBe "dp-behandling"
        capturedVedtak.captured.utbetalinger.minBy { it.dato } shouldBe
            Utbetalingsdag(
                meldeperiode = "132460781",
                dato = LocalDate.of(2021, 6, 7),
                sats = 1077,
                utbetaltBeløp = 553,
            )
        capturedVedtak.captured.utbetalinger.maxBy { it.dato } shouldBe
            Utbetalingsdag(
                meldeperiode = "132463246",
                dato = LocalDate.of(2021, 8, 1),
                sats = 1077,
                utbetaltBeløp = 0,
            )
        capturedVedtak.captured.utbetalinger.size shouldBe 56
        capturedVedtak.captured.status shouldBe UtbetalingStatus.MOTTATT
        capturedVedtak.captured.opprettet shouldBe LocalDateTime.of(2025, 5, 16, 9, 37, 17, 336661000)
    }
}
