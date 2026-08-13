package com.jp.whatsappclone.data

import com.jp.whatsappclone.data.util.UuidV7
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UuidV7Test {
    @Test
    fun generatedIdsAreRfc9562Version7AndCarryTheTimestamp() {
        val before = System.currentTimeMillis()
        val value = UuidV7.new()
        val after = System.currentTimeMillis()

        assertEquals(7, value.version())
        assertEquals(2, value.variant())
        assertTrue(UuidV7.timestampMillis(value) in before..after)
    }

    @Test
    fun encodedTimestampMakesIdsSortableAcrossDifferentMilliseconds() {
        val first = UuidV7.new(1_700_000_000_000)
        val second = UuidV7.new(1_700_000_000_001)

        assertTrue(first.toString() < second.toString())
        assertEquals(1_700_000_000_000, UuidV7.timestampMillis(UUID.fromString(first.toString())))
    }

    @Test
    fun repeatedGenerationAtOneMillisecondKeepsTheClientIdUnique() {
        val ids = List(1_000) { UuidV7.new(1_700_000_000_000).toString() }

        assertEquals(ids.size, ids.toSet().size)
    }
}
