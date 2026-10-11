package com.dosecare.app.ui.settings

import androidx.compose.ui.text.input.ImeAction
import com.dosecare.app.ui.calibration.calibrationEditorAnalyteImeAction
import org.junit.Assert.assertEquals
import org.junit.Test

class CalibrationEditorComponentsTest {
    @Test
    fun calibrationEditorAnalyteImeAction_returnsNextForNonLastAnalytes() {
        assertEquals(ImeAction.Next, calibrationEditorAnalyteImeAction(index = 0, count = 3))
        assertEquals(ImeAction.Next, calibrationEditorAnalyteImeAction(index = 1, count = 3))
    }

    @Test
    fun calibrationEditorAnalyteImeAction_returnsDoneForLastAnalyte() {
        assertEquals(ImeAction.Done, calibrationEditorAnalyteImeAction(index = 2, count = 3))
        assertEquals(ImeAction.Done, calibrationEditorAnalyteImeAction(index = 0, count = 1))
    }
}
