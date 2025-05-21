package no.nav.dagpenger.mellom.barken.og.veden

import com.github.navikt.tbd_libs.naisful.naisApp
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.Clock
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.prometheus.metrics.model.registry.PrometheusRegistry
import mu.KotlinLogging
import no.nav.dagpenger.mellom.barken.og.veden.PostgresConfiguration.dataSource
import no.nav.dagpenger.mellom.barken.og.veden.api.authenticationConfig
import no.nav.dagpenger.mellom.barken.og.veden.api.utbetalingApi
import no.nav.dagpenger.mellom.barken.og.veden.jobber.BehandleMottatteUtbetalinger
import no.nav.dagpenger.mellom.barken.og.veden.jobber.UtsendingsHjelper
import no.nav.dagpenger.mellom.barken.og.veden.leaderelection.LeaderElectionClient
import no.nav.dagpenger.mellom.barken.og.veden.mottak.MeldingOmUtbetalingVedtakMottak
import no.nav.dagpenger.mellom.barken.og.veden.repository.UtbetalingPostgresRepository
import no.nav.helse.rapids_rivers.RapidApplication

internal class ApplicationBuilder(
    config: Map<String, String>,
) : RapidsConnection.StatusListener {
    private val repo = UtbetalingPostgresRepository(dataSource)
    private val utsendingsHjelper = UtsendingsHjelper(repo)

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
                            applicationLogger = KotlinLogging.logger("ApplicationLogger"),
                            callLogger = KotlinLogging.logger("CallLogger"),
                            aliveCheck = rapid::isReady,
                            readyCheck = rapid::isReady,
                            preStopHook = preStopHook::handlePreStopRequest,
                        ) {
                            authenticationConfig()
                            utbetalingApi()
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
        PostgresConfiguration.clean()
        PostgresConfiguration.runMigration()
    }
}

private fun createLeaderElectionClient() =
    LeaderElectionClient(
        electorPath = Configuration.electorPath(),
    )
