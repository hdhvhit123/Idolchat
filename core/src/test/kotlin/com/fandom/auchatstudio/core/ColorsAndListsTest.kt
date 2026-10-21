package com.fandom.auchatstudio.core

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ColorsAndListsTest {
    @Test
    fun `nextFree skips used colours`() {
        val used = RolePalette.colors.take(2)
        assertThat(RolePalette.nextFree(used)).isEqualTo(RolePalette.colors[2])
    }

    @Test
    fun `nextFree wraps when everything is used`() {
        assertThat(RolePalette.nextFree(RolePalette.colors)).isIn(RolePalette.colors)
    }

    @Test
    fun `luminance picks readable text colour`() {
        assertThat(ColorMath.prefersDarkText(0xFFFFFFFF)).isTrue()
        assertThat(ColorMath.prefersDarkText(0xFF000000)).isFalse()
        assertThat(ColorMath.prefersDarkText(0xFF6C63FF)).isFalse()
        assertThat(ColorMath.prefersDarkText(0xFF43D8C9)).isTrue()
        assertThat(ColorMath.luminance(0xFFFFFFFF)).isWithin(1e-6).of(1.0)
    }

    @Test
    fun `move reorders and ignores invalid indices`() {
        val list = listOf("a", "b", "c", "d")
        assertThat(ListOps.move(list, 0, 2)).containsExactly("b", "c", "a", "d").inOrder()
        assertThat(ListOps.move(list, 3, 0)).containsExactly("d", "a", "b", "c").inOrder()
        assertThat(ListOps.move(list, 5, 0)).isSameInstanceAs(list)
        assertThat(ListOps.move(list, 1, 1)).isSameInstanceAs(list)
    }

    @Test
    fun `export paging keeps pages under the limit`() {
        val pages = ExportPaging.split(listOf(100, 200, 300, 400, 50), maxPageHeight = 500)
        assertThat(pages).containsExactly(0..1, 2..2, 3..4).inOrder()
    }

    @Test
    fun `oversized item gets its own page`() {
        val pages = ExportPaging.split(listOf(100, 900, 100), maxPageHeight = 500)
        assertThat(pages).containsExactly(0..0, 1..1, 2..2).inOrder()
    }

    @Test
    fun `export paging of nothing is empty`() {
        assertThat(ExportPaging.split(emptyList(), 100)).isEmpty()
    }

    @Test
    fun `watermark text matches the design document`() {
        assertThat(Watermark.TEXT).isEqualTo("Nội dung hư cấu tạo bởi AU Studio")
    }
}
