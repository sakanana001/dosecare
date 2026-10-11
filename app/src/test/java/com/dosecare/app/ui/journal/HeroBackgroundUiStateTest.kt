package com.dosecare.app.ui.journal

import com.dosecare.app.model.journal.AnchorIcon
import com.dosecare.app.model.journal.HeroBackground
import com.dosecare.app.model.journal.PrideFlag
import com.dosecare.app.model.journal.TrackedDate
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class HeroBackgroundUiStateTest {
    private fun trackedDate(heroBackground: HeroBackground) = TrackedDate(
        id = "id1",
        name = "Started E",
        icon = AnchorIcon.MEDICATION,
        date = LocalDate.parse("2024-01-01"),
        palette = null,
        heroBackground = heroBackground,
        pinnedOrder = 0,
    )

    @Test
    fun toAnchorRowUiState_propagatesHeroBackground() {
        val row = trackedDate(HeroBackground.Flag(PrideFlag.RAINBOW))
            .toAnchorRowUiState(LocalDate.parse("2024-06-01"))
        assertEquals(HeroBackground.Flag(PrideFlag.RAINBOW), row.heroBackground)
    }

    @Test
    fun toAnchorRowUiState_propagatesDateColorDefault() {
        val row = trackedDate(HeroBackground.DateColor)
            .toAnchorRowUiState(LocalDate.parse("2024-06-01"))
        assertEquals(HeroBackground.DateColor, row.heroBackground)
    }

    @Test
    fun toAnchorRowUiState_propagatesExplicitNone() {
        val row = trackedDate(HeroBackground.None)
            .toAnchorRowUiState(LocalDate.parse("2024-06-01"))
        assertEquals(HeroBackground.None, row.heroBackground)
    }
}
