package no.nav.dagpenger.mellom.barken.og.veden

import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType

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
            ),
        )

    internal val properties =
        ConfigurationProperties.systemProperties() overriding EnvironmentVariables() overriding defaultProperties

    val config: Map<String, String> =
        properties.list().reversed().fold(emptyMap()) { map, pair ->
            map + pair.second
        }

    val utbetalingTopic: String = properties[Key("UTBETALING_TOPIC", stringType)]

    fun electorPath(): String = properties[Key("ELECTOR_GET_URL", stringType)]
}
