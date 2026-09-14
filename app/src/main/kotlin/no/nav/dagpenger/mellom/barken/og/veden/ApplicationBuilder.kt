package no.nav.dagpenger.mellom.barken.og.veden

import com.github.navikt.tbd_libs.kafka.AivenConfig
import com.github.navikt.tbd_libs.kafka.ConsumerProducerFactory
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.mellom.barken.og.veden.PostgresConfiguration.dataSource
import no.nav.dagpenger.mellom.barken.og.veden.leaderelection.LeaderElectionClient
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.BehovsløserFerietilleggBeløpMottak
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.MeldingOmUtbetalingVedtakMottak
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.SakIdHenter
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.api.utbetalingApiModule
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.helved.HelvedStatusMottak
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.helved.HelvedUtsender
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.helved.repository.HelvedPostgresRepository
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.helved.repository.Repo
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.jobber.BehandleMottatteUtbetalinger
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.jobber.UtsendingsHjelper
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.repository.UtbetalingPostgresRepository
import no.nav.helse.rapids_rivers.RapidApplication

internal class ApplicationBuilder(
    config: Map<String, String>,
) : RapidsConnection.StatusListener {
    private val utbetalingRepo = UtbetalingPostgresRepository(dataSource)
    private val helvedRepo = HelvedPostgresRepository()
    private val repo = Repo(dataSource, utbetalingRepo, helvedRepo)
    private val consumerProducerFactory = ConsumerProducerFactory(AivenConfig.default)
    private val producer = consumerProducerFactory.createProducer()
    private val helvedUtsender = HelvedUtsender(Configuration.utbetalingTopic, producer)

    companion object {
        private val logger = KotlinLogging.logger { }
    }

    private val rapidsConnection: RapidsConnection =
        RapidApplication
            .create(
                env = config,
                builder = {
                    withKtorModule { utbetalingApiModule(utbetalingRepo, rapidsConnection) }
                },
            ).apply {
                MeldingOmUtbetalingVedtakMottak(
                    rapidsConnection = this,
                    repo = utbetalingRepo,
                    sakIdHenter = SakIdHenter(Configuration.sakApiBaseUrl, Configuration.sakApiToken),
                )
                BehovsløserFerietilleggBeløpMottak(
                    rapidsConnection = this,
                    repo = utbetalingRepo,
                )
                HelvedStatusMottak(
                    rapidsConnection = this,
                    utbetalingRepo = utbetalingRepo,
                    repo = repo,
                )
            }

    private val utsendingsHjelper = UtsendingsHjelper(utbetalingRepo, helvedUtsender, rapidsConnection)

    init {
        rapidsConnection.register(this)
        BehandleMottatteUtbetalinger(
            leaderElection = createLeaderElectionClient(),
            utsendingsHjelper = utsendingsHjelper,
        ).start()
    }

    fun start() = rapidsConnection.start()

    fun stop() = rapidsConnection.stop()

    override fun onStartup(rapidsConnection: RapidsConnection) {
        logger.info { "Starter opp dp-mellom-barken-og-veden" }
        PostgresConfiguration.runMigration()
    }
}

private fun createLeaderElectionClient() =
    LeaderElectionClient(
        electorPath = Configuration.electorPath(),
    )
