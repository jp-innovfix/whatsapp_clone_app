package com.jp.whatsappclone.data.util

import java.security.SecureRandom
import java.util.UUID

/** RFC 9562 UUIDv7: Unix epoch milliseconds followed by cryptographically random bits. */
object UuidV7 {
    private val random = SecureRandom()

    @Synchronized
    fun new(): UUID = new(System.currentTimeMillis())

    @Synchronized
    internal fun new(timestampMillis: Long): UUID {
        require(timestampMillis in 0..0xFFFF_FFFF_FFFFL) { "timestamp out of UUIDv7 range" }
        val bytes = ByteArray(16).also(random::nextBytes)
        for (index in 0..5) {
            bytes[index] = (timestampMillis ushr (40 - index * 8)).toByte()
        }
        bytes[6] = ((bytes[6].toInt() and 0x0F) or 0x70).toByte()
        bytes[8] = ((bytes[8].toInt() and 0x3F) or 0x80).toByte()

        var mostSignificant = 0L
        var leastSignificant = 0L
        for (index in 0..7) mostSignificant = (mostSignificant shl 8) or (bytes[index].toLong() and 0xFF)
        for (index in 8..15) leastSignificant = (leastSignificant shl 8) or (bytes[index].toLong() and 0xFF)
        return UUID(mostSignificant, leastSignificant)
    }

    fun newString(): String = new().toString()

    fun timestampMillis(uuid: UUID): Long = uuid.mostSignificantBits ushr 16
}
