package no.nav.dagpenger.mellom.barken.og.veden

import com.github.navikt.tbd_libs.kafka.AivenConfig
import com.github.navikt.tbd_libs.kafka.ConsumerProducerFactory
import com.github.navikt.tbd_libs.naisful.naisApp
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.Clock
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.prometheus.metrics.model.registry.PrometheusRegistry
import no.nav.dagpenger.mellom.barken.og.veden.PostgresConfiguration.dataSource
import no.nav.dagpenger.mellom.barken.og.veden.leaderelection.LeaderElectionClient
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.MeldingOmUtbetalingVedtakMottak
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.SakIdHenter
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.api.authenticationConfig
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.api.utbetalingApi
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.helved.HelvedStatusMottak
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.helved.HelvedUtsender
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.helved.repository.HelvedPostgresRepository
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.helved.repository.Repo
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.jobber.BehandleMottatteUtbetalinger
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.jobber.UtsendingsHjelper
import no.nav.dagpenger.mellom.barken.og.veden.utbetaling.repository.UtbetalingPostgresRepository
import no.nav.helse.rapids_rivers.RapidApplication
import org.slf4j.LoggerFactory

internal class ApplicationBuilder(
    config: Map<String, String>,
) : RapidsConnection.StatusListener {
    private val utbetalingRepo = UtbetalingPostgresRepository(dataSource)
    private val helvedRepo = HelvedPostgresRepository()
    private val repo = Repo(dataSource, utbetalingRepo, helvedRepo)
    private val consumerProducerFactory = ConsumerProducerFactory(AivenConfig.default)
    private val producer = consumerProducerFactory.createProducer()
    private val helvedUtsender = HelvedUtsender(Configuration.utbetalingTopic, producer)
    private val utsendingsHjelper = UtsendingsHjelper(utbetalingRepo, helvedUtsender)

    companion object {
        private val logger = KotlinLogging.logger { }
    }

    private val rapidsConnection: RapidsConnection =
        RapidApplication
            .create(
                env = config,
                builder = {
                    withKtor { preStopHook, rapid ->
                        naisApp(
                            meterRegistry =
                                PrometheusMeterRegistry(
                                    PrometheusConfig.DEFAULT,
                                    PrometheusRegistry.defaultRegistry,
                                    Clock.SYSTEM,
                                ),
                            objectMapper = objectMapper,
                            applicationLogger = LoggerFactory.getLogger("ApplicationLogger"),
                            callLogger = LoggerFactory.getLogger("CallLogger"),
                            aliveCheck = rapid::isReady,
                            readyCheck = rapid::isReady,
                            preStopHook = preStopHook::handlePreStopRequest,
                        ) {
                            authenticationConfig()
                            utbetalingApi(utbetalingRepo)
                        }
                    }
                },
            ).apply {
                BehandleMottatteUtbetalinger(
                    leaderElection = createLeaderElectionClient(),
                    utsendingsHjelper = utsendingsHjelper,
                ).start()
                MeldingOmUtbetalingVedtakMottak(
                    rapidsConnection = this,
                    repo = utbetalingRepo,
                    sakIdHenter = SakIdHenter(Configuration.sakApiBaseUrl, Configuration.sakApiToken),
                )
                HelvedStatusMottak(
                    rapidsConnection = this,
                    utbetalingRepo = utbetalingRepo,
                    repo = repo,
                )
            }

    init {
        rapidsConnection.register(this)
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
