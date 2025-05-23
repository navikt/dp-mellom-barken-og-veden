package no.nav.dagpenger.mellom.barken.og.veden.helved

import java.lang.Long
import java.nio.ByteBuffer
import java.util.Base64
import java.util.UUID

data class UtbetalingId(
    val uuid: UUID,
) {
    private val byteBuffer =
        ByteBuffer.allocate(Long.BYTES * 2).apply {
            // 128 bits
            putLong(uuid.mostSignificantBits) // første 64 bits
            putLong(uuid.leastSignificantBits) // siste 64 bits
        }

    override fun toString(): String =
        Base64.getEncoder().encodeToString(byteBuffer.array()).also {
            require(it.length <= 30) { "base64 encoding av UUID ble over 30 tegn." }
        }

    companion object {
        fun fromString(base64: String): UtbetalingId {
            val decoded = Base64.getDecoder().decode(base64)
            val byteBuffer = ByteBuffer.wrap(decoded)
            val mostSignificantBits = byteBuffer.long
            val leastSignificantBits = byteBuffer.long
            return UtbetalingId(UUID(mostSignificantBits, leastSignificantBits))
        }
    }
}
