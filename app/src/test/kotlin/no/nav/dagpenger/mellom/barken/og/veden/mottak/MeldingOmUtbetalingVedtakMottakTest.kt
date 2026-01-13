package no.nav.dagpenger.mellom.barken.og.veden.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.MeldingOmUtbetalingVedtakMottak
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.Opprinnelse
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.Status
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.UtbetalingVedtak
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.Utbetalingsdag
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.repository.UtbetalingRepo
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.jvm.javaClass

class MeldingOmUtbetalingVedtakMottakTest {
    private val repo = mockk<UtbetalingRepo>(relaxed = true)
    private val testSakId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000")
    private val rapid =
        TestRapid().apply {
            MeldingOmUtbetalingVedtakMottak(
                rapidsConnection = this,
                repo = repo,
                sakIdHenter =
                    mockk {
                        coEvery { hentSakId(any()) } returns testSakId
                    },
            )
        }

    @Test
    fun `mottar melding om utbetaling vedtak`() {
        val json = javaClass.getResource("/test-data/behandlingresultatMedUtbetalinger.json")!!.readText()

        val capturedVedtak = slot<UtbetalingVedtak>()
        every { repo.lagreVedtak(capture(capturedVedtak)) } returns Unit

        rapid.sendTestMessage(json)

        val vedtak = capturedVedtak.captured
        with(vedtak) {
            behandlingId shouldBe UUID.fromString("019a9b85-c056-7252-ba57-bc318ff3bfd6")
            basertPåBehandlingId shouldBe UUID.fromString("019a9b85-bf25-72ee-8fd7-1d8f6ab74ce2")
            vedtakstidspunkt shouldBe LocalDateTime.parse("2025-11-19T10:50:25.403304")
            behandletHendelseId shouldBe "019a9b85-bee3-7b3e-a122-7433aa5542bd"
            person.ident shouldBe "13216349431"
            sakId shouldBe testSakId
            saksbehandletAv shouldBe "NAV123123"
            utbetalinger.minBy { it.dato } shouldBe
                Utbetalingsdag(
                    meldeperiode = "132264559",
                    dato = LocalDate.of(2018, 6, 21),
                    sats = 1259,
                    utbetaltBeløp = 719,
                    opprinnelse = Opprinnelse.Ukjent,
                )
            utbetalinger.maxBy { it.dato } shouldBe
                Utbetalingsdag(
                    meldeperiode = "132266061",
                    dato = LocalDate.of(2018, 7, 15),
                    sats = 1259,
                    utbetaltBeløp = 0,
                    opprinnelse = Opprinnelse.Ukjent,
                )
            utbetalinger.size shouldBe 25
            status.type shouldBe Status.Type.MOTTATT
            opprettet shouldBe LocalDateTime.parse("2025-11-19T10:50:25.403304")
        }
        with(rapid.inspektør) {
            size shouldBe 1
            message(0)["@event_name"].asText() shouldBe "utbetaling_mottatt"
        }
    }

    @Test
    fun `mottar melding uten utbetalinger skippes`() {
        val json = javaClass.getResource("/test-data/behandlingresultatUtenUtbetalinger.json")!!.readText()

        val capturedVedtak = slot<UtbetalingVedtak>()
        every { repo.lagreVedtak(capture(capturedVedtak)) } returns Unit

        rapid.sendTestMessage(json)

        verify(exactly = 0) { repo.lagreVedtak(any()) }
        capturedVedtak.isCaptured shouldBe false

        with(rapid.inspektør) {
            size shouldBe 0
        }
    }

    @Test
    fun `kan ta imot resultat fra manuell behandling`() {
        val json = javaClass.getResource("/test-data/behandlingresultatManuellMedUtbetalinger.json")!!.readText()

        val capturedVedtak = slot<UtbetalingVedtak>()
        every { repo.lagreVedtak(capture(capturedVedtak)) } returns Unit

        rapid.sendTestMessage(json)

        val vedtak = capturedVedtak.captured
        with(vedtak) {
            behandlingId shouldBe UUID.fromString("019a9b85-c056-7252-ba57-bc318ff3bfd6")
            basertPåBehandlingId shouldBe UUID.fromString("019a9b85-bf25-72ee-8fd7-1d8f6ab74ce2")
            vedtakstidspunkt shouldBe LocalDateTime.parse("2025-11-19T10:50:25.403304")
            behandletHendelseId shouldBe "019a9c58-7390-703e-a7af-97ed475d9546"
            person.ident shouldBe "13216349431"
            sakId shouldBe testSakId
            saksbehandletAv shouldBe "NAV123123"
            utbetalinger.minBy { it.dato } shouldBe
                Utbetalingsdag(
                    meldeperiode = "132264559",
                    dato = LocalDate.of(2018, 6, 21),
                    sats = 1259,
                    utbetaltBeløp = 719,
                    opprinnelse = Opprinnelse.Ukjent,
                )
            utbetalinger.maxBy { it.dato } shouldBe
                Utbetalingsdag(
                    meldeperiode = "132266061",
                    dato = LocalDate.of(2018, 7, 15),
                    sats = 1259,
                    utbetaltBeløp = 0,
                    opprinnelse = Opprinnelse.Ukjent,
                )
            utbetalinger.size shouldBe 25
            status.type shouldBe Status.Type.MOTTATT
            opprettet shouldBe LocalDateTime.parse("2025-11-19T10:50:25.403304")
        }
        with(rapid.inspektør) {
            size shouldBe 1
            message(0)["@event_name"].asText() shouldBe "utbetaling_mottatt"
        }
    }
}
