package com.jp.whatsappclone.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchIndexTest {
    @Test
    fun prefixQueryTreatsUserInputAsTokensInsteadOfFtsSyntax() {
        assertEquals("akshara* innovfix*", SearchIndex.prefixQuery(" Akshara @ Innovfix "))
        assertEquals("project* or* atlas*", SearchIndex.prefixQuery("project OR atlas"))
        assertNull(SearchIndex.prefixQuery(" -- !!! "))
    }

    @Test
    fun stableRowsAreRepeatableAndNamespacedByEntityType() {
        val first = SearchIndex.stableRowId(SearchIndex.MESSAGE, "same-id")
        assertEquals(first, SearchIndex.stableRowId(SearchIndex.MESSAGE, "same-id"))
        assertNotEquals(first, SearchIndex.stableRowId(SearchIndex.MEMBER, "same-id"))
        assertTrue(first > 0)
    }
}
