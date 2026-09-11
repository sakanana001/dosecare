package com.picohrt.ui.components

import androidx.annotation.StringRes
import com.picohrt.R
import com.picohrt.model.personalization.WeightUnit

@get:StringRes
val WeightUnit.shortLabelRes: Int
    get() = when (this) {
        WeightUnit.KILOGRAMS -> R.string.personalization_weight_unit_kg_short
        WeightUnit.POUNDS -> R.string.personalization_weight_unit_lb_short
    }
