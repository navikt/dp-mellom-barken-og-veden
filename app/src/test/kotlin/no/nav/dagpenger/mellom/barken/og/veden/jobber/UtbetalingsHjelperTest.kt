package no.nav.dagpenger.mellom.barken.og.veden.jobber

import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.mellom.barken.og.veden.PostgresConfiguration.dataSource
import no.nav.dagpenger.mellom.barken.og.veden.domene.UtbetalingStatus
import no.nav.dagpenger.mellom.barken.og.veden.helved.HelvedProducer
import no.nav.dagpenger.mellom.barken.og.veden.repository.Postgres.withMigratedDb
import no.nav.dagpenger.mellom.barken.og.veden.repository.UtbetalingPostgresRepository
import no.nav.dagpenger.mellom.barken.og.veden.repository.vedtak
import org.apache.kafka.clients.producer.MockProducer
import org.apache.kafka.common.serialization.StringSerializer
import org.junit.jupiter.api.Test

class UtbetalingsHjelperTest {
    @Test
    fun `kan hente og oppdatere utbetalinger som ligger klare`() {
        withMigratedDb {
            val repo = UtbetalingPostgresRepository(dataSource)
            val producer = MockProducer<String, String>(true, StringSerializer(), StringSerializer())
            val helvedProducer = HelvedProducer(producer)
            val hjelper = UtsendingsHjelper(repo, helvedProducer)
            val mottattVedtak1 = vedtak()
            val mottattVedtak2 = vedtak()

            repo.lagreVedtak(mottattVedtak1)
            repo.lagreVedtak(mottattVedtak2)

            hjelper.behandleUtbetalingVedtak()

            producer.history().shouldNotBeEmpty()

            repo.hentAlleVedtakMedStatus(UtbetalingStatus.MOTTATT) shouldBe emptyList()

            repo.hentVedtak(mottattVedtak1.behandlingId)?.status shouldBe UtbetalingStatus.SENDT
            repo.hentVedtak(mottattVedtak2.behandlingId)?.status shouldBe UtbetalingStatus.SENDT
        }
    }
}
