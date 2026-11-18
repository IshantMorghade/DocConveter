
package com.example.docconveter

object PdfPageSelection {

    fun parsePageSelection(
        mode: String,
        input: String,
        pageCount: Int
    ): List<Int> {

        require(pageCount > 0) {
            "PDF contains no pages."
        }

        return when (mode) {

            "All pages" -> {
                (0 until pageCount).toList()
            }

            "Page range" -> {

                val match = Regex("""^\s*(\d+)\s*-\s*(\d+)\s*$""")
                    .matchEntire(input)
                    ?: throw IllegalArgumentException(
                        "Enter a page range like 1-10."
                    )

                val start = match.groupValues[1].toInt()
                val end = match.groupValues[2].toInt()

                require(
                    start >= 1 &&
                            end <= pageCount &&
                            start <= end
                ) {
                    "Enter a valid range between 1 and $pageCount."
                }

                (start - 1 until end).toList()
            }

            "Individual pages" -> {

                val pages = input.split(",").map { value ->

                    val page = value.trim().toIntOrNull()
                        ?: throw IllegalArgumentException(
                            "Enter page numbers separated by commas, e.g. 1,3,7."
                        )

                    require(page in 1..pageCount) {
                        "Page numbers must be between 1 and $pageCount."
                    }

                    page
                }

                require(pages.isNotEmpty()) {
                    "Select at least one page."
                }

                require(pages.distinct().size == pages.size) {
                    "Duplicate page numbers are not allowed."
                }

                pages.map { it - 1 }
            }

            else -> throw IllegalArgumentException(
                "Invalid page selection."
            )
        }
    }
}