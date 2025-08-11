package no.nav.dagpenger.mellom.barken.og.veden.helved

import java.lang.Long
import java.nio.ByteBuffer
import java.util.Base64
import java.util.UUID

@JvmInline
value class BehandlingId(
    val uuid: UUID,
) {
    private val byteBuffer get() =
        ByteBuffer.allocate(Long.BYTES * 2).apply {
            // 128 bits
            putLong(uuid.mostSignificantBits) // første 64 bits
            putLong(uuid.leastSignificantBits) // siste 64 bits
        }

    fun tilBase64(): String =
        Base64.getEncoder().encodeToString(byteBuffer.array()).also {
            require(it.length <= 30) { "base64 encoding av UUID ble over 30 tegn." }
        }

    override fun toString(): String = """BehandlingId: $uuid - Base64 versjon: ${tilBase64()}"""

    companion object {
        fun fromString(base64: String): BehandlingId {
            val decoded = Base64.getDecoder().decode(base64)
            val byteBuffer = ByteBuffer.wrap(decoded)
            val mostSignificantBits = byteBuffer.long
            val leastSignificantBits = byteBuffer.long
            return BehandlingId(UUID(mostSignificantBits, leastSignificantBits))
        }
    }
}
