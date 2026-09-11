package com.picohrt.ui.journal

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.picohrt.R
import com.picohrt.model.journal.AnchorIcon
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AnchorIconResTest {
    @Test
    fun everyAnchorIcon_resolvesToADrawable() {
        AnchorIcon.entries.forEach { icon ->
            assertNotEquals("No drawable for $icon", 0, anchorIconRes(icon))
        }
    }

    @Test
    fun favoriteResolvesToHeartDrawable() {
        assertEquals(R.drawable.ic_favorite, anchorIconRes(AnchorIcon.FAVORITE))
    }
}
