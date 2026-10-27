package com.nexashift.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LayoutsTest {
    @Test
    fun everyLayoutHasFiveRowsAndNoEmptyKeys() {
        for ((id, rows) in Layouts.all) {
            assertEquals("rows in $id", 5, rows.size)
            for (row in rows) for (key in row) {
                assertTrue("empty label in $id", key.label.isNotEmpty())
                assertTrue("empty output in $id", key.output.isNotEmpty())
            }
        }
    }

    @Test
    fun everyLayoutHasEnterSpaceGlobeAndBackspace() {
        for ((id, rows) in Layouts.all) {
            val keys = rows.flatten()
            assertTrue("enter in $id", keys.any { it.type == KeyType.ENTER })
            assertTrue("space in $id", keys.any { it.type == KeyType.SPACE })
            assertTrue("globe in $id", keys.any { it.type == KeyType.GLOBE })
            assertTrue("backspace in $id", keys.any { it.type == KeyType.BACKSPACE })
        }
    }

    @Test
    fun bengaliAndHindiContainBaseLetters() {
        assertTrue(Layouts.get("bn1").flatten().any { it.output == "ক" })
        assertTrue(Layouts.get("hi1").flatten().any { it.output == "क" })
    }
}
