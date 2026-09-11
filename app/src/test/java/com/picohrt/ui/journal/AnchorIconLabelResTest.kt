package com.picohrt.ui.journal

import com.picohrt.R
import com.picohrt.model.journal.AnchorIcon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AnchorIconLabelResTest {
    @Test
    fun everyIconHasALabel() {
        AnchorIcon.entries.forEach { icon ->
            assertNotEquals("No label for $icon", 0, anchorIconLabelRes(icon))
        }
    }

    @Test
    fun favoriteMapsToPersonalLabel() {
        assertEquals(R.string.journal_anchor_icon_favorite, anchorIconLabelRes(AnchorIcon.FAVORITE))
    }
}
