package com.dosecare.app.ui.journal

import com.dosecare.app.R
import com.dosecare.app.model.journal.AnchorIcon
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
