package com.dosecare.app.ui.journal

import androidx.annotation.StringRes
import com.dosecare.app.R
import com.dosecare.app.model.journal.PrideFlag

/** Accessibility label for a flag chip; chips show no visible name (spec Selection dialog). */
@StringRes
fun prideFlagLabelRes(flag: PrideFlag): Int = when (flag) {
    PrideFlag.TRANSGENDER -> R.string.journal_pride_flag_transgender
    PrideFlag.RAINBOW -> R.string.journal_pride_flag_rainbow
    PrideFlag.BISEXUAL -> R.string.journal_pride_flag_bisexual
    PrideFlag.PANSEXUAL -> R.string.journal_pride_flag_pansexual
    PrideFlag.NONBINARY -> R.string.journal_pride_flag_nonbinary
    PrideFlag.LESBIAN -> R.string.journal_pride_flag_lesbian
    PrideFlag.ASEXUAL -> R.string.journal_pride_flag_asexual
    PrideFlag.GENDERFLUID -> R.string.journal_pride_flag_genderfluid
    PrideFlag.AGENDER -> R.string.journal_pride_flag_agender
}
