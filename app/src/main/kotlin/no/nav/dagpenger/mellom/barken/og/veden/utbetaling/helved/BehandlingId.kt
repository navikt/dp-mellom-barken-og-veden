package no.nav.dagpenger.mellom.barken.og.veden.utbetaling.helved

import java.lang.Long
import java.nio.ByteBuffer
import java.util.Base64
import java.util.UUID

@JvmInline
value class BehandlingId(
    val uuid: UUID,
) {
    override fun toString(): String = """BehandlingId: $uuid - Base64 versjon: ${uuid.tilBase64()}"""
}

fun UUID.tilBase64(): String {
    val byteBuffer = ByteBuffer.allocate(Long.BYTES * 2)
    byteBuffer.putLong(this.mostSignificantBits)
    byteBuffer.putLong(this.leastSignificantBits)

    return Base64.getEncoder().encodeToString(byteBuffer.array()).also {
        require(it.length <= 30) { "base64 encoding av UUID ble over 30 tegn." }
    }
}

fun String.fraBase64(): UUID {
    val decoded = Base64.getDecoder().decode(this)
    val byteBuffer = ByteBuffer.wrap(decoded)
    val mostSignificantBits = byteBuffer.long
    val leastSignificantBits = byteBuffer.long
    return UUID(mostSignificantBits, leastSignificantBits)
}
