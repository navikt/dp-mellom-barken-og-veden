package no.nav.dagpenger.mellom.barken.og.veden.helved

import mu.KLogger
import mu.KotlinLogging
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord

class HelvedUtsender(
    private val topic: String,
    private val producer: Producer<String, String>,
) {
    companion object {
        private val logger = KotlinLogging.logger { }
    }

    fun send(
        behandlingId: BehandlingId,
        utbetaling: String,
    ) {
        try {
            val record =
                ProducerRecord(
                    topic,
                    behandlingId.uuid.toString(),
                    utbetaling,
                )
            val metadata = producer.send(record).get()
            logger.info(
                "Utbetaling sendt til helved: ${metadata.topic()} med offset ${metadata.offset()}",
            )
            logger.sikkerlogg().info("Utbetaling sendt til helved: $utbetaling")
        } catch (e: Exception) {
            logger.error("Feil ved sending av utbetaling", e)
            throw e
        }
    }

    private fun KLogger.sikkerlogg() = KotlinLogging.logger("tjenestekall.$name")
}
