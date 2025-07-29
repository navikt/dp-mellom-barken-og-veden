package no.nav.dagpenger.mellom.barken.og.veden.helved

import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord

class HelvedUtsender(
    private val topic: String,
    private val producer: Producer<String, String>,
) {
    fun send(
        behandlingId: BehandlingId,
        utbetaling: String,
    ) {
        val record =
            ProducerRecord(
                topic,
                behandlingId.uuid.toString(),
                utbetaling,
            )
        producer.send(record)
    }
}
