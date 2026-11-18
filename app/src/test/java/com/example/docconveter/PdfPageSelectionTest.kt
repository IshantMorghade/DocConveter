
package com.example.docconveter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PdfPageSelectionTest {

    @Test
    fun allPages_returnsEveryPageIndex() {
        val result = PdfPageSelection.parsePageSelection(
            mode = "All pages",
            input = "",
            pageCount = 5
        )

        assertEquals(listOf(0, 1, 2, 3, 4), result)
    }

    @Test
    fun validPageRange_returnsCorrectIndexes() {
        val result = PdfPageSelection.parsePageSelection(
            mode = "Page range",
            input = "2-5",
            pageCount = 10
        )

        assertEquals(listOf(1, 2, 3, 4), result)
    }

    @Test
    fun validIndividualPages_returnsCorrectIndexes() {
        val result = PdfPageSelection.parsePageSelection(
            mode = "Individual pages",
            input = "1,3,7",
            pageCount = 10
        )

        assertEquals(listOf(0, 2, 6), result)
    }

    @Test
    fun duplicatePages_throwsException() {
        assertThrows(IllegalArgumentException::class.java) {
            PdfPageSelection.parsePageSelection(
                mode = "Individual pages",
                input = "1,3,3",
                pageCount = 10
            )
        }
    }

    @Test
    fun pageBeyondTotal_throwsException() {
        assertThrows(IllegalArgumentException::class.java) {
            PdfPageSelection.parsePageSelection(
                mode = "Page range",
                input = "1-11",
                pageCount = 10
            )
        }
    }

    @Test
    fun reversedRange_throwsException() {
        assertThrows(IllegalArgumentException::class.java) {
            PdfPageSelection.parsePageSelection(
                mode = "Page range",
                input = "8-3",
                pageCount = 10
            )
        }
    }

    @Test
    fun invalidRangeFormat_throwsException() {
        assertThrows(IllegalArgumentException::class.java) {
            PdfPageSelection.parsePageSelection(
                mode = "Page range",
                input = "abc",
                pageCount = 10
            )
        }
    }

    @Test
    fun zeroPageCount_throwsException() {
        assertThrows(IllegalArgumentException::class.java) {
            PdfPageSelection.parsePageSelection(
                mode = "All pages",
                input = "",
                pageCount = 0
            )
        }
    }

    @Test
    fun largePdf197Pages_returnsCorrectRange() {
        val result = PdfPageSelection.parsePageSelection(
            mode = "Page range",
            input = "50-100",
            pageCount = 197
        )

        assertEquals(51, result.size)
        assertEquals(49, result.first())
        assertEquals(99, result.last())
    }
}
