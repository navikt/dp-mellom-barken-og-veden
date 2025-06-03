package no.nav.dagpenger.mellom.barken.og.veden.repository

import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.nulls.shouldNotBeNull
import no.nav.dagpenger.mellom.barken.og.veden.PostgresConfiguration.dataSource
import no.nav.dagpenger.mellom.barken.og.veden.domene.Person
import no.nav.dagpenger.mellom.barken.og.veden.domene.UtbetalingStatus
import no.nav.dagpenger.mellom.barken.og.veden.domene.UtbetalingVedtak
import no.nav.dagpenger.mellom.barken.og.veden.domene.Utbetalingsdag
import no.nav.dagpenger.mellom.barken.og.veden.repository.Postgres.withMigratedDb
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class UtbetalingRepoTest {
    @Test
    fun `kan lagre og hente utbetalinger i basen via servicen`() {
        withMigratedDb {
            val repo = UtbetalingPostgresRepository(dataSource)
            val vedtak = vedtak()

            repo.lagreVedtak(vedtak)
            val lagretVedtak = repo.hentVedtak(vedtak.behandlingId)

            lagretVedtak.shouldNotBeNull()
            lagretVedtak shouldBeEqual vedtak
        }
    }
}

fun vedtak() =
    UtbetalingVedtak(
        behandlingId = UUID.randomUUID(),
        basertPåBehandlingId = null,
        vedtakstidspunkt = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS),
        meldekortId = "m1",
        sakId = "s1",
        ident = Person("12345678910"),
        saksbehandletAv = "saksbehandler",
        besluttetAv = "beslutter",
        utbetalinger =
            listOf(
                Utbetalingsdag(
                    meldeperiode = "m1",
                    dato = LocalDate.now(),
                    sats = 1000,
                    utbetaltBeløp = 1000,
                ),
            ),
        status = UtbetalingStatus.MOTTATT,
        opprettet = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS),
    )
