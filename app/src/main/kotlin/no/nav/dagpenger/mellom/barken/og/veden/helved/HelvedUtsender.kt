package no.nav.dagpenger.mellom.barken.og.veden.helved

import no.nav.dagpenger.mellom.barken.og.veden.domene.Ident
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord

class HelvedUtsender(
    private val topic: String,
    private val producer: Producer<String, String>,
) {
    fun send(
        ident: Ident,
        utbetaling: String,
    ) {
        val record =
            ProducerRecord(
                topic,
                ident.verdi,
                utbetaling,
            )
        producer.send(record)
    }
}
