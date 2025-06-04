package no.nav.dagpenger.mellom.barken.og.veden.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.MeldingOmUtbetalingVedtakMottak
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.UtbetalingStatus
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.UtbetalingVedtak
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.Utbetalingsdag
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.repository.UtbetalingRepo
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

        val vedtak = capturedVedtak.captured
        with(vedtak) {
            behandlingId shouldBe UUID.fromString("76755FC9-592A-46A1-88C9-1B9AE622E5F2")
            basertPåBehandlingId shouldBe UUID.fromString("A421B97A-EDC8-4DB7-BCC0-3F87B2DBDB1D")
            vedtakstidspunkt shouldBe LocalDateTime.parse("2025-05-16T09:37:17.336661")
            meldekortId shouldBe "5"
            ident.ident shouldBe "11109233444"
            saksbehandletAv shouldBe "dp-behandling"
            utbetalinger.minBy { it.dato } shouldBe
                Utbetalingsdag(
                    meldeperiode = "132460781",
                    dato = LocalDate.of(2021, 6, 7),
                    sats = 1077,
                    utbetaltBeløp = 553,
                )
            utbetalinger.maxBy { it.dato } shouldBe
                Utbetalingsdag(
                    meldeperiode = "132463246",
                    dato = LocalDate.of(2021, 8, 1),
                    sats = 1077,
                    utbetaltBeløp = 0,
                )
            utbetalinger.size shouldBe 56
            status shouldBe UtbetalingStatus.MOTTATT
            opprettet shouldBe LocalDateTime.parse("2025-05-16T09:37:17.338979")
        }
    }
}
