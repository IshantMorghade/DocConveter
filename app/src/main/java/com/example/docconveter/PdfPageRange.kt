
package com.example.docconveter

object PdfPageRange {

    fun parsePageRange(
        range: String,
        totalPages: Int
    ): Set<Int> {

        if (totalPages <= 0) {
            throw Exception("PDF must contain at least one page.")
        }

        if (range.isBlank()) {
            return (1..totalPages).toSet()
        }

        val pages = mutableSetOf<Int>()

        range.split(",").forEach { part ->

            val value = part.trim()

            if (value.contains("-")) {

                val limits = value.split("-")

                if (limits.size != 2) {
                    throw Exception("Invalid page range.")
                }

                val start = limits[0].trim().toIntOrNull()
                    ?: throw Exception("Invalid page number.")

                val end = limits[1].trim().toIntOrNull()
                    ?: throw Exception("Invalid page number.")

                if (
                    start < 1 ||
                    end > totalPages ||
                    start > end
                ) {
                    throw Exception(
                        "Enter a valid range between 1 and $totalPages."
                    )
                }

                pages.addAll(start..end)

            } else {

                val page = value.toIntOrNull()
                    ?: throw Exception("Invalid page number.")

                if (page < 1 || page > totalPages) {
                    throw Exception(
                        "Page number must be between 1 and $totalPages."
                    )
                }

                pages.add(page)
            }
        }

        if (pages.isEmpty()) {
            throw Exception("Enter at least one page number.")
        }

        return pages
    }
}
