package no.nav.dagpenger.mellom.barken.og.veden.utbetaling

import no.nav.dagpenger.mellom.barken.og.veden.helved.BehandlingId
import org.junit.jupiter.api.Assertions.assertEquals
import java.util.UUID
import kotlin.test.Test

class BehandlingIdTest {
    @Test
    fun `kan konvertere til og fra base64`() {
        val uuid = BehandlingId(UUID.randomUUID())
        val base64 = uuid.tilBase64()
        val decoded = BehandlingId.fromString(base64)

        assertEquals(uuid.uuid, decoded.uuid)
    }
}
