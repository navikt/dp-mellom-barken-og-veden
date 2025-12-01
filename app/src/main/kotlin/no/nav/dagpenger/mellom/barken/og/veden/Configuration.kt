package no.nav.dagpenger.mellom.barken.og.veden

import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.oauth2.CachedOauth2Client
import no.nav.dagpenger.oauth2.OAuth2Config
import kotlin.String

object Configuration {
    val utvikler = "7e7a9ef8-d9ba-445b-bb91-d2b3c10a0c13"
    const val APP_NAME = "dp-mellom-barken-og-veden"

    private val defaultProperties =
        ConfigurationMap(
            mapOf(
                "RAPID_APP_NAME" to "dp-mellom-barken-og-veden",
                "KAFKA_CONSUMER_GROUP_ID" to "dp-mellom-barken-og-veden-v1",
                "KAFKA_RAPID_TOPIC" to "teamdagpenger.rapid.v1",
                "KAFKA_EXTRA_TOPIC" to "helved.status.v1",
                "KAFKA_RESET_POLICY" to "EARLIEST",
                "UTBETALING_TOPIC" to "teamdagpenger.utbetaling.v1",
                "DP_SAKSBEHANDLING_URL" to "http://dp-saksbehandling/",
            ),
        )

    internal val properties =
        ConfigurationProperties.systemProperties() overriding EnvironmentVariables() overriding defaultProperties

    val config: Map<String, String> =
        properties.list().reversed().fold(emptyMap()) { map, pair ->
            map + pair.second
        }

    val localHostname = properties[Key("HOSTNAME", stringType)]

    val utbetalingTopic: String = properties[Key("UTBETALING_TOPIC", stringType)]

    val sakApiBaseUrl: String = properties[Key("DP_SAKSBEHANDLING_URL", stringType)]
    val sakApiToken: () -> String = azureAdTokenSupplier(properties[Key("DP_SAKSBEHANDLING_SCOPE", stringType)])

    fun electorPath(): String = properties[Key("ELECTOR_GET_URL", stringType)]

    private val azureAdClient: CachedOauth2Client by lazy {
        val azureAdConfig = OAuth2Config.AzureAd(config)
        CachedOauth2Client(
            tokenEndpointUrl = azureAdConfig.tokenEndpointUrl,
            authType = azureAdConfig.clientSecret(),
        )
    }

    private fun azureAdTokenSupplier(scope: String): () -> String =
        {
            runBlocking { azureAdClient.clientCredentials(scope).access_token }
                ?: throw RuntimeException("Kunne ikke hente 'access_token' fra Azure AD for scope $scope")
        }
}
