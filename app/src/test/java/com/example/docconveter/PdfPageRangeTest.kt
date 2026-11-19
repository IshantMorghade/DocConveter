
package com.example.docconveter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PdfPageRangeTest {

    @Test
    fun blankRange_returnsAllPages() {
        val result = PdfPageRange.parsePageRange("", 10)

        assertEquals((1..10).toSet(), result)
    }

    @Test
    fun whitespaceRange_returnsAllPages() {
        val result = PdfPageRange.parsePageRange("   ", 5)

        assertEquals((1..5).toSet(), result)
    }

    @Test
    fun validIndividualPages_returnsSelectedPages() {
        val result = PdfPageRange.parsePageRange("1, 5, 8", 10)

        assertEquals(setOf(1, 5, 8), result)
    }

    @Test
    fun validRange_returnsAllPagesInRange() {
        val result = PdfPageRange.parsePageRange("2-5", 10)

        assertEquals(setOf(2, 3, 4, 5), result)
    }

    @Test
    fun combinedSelection_returnsCorrectPages() {
        val result = PdfPageRange.parsePageRange("1-3, 5, 8-10", 10)

        assertEquals(
            setOf(1, 2, 3, 5, 8, 9, 10),
            result
        )
    }

    @Test
    fun duplicatePages_areRemoved() {
        val result = PdfPageRange.parsePageRange("1, 2, 2, 3, 1", 10)

        assertEquals(setOf(1, 2, 3), result)
    }

    @Test
    fun pageBeyondTotal_throwsException() {
        assertThrows(Exception::class.java) {
            PdfPageRange.parsePageRange("11", 10)
        }
    }

    @Test
    fun zeroPage_throwsException() {
        assertThrows(Exception::class.java) {
            PdfPageRange.parsePageRange("0", 10)
        }
    }

    @Test
    fun reversedRange_throwsException() {
        assertThrows(Exception::class.java) {
            PdfPageRange.parsePageRange("5-2", 10)
        }
    }

    @Test
    fun invalidRangeFormat_throwsException() {
        assertThrows(Exception::class.java) {
            PdfPageRange.parsePageRange("1-3-5", 10)
        }
    }

    @Test
    fun invalidPageText_throwsException() {
        assertThrows(Exception::class.java) {
            PdfPageRange.parsePageRange("1, abc, 3", 10)
        }
    }

    @Test
    fun zeroTotalPages_throwsException() {
        assertThrows(Exception::class.java) {
            PdfPageRange.parsePageRange("", 0)
        }
    }

    @Test
    fun largePdf197Pages_returnsCorrectRange() {
        val result = PdfPageRange.parsePageRange("50-150", 197)

        assertEquals((50..150).toSet(), result)
    }
}
