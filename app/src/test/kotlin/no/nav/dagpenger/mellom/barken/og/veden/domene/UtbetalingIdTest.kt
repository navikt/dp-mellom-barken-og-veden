package no.nav.dagpenger.mellom.barken.og.veden.domene

import org.junit.jupiter.api.Assertions.assertEquals
import java.util.UUID
import kotlin.test.Test

class UtbetalingIdTest {
    @Test
    fun `kan konvertere til og fra base64`() {
        val uuid = UtbetalingId(UUID.randomUUID())
        val base64 = uuid.toString()
        val decoded = UtbetalingId.fromString(base64)

        assertEquals(uuid.uuid, decoded.uuid)
    }
}
