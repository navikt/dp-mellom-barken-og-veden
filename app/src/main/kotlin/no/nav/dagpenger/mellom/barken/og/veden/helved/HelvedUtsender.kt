package no.nav.dagpenger.mellom.barken.og.veden.helved

import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import java.util.UUID

class HelvedUtsender(
    private val topic: String,
    private val producer: Producer<String, String>,
) {
    fun send(
        behandlingId: UUID,
        utbetaling: String,
    ) {
        val record =
            ProducerRecord(
                topic,
                behandlingId.toString(),
                utbetaling,
            )
        producer.send(record)
    }
}
