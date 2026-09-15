package no.nav.dagpenger.mellom.barken.og.veden.utbetaling

import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.UUID

/**
 * De første sakene i dp-behandling ble opprettet før sakId ble en del av behandlingskjeden.
 * For disse sakene har vi en statisk oversikt over hvilken sakId som hører til hvilken
 * behandlingskjedeId. For alle senere saker er behandlingskjedeId og sakId lik, og vi
 * trenger ikke slå opp noe.
 *
 * Kilde for sakid_mapping.tsv kommer fra dp-saksbehandling (https://github.com/navikt/dp-saksbehandling)
 */
internal class SakIdMapper(
    resourcePath: String = "/sakid-mapping.tsv",
) {
    private companion object {
        private val logger = KotlinLogging.logger {}
    }

    private val mapping: Map<UUID, UUID> = lesMapping(resourcePath)

    fun sakIdFor(behandlingskjedeId: UUID): UUID =
        mapping[behandlingskjedeId]?.also {
            logger.info { "Fant sakId=$it for behandlingskjedeId=$behandlingskjedeId i mapping-tabellen" }
        } ?: behandlingskjedeId

    private fun lesMapping(resourcePath: String): Map<UUID, UUID> {
        val tekst =
            javaClass.getResourceAsStream(resourcePath)?.bufferedReader()?.readText()
                ?: throw IllegalStateException("Fant ikke mapping-fil $resourcePath på klassestien")

        return tekst
            .lineSequence()
            .drop(1) // header
            .filter { it.isNotBlank() }
            .associate { linje ->
                val (behandlingskjedeId, sakId) =
                    linje.split("\t").map { felt -> felt.trim() }
                UUID.fromString(behandlingskjedeId) to UUID.fromString(sakId)
            }
    }
}
