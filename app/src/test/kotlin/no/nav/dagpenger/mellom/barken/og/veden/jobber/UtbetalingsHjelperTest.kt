package no.nav.dagpenger.mellom.barken.og.veden.jobber

import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.mellom.barken.og.veden.PostgresConfiguration.dataSource
import no.nav.dagpenger.mellom.barken.og.veden.helved.HelvedUtsender
import no.nav.dagpenger.mellom.barken.og.veden.repository.Postgres.withMigratedDb
import no.nav.dagpenger.mellom.barken.og.veden.repository.vedtak
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.Status
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.jobber.UtsendingsHjelper
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.repository.UtbetalingPostgresRepository
import org.apache.kafka.clients.producer.MockProducer
import org.apache.kafka.clients.producer.RoundRobinPartitioner
import org.apache.kafka.common.serialization.StringSerializer
import org.junit.jupiter.api.Test
import java.util.UUID

class UtbetalingsHjelperTest {
    @Test
    fun `kan hente og oppdatere utbetalinger som ligger klare`() {
        withMigratedDb {
            val repo = UtbetalingPostgresRepository(dataSource)
            val topic = "utbetaling.topic"
            val producer = MockProducer(true, RoundRobinPartitioner(), StringSerializer(), StringSerializer())
            val helvedUtsender = HelvedUtsender(topic, producer)
            val hjelper = UtsendingsHjelper(repo, helvedUtsender)
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
            val hjelper = UtsendingsHjelper(repo, helvedUtsender)
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
}
