package no.nav.dagpenger.mellom.barken.og.veden.service

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.mellom.barken.og.veden.PostgresConfiguration.dataSource
import no.nav.dagpenger.mellom.barken.og.veden.domene.UtbetalingStatus
import no.nav.dagpenger.mellom.barken.og.veden.domene.UtbetalingVedtak
import no.nav.dagpenger.mellom.barken.og.veden.domene.Utbetalingsdag
import no.nav.dagpenger.mellom.barken.og.veden.repository.Postgres.withMigratedDb
import no.nav.dagpenger.mellom.barken.og.veden.repository.UtbetalingPostgresRepository
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class UtbetalingServiceImplTest {
    @Test
    fun `kan lagre og hente utbetalinger i basen via servicen`() {
        withMigratedDb {
            val repo = UtbetalingPostgresRepository(dataSource)
            val service = UtbetalingServiceImpl(repo)
            val vedtak = vedtak()

            service.mottaUtbetalingVedtak(vedtak)
            val lagretVedtak = repo.hentVedtak(vedtak.behandlingId)

            lagretVedtak shouldBe vedtak
        }
    }

    @Test
    fun `kan hente og oppdatere utbetalinger som ligger klare`() {
        withMigratedDb {
            val repo = UtbetalingPostgresRepository(dataSource)
            val service = UtbetalingServiceImpl(repo)
            val mottattVedtak1 = vedtak()
            val mottattVedtak2 = vedtak()

            repo.lagreVedtak(mottattVedtak1)
            repo.lagreVedtak(mottattVedtak2)

            service.behandleUtbetalingVedtak()
            // her vil jeg mocke kafka og sjekke at det sendes til kafka

            repo.hentNesteVedtakMedStatus(UtbetalingStatus.MOTTATT) shouldBe null

            repo.hentVedtak(mottattVedtak1.behandlingId)?.status shouldBe UtbetalingStatus.SENDT
            repo.hentVedtak(mottattVedtak2.behandlingId)?.status shouldBe UtbetalingStatus.SENDT
        }
    }
}

private fun vedtak() =
    UtbetalingVedtak(
        behandlingId = UUID.randomUUID(),
        meldekortId = "m1",
        ident = "123",
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
        basertPåBehandlingId = null,
        behandletAv = "saksbehandler",
        opprettet = LocalDateTime.now(),
    )
