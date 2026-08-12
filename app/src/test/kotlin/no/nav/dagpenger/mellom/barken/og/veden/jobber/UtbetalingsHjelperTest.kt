package no.nav.dagpenger.mellom.barken.og.veden.jobber

import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.dagpenger.mellom.barken.og.veden.PostgresConfiguration.dataSource
import no.nav.dagpenger.mellom.barken.og.veden.TestRapid
import no.nav.dagpenger.mellom.barken.og.veden.repository.Postgres.withMigratedDb
import no.nav.dagpenger.mellom.barken.og.veden.repository.vedtak
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.Status
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.helved.HelvedUtsender
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.jobber.UtsendingsHjelper
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.repository.UtbetalingPostgresRepository
import no.nav.dagpenger.mellom.barken.og.veden.varsling.GjentagendeVarsling
import org.apache.kafka.clients.producer.MockProducer
import org.apache.kafka.clients.producer.RoundRobinPartitioner
import org.apache.kafka.common.serialization.StringSerializer
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource

class UtbetalingsHjelperTest {
    @Test
    fun `kan hente og oppdatere utbetalinger som ligger klare`() {
        withMigratedDb {
            val repo = UtbetalingPostgresRepository(dataSource)
            val topic = "utbetaling.topic"
            val producer = MockProducer(true, RoundRobinPartitioner(), StringSerializer(), StringSerializer())
            val helvedUtsender = HelvedUtsender(topic, producer)
            val hjelper = UtsendingsHjelper(repo, helvedUtsender, TestRapid())
            val sak1 = UUID.randomUUID()
            val sak2 = UUID.randomUUID()

            val mottattVedtak1 = vedtak(sakId = sak1)
            val mottattVedtak2 = vedtak(sakId = sak2)

            repo.lagreVedtak(mottattVedtak1)
            repo.lagreVedtak(mottattVedtak2)

            hjelper.behandleUtbetalingVedtak()

            producer.history().shouldNotBeEmpty()
            producer.history().firstOrNull()?.topic() shouldBe topic

            repo.hentAlleVedtakMedStatus(Status.Type.MOTTATT) shouldBe emptyList()

            with(repo.hentVedtak(mottattVedtak1.behandlingId)?.status) {
                this.shouldNotBeNull()
                type shouldBe Status.Type.TIL_UTBETALING
                this as Status.TilUtbetaling
                eksternStatus shouldBe Status.UtbetalingStatus.SENDT
            }
            with(repo.hentVedtak(mottattVedtak2.behandlingId)?.status) {
                this.shouldNotBeNull()
                type shouldBe Status.Type.TIL_UTBETALING
                this as Status.TilUtbetaling
                eksternStatus shouldBe Status.UtbetalingStatus.SENDT
            }
        }
    }

    @Test
    fun `sender bare en ubtbetaling av gangen for en person`() {
        withMigratedDb {
            val repo = UtbetalingPostgresRepository(dataSource)
            val topic = "utbetaling.topic"
            val producer = MockProducer(true, RoundRobinPartitioner(), StringSerializer(), StringSerializer())
            val helvedUtsender = HelvedUtsender(topic, producer)
            val hjelper = UtsendingsHjelper(repo, helvedUtsender, TestRapid())
            val sak1 = UUID.randomUUID()
            val mottattVedtak1 = vedtak(sakId = sak1)
            val mottattVedtak2 = vedtak(sakId = sak1)

            repo.lagreVedtak(mottattVedtak1)
            repo.lagreVedtak(mottattVedtak2)

            hjelper.behandleUtbetalingVedtak()

            producer.history().shouldNotBeEmpty()
            producer.history().firstOrNull()?.topic() shouldBe topic

            repo.hentAlleVedtakMedStatus(Status.Type.MOTTATT).size shouldBe 1

            repo.hentVedtak(mottattVedtak1.behandlingId)?.status?.type shouldBe Status.Type.TIL_UTBETALING
            repo.hentVedtak(mottattVedtak2.behandlingId)?.status?.type shouldBe Status.Type.MOTTATT
        }
    }

    @Test
    fun `varsler ikke om en utbetaling som venter på svar er fersk`() {
        withMigratedDb {
            val repo = UtbetalingPostgresRepository(dataSource)
            val producer = MockProducer(true, RoundRobinPartitioner(), StringSerializer(), StringSerializer())
            val helvedUtsender = HelvedUtsender("utbetaling.topic", producer)
            val rapid = TestRapid()
            val hjelper =
                UtsendingsHjelper(
                    repo = repo,
                    utsender = helvedUtsender,
                    rapidsConnection = rapid,
                    varslingOmHengendeUtbetaling =
                        GjentagendeVarsling(
                            minsteVentetidFørVarsel = Duration.ofHours(1),
                            tidMellomGjentatteVarsler = Duration.ofHours(1),
                            toleransevindu = Duration.ofMinutes(1),
                        ),
                )
            val sak1 = UUID.randomUUID()
            val ventendeVedtak = vedtak(sakId = sak1)
            val nyttVedtak = vedtak(sakId = sak1)

            repo.lagreVedtak(ventendeVedtak)
            repo.lagreVedtak(nyttVedtak)
            repo.oppdaterStatus(ventendeVedtak.behandlingId, Status.TilUtbetaling(Status.UtbetalingStatus.SENDT))
            settSistEndretTilstand(dataSource, ventendeVedtak.behandlingId, LocalDateTime.now().minusMinutes(1))

            hjelper.behandleUtbetalingVedtak()

            rapid.inspektør.size shouldBe 0
        }
    }

    @Test
    fun `varsler når en utbetaling som venter på svar har hengt lenger enn terskelen`() {
        withMigratedDb {
            val repo = UtbetalingPostgresRepository(dataSource)
            val producer = MockProducer(true, RoundRobinPartitioner(), StringSerializer(), StringSerializer())
            val helvedUtsender = HelvedUtsender("utbetaling.topic", producer)
            val rapid = TestRapid()
            val hjelper =
                UtsendingsHjelper(
                    repo = repo,
                    utsender = helvedUtsender,
                    rapidsConnection = rapid,
                    varslingOmHengendeUtbetaling =
                        GjentagendeVarsling(
                            minsteVentetidFørVarsel = Duration.ofHours(1),
                            tidMellomGjentatteVarsler = Duration.ofHours(1),
                            toleransevindu = Duration.ofMinutes(1),
                        ),
                )
            val sak1 = UUID.randomUUID()
            val ventendeVedtak = vedtak(sakId = sak1)
            val nyttVedtak = vedtak(sakId = sak1)

            repo.lagreVedtak(ventendeVedtak)
            repo.lagreVedtak(nyttVedtak)
            repo.oppdaterStatus(ventendeVedtak.behandlingId, Status.TilUtbetaling(Status.UtbetalingStatus.SENDT))
            // Har ventet 1 time og 30 sekunder, rett over terskelen
            settSistEndretTilstand(
                dataSource,
                ventendeVedtak.behandlingId,
                LocalDateTime.now().minusHours(1).minusSeconds(30),
            )

            hjelper.behandleUtbetalingVedtak()

            rapid.inspektør.size shouldBe 1
            rapid.inspektør.field(0, "@event_name").asText() shouldBe "utbetaling_henger"
            rapid.inspektør.field(0, "sakId").asText() shouldBe sak1.toString()
        }
    }

    @Test
    fun `varsler ikke igjen mellom varslingsintervallene`() {
        withMigratedDb {
            val repo = UtbetalingPostgresRepository(dataSource)
            val producer = MockProducer(true, RoundRobinPartitioner(), StringSerializer(), StringSerializer())
            val helvedUtsender = HelvedUtsender("utbetaling.topic", producer)
            val rapid = TestRapid()
            val hjelper =
                UtsendingsHjelper(
                    repo = repo,
                    utsender = helvedUtsender,
                    rapidsConnection = rapid,
                    varslingOmHengendeUtbetaling =
                        GjentagendeVarsling(
                            minsteVentetidFørVarsel = Duration.ofHours(1),
                            tidMellomGjentatteVarsler = Duration.ofHours(1),
                            toleransevindu = Duration.ofMinutes(1),
                        ),
                )
            val sak1 = UUID.randomUUID()
            val ventendeVedtak = vedtak(sakId = sak1)
            val nyttVedtak = vedtak(sakId = sak1)

            repo.lagreVedtak(ventendeVedtak)
            repo.lagreVedtak(nyttVedtak)
            repo.oppdaterStatus(ventendeVedtak.behandlingId, Status.TilUtbetaling(Status.UtbetalingStatus.SENDT))
            // Har ventet 1 time og 30 minutter, midt mellom varslingsintervallene på 1 time
            settSistEndretTilstand(
                dataSource,
                ventendeVedtak.behandlingId,
                LocalDateTime.now().minusHours(1).minusMinutes(30),
            )

            hjelper.behandleUtbetalingVedtak()

            rapid.inspektør.size shouldBe 0
        }
    }
}

private fun settSistEndretTilstand(
    dataSource: DataSource,
    behandlingId: UUID,
    tidspunkt: LocalDateTime,
) {
    sessionOf(dataSource).use { session ->
        session.run(
            queryOf(
                "UPDATE utbetaling SET sist_endret_tilstand = :tidspunkt WHERE behandling_id = :behandlingId",
                mapOf(
                    "tidspunkt" to tidspunkt,
                    "behandlingId" to behandlingId,
                ),
            ).asUpdate,
        )
    }
}
