package no.nav.dagpenger.mellom.barken.og.veden.helved

import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord

class HelvedProducer(
    private val producer: Producer<String, String>,
) {
    fun send(
        sakId: String,
        utbetaling: String,
    ) {
        val record = ProducerRecord<String, String>(sakId, utbetaling)
        producer.send(record)
    }
}
