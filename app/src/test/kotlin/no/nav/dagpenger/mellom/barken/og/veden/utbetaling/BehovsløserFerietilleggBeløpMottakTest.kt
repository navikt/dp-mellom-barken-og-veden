package no.nav.dagpenger.mellom.barken.og.veden.utbetaling

import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.Status.UtbetalingStatus
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.repository.UtbetalingRepo
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.Year
import java.util.UUID

class BehovsløserFerietilleggBeløpMottakTest {
    private val repo = mockk<UtbetalingRepo>(relaxed = true)
    private val rapid =
        TestRapid().apply {
            BehovsløserFerietilleggBeløpMottak(
                rapidsConnection = this,
                repo = repo,
            )
        }

    @Test
    fun `vi kan finne og legge sammen beløp`() {
        val ident = "13216349431"
        val opptjeningsår = Year.of(2025)
        val json =
            """
            {
                "@event_name": "behov",
                "@behovId": "123e4567-e89b-12d3-a456-426614174000",
                "@behov": ["OpptjeningsBeløp"],
                "ident": "$ident",
                "OpptjeningsBeløp": {
                   "OpptjeningsårFerietillegg": 2025
                }
            }
            """.trimIndent()

        every { repo.hentAlleFerdigeUtenFerietilleggForIdent(ident, opptjeningsår) } returns
            listOf(
                nyttVedtak(
                    vedtakstidspunkt = LocalDate.of(2025, 1, 1),
                    utbetDager =
                        listOf(
                            utbetalingsdag(
                                dag = LocalDate.of(2025, 6, 21),
                                beløp = 100,
                            ),
                            utbetalingsdag(
                                dag = LocalDate.of(2025, 6, 22),
                                beløp = 200,
                            ),
                            // Denne er ikke i riktig år og skal ikke bli med
                            utbetalingsdag(
                                dag = LocalDate.of(2026, 1, 1),
                                beløp = 200,
                            ),
                        ),
                ),
            )

        rapid.sendTestMessage(json)

        with(rapid.inspektør) {
            size shouldBe 1
            message(0)["@løsning"]["OpptjeningsBeløp"]["verdi"].asInt() shouldBe 300
            message(0)["@løsning"]["OpptjeningsBeløp"]["gyldigFraOgMed"].asLocalDate() shouldBe LocalDate.of(2025, 1, 1)
        }
    }

    @Test
    fun `det går fint selv om vi ikke finner noe`() {
        val ident = "13216349431"
        val opptjeningsår = Year.of(2025)
        val json =
            """
            {
                "@event_name": "behov",
                "@behovId": "123e4567-e89b-12d3-a456-426614174000",
                "@behov": ["OpptjeningsBeløp"],
                "ident": "$ident",
                "OpptjeningsBeløp": {
                   "OpptjeningsårFerietillegg": 2025
                }
            }
            """.trimIndent()

        every { repo.hentAlleFerdigeUtenFerietilleggForIdent(ident, opptjeningsår) } returns emptyList()

        rapid.sendTestMessage(json)

        with(rapid.inspektør) {
            size shouldBe 1
            message(0)["@løsning"]["OpptjeningsBeløp"]["verdi"].asInt() shouldBe 0
            message(0)["@løsning"]["OpptjeningsBeløp"]["gyldigFraOgMed"].asLocalDate() shouldBe LocalDate.of(2025, 1, 1)
        }
    }

    @Test
    fun `vi klarer å finne dager fra flere saker`() {
        val ident = "13216349431"
        val opptjeningsår = Year.of(2025)
        val json =
            """
            {
                "@event_name": "behov",
                "@behovId": "123e4567-e89b-12d3-a456-426614174000",
                "@behov": ["OpptjeningsBeløp"],
                "ident": "$ident",
                "OpptjeningsBeløp": {
                   "OpptjeningsårFerietillegg": 2025
                }
            }
            """.trimIndent()

        every { repo.hentAlleFerdigeUtenFerietilleggForIdent(ident, opptjeningsår) } returns
            listOf(
                nyttVedtak(
                    vedtakstidspunkt = LocalDate.of(2025, 1, 1),
                    utbetDager =
                        listOf(
                            utbetalingsdag(
                                dag = LocalDate.of(2025, 1, 21),
                                beløp = 100,
                            ),
                            utbetalingsdag(
                                dag = LocalDate.of(2025, 1, 22),
                                beløp = 200,
                            ),
                        ),
                ),
                nyttVedtak(
                    vedtakstidspunkt = LocalDate.of(2025, 12, 1),
                    utbetDager =
                        listOf(
                            utbetalingsdag(
                                dag = LocalDate.of(2025, 12, 21),
                                beløp = 100,
                            ),
                            utbetalingsdag(
                                dag = LocalDate.of(2025, 12, 22),
                                beløp = 200,
                            ),
                        ),
                ),
            )

        rapid.sendTestMessage(json)

        with(rapid.inspektør) {
            size shouldBe 1
            message(0)["@løsning"]["OpptjeningsBeløp"]["verdi"].asInt() shouldBe 600
            message(0)["@løsning"]["OpptjeningsBeløp"]["gyldigFraOgMed"].asLocalDate() shouldBe LocalDate.of(2025, 1, 1)
        }
    }

    private fun utbetalingsdag(
        dag: LocalDate = LocalDate.of(2025, 6, 21),
        beløp: Int = 100,
    ) = Utbetalingsdag(
        meldeperiode = "132264559",
        dato = dag,
        sats = beløp,
        utbetaltBeløp = beløp,
        opprinnelse = Opprinnelse.Ny,
        dagpengeType = DagpengeType.FERIETILLEGG,
    )

    private fun nyttVedtak(
        sakId: UUID = UUID.randomUUID(),
        vedtakstidspunkt: LocalDate = LocalDate.of(2025, 1, 1),
        utbetDager: List<Utbetalingsdag> =
            listOf(
                utbetalingsdag(),
            ),
    ) = UtbetalingVedtak(
        sakId = sakId,
        behandlingId = UUID.randomUUID(),
        vedtakstidspunkt = vedtakstidspunkt.atTime(10, 50, 25),
        behandletHendelseId = "hendelseId",
        behandletHendelseType = "MELDEKORT",
        utbetalinger = utbetDager,
        basertPåBehandlingId = null,
        person = Person("12345678901"),
        saksbehandletAv = "saksbehandler",
        besluttetAv = "beslutter",
        status = Status.Ferdig(UtbetalingStatus.OK),
        opprettet = vedtakstidspunkt.atTime(10, 50, 25),
    )
}
