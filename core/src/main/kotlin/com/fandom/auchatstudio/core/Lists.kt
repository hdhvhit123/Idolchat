package com.fandom.auchatstudio.core

object ListOps {
    /** Returns a copy of [list] with the element at [from] moved to [to]. Out-of-range is a no-op. */
    fun <T> move(list: List<T>, from: Int, to: Int): List<T> {
        if (from !in list.indices || to !in list.indices || from == to) return list
        val result = list.toMutableList()
        val item = result.removeAt(from)
        result.add(to, item)
        return result
    }
}

/** Splits a long screenshot into pages so a single Bitmap never gets too big for memory. */
object ExportPaging {
    /**
     * Groups consecutive item heights into pages whose summed height is at most [maxPageHeight].
     * An item taller than a page gets a page of its own. Returns index ranges into [heights].
     */
    fun split(heights: List<Int>, maxPageHeight: Int): List<IntRange> {
        require(maxPageHeight > 0) { "maxPageHeight must be positive" }
        if (heights.isEmpty()) return emptyList()
        val pages = mutableListOf<IntRange>()
        var start = 0
        var current = 0
        heights.forEachIndexed { i, h ->
            if (i > start && current + h > maxPageHeight) {
                pages += start until i
                start = i
                current = 0
            }
            current += h
        }
        pages += start until heights.size
        return pages
    }
}

object Watermark {
    const val TEXT = "Nội dung hư cấu tạo bởi AU Studio"
}
