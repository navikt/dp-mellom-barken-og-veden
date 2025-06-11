package no.nav.dagpenger.mellom.barken.og.veden.jobber

import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.mellom.barken.og.veden.PostgresConfiguration.dataSource
import no.nav.dagpenger.mellom.barken.og.veden.helved.HelvedUtsender
import no.nav.dagpenger.mellom.barken.og.veden.repository.Postgres.withMigratedDb
import no.nav.dagpenger.mellom.barken.og.veden.repository.vedtak
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.Status
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.jobber.UtsendingsHjelper
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.repository.UtbetalingPostgresRepository
import org.apache.kafka.clients.producer.MockProducer
import org.apache.kafka.common.serialization.StringSerializer
import org.junit.jupiter.api.Test

class UtbetalingsHjelperTest {
    @Test
    fun `kan hente og oppdatere utbetalinger som ligger klare`() {
        withMigratedDb {
            val repo = UtbetalingPostgresRepository(dataSource)
            val topic = "utbetaling.topic"
            val producer = MockProducer<String, String>(true, StringSerializer(), StringSerializer())
            val helvedUtsender = HelvedUtsender(topic, producer)
            val hjelper = UtsendingsHjelper(repo, helvedUtsender)
            val mottattVedtak1 = vedtak(sakId = "s1")
            val mottattVedtak2 = vedtak(sakId = "s2")

            repo.lagreVedtak(mottattVedtak1)
            repo.lagreVedtak(mottattVedtak2)

            hjelper.behandleUtbetalingVedtak()

            producer.history().shouldNotBeEmpty()
            producer.history().firstOrNull()?.topic() shouldBe topic

            repo.hentAlleVedtakMedStatus(Status.Mottatt) shouldBe emptyList()

            repo.hentVedtak(mottattVedtak1.behandlingId)?.status shouldBe Status.TilUtbetaling(Status.UtbetalingStatus.SENDT)
            repo.hentVedtak(mottattVedtak2.behandlingId)?.status shouldBe Status.TilUtbetaling(Status.UtbetalingStatus.SENDT)
        }
    }

    @Test
    fun `sender bare en ubtbetaling av gangen for en person`() {
        withMigratedDb {
            val repo = UtbetalingPostgresRepository(dataSource)
            val topic = "utbetaling.topic"
            val producer = MockProducer<String, String>(true, StringSerializer(), StringSerializer())
            val helvedUtsender = HelvedUtsender(topic, producer)
            val hjelper = UtsendingsHjelper(repo, helvedUtsender)
            val mottattVedtak1 = vedtak(sakId = "s1")
            val mottattVedtak2 = vedtak(sakId = "s1")

            repo.lagreVedtak(mottattVedtak1)
            repo.lagreVedtak(mottattVedtak2)

            hjelper.behandleUtbetalingVedtak()

            producer.history().shouldNotBeEmpty()
            producer.history().firstOrNull()?.topic() shouldBe topic

            repo.hentAlleVedtakMedStatus(Status.Mottatt).size shouldBe 1

            repo.hentVedtak(mottattVedtak1.behandlingId)?.status shouldBe Status.TilUtbetaling(Status.UtbetalingStatus.SENDT)
            repo.hentVedtak(mottattVedtak2.behandlingId)?.status shouldBe Status.Mottatt
        }
    }
}
