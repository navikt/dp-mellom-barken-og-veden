package no.nav.dagpenger.mellom.barken.og.veden.utbetaling

import no.nav.dagpenger.mellom.barken.og.veden.helved.fraBase64
import no.nav.dagpenger.mellom.barken.og.veden.helved.tilBase64
import org.junit.jupiter.api.Assertions.assertEquals
import java.util.UUID
import kotlin.test.Test

class BehandlingIdTest {
    @Test
    fun `kan konvertere til og fra base64`() {
        val uuid = UUID.randomUUID()
        val base64 = uuid.tilBase64()
        val decoded = base64.fraBase64()

        assertEquals(uuid, decoded)
    }
}
