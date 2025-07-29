package no.nav.dagpenger.mellom.barken.og.veden.repository

import io.kotest.matchers.equals.shouldBeEqual
import io.kotest.matchers.nulls.shouldNotBeNull
import no.nav.dagpenger.mellom.barken.og.veden.PostgresConfiguration.dataSource
import no.nav.dagpenger.mellom.barken.og.veden.helved.BehandlingId
import no.nav.dagpenger.mellom.barken.og.veden.repository.Postgres.withMigratedDb
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.Person
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.Status
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.UtbetalingVedtak
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.Utbetalingsdag
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.repository.UtbetalingPostgresRepository
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

fun vedtak(
    behandlingId: UUID = UUID.randomUUID(),
    basertPåBehandlingId: UUID? = null,
    vedtakstidspunkt: LocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS),
    meldekortId: String = "m1",
    sakId: String = "s1",
    ident: Person = Person("12345678910"),
    saksbehandletAv: String = "saksbehandler",
    besluttetAv: String = "beslutter",
    utbetalinger: List<Utbetalingsdag> =
        listOf(
            Utbetalingsdag(
                meldeperiode = "m1",
                dato = LocalDate.now(),
                sats = 1000,
                utbetaltBeløp = 1000,
            ),
        ),
    status: Status = Status.Mottatt,
    opprettet: LocalDateTime = LocalDateTime.now().truncatedTo(ChronoUnit.MILLIS),
) = UtbetalingVedtak(
    behandlingId = BehandlingId(behandlingId),
    basertPåBehandlingId = basertPåBehandlingId?.let { BehandlingId(it) },
    vedtakstidspunkt = vedtakstidspunkt,
    meldekortId = meldekortId,
    sakId = sakId,
    ident = ident,
    saksbehandletAv = saksbehandletAv,
    besluttetAv = besluttetAv,
    utbetalinger = utbetalinger,
    status = status,
    opprettet = opprettet,
)
