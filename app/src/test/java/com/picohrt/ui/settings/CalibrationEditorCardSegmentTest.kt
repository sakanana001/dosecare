package com.picohrt.ui.settings

import androidx.compose.material3.Text
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.createComposeRule
import com.picohrt.ui.calibration.CalibrationEditorCard
import com.picohrt.ui.components.HrtSection
import com.picohrt.ui.components.SegmentPosition
import com.picohrt.ui.components.SegmentPositionSemanticsKey
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34])
class CalibrationEditorCardSegmentTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun editorCards_inSection_inheritPositions() {
        composeRule.setContent {
            HrtSection(title = null) {
                item { CalibrationEditorCard { Text("a") } }
                item { CalibrationEditorCard { Text("b") } }
                item { CalibrationEditorCard { Text("c") } }
            }
        }
        listOf(
            SegmentPosition(0, 3),
            SegmentPosition(1, 3),
            SegmentPosition(2, 3),
        ).forEach {
            composeRule.onNode(SemanticsMatcher.expectValue(SegmentPositionSemanticsKey, it))
                .assertExists()
        }
    }
}
