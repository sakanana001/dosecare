package com.picohrt.ui.settings

import androidx.annotation.StringRes
import com.picohrt.R
import com.picohrt.model.settings.AppLanguageOption
import com.picohrt.model.settings.AppLockGracePeriodOption
import com.picohrt.model.settings.DarkModeOption
import com.picohrt.model.settings.FirstDayOfWeekOption
import com.picohrt.model.settings.HrtGoal

@get:StringRes
val DarkModeOption.labelRes: Int
    get() = when (this) {
        DarkModeOption.FOLLOW_SYSTEM -> R.string.dark_mode_follow_system
        DarkModeOption.LIGHT -> R.string.dark_mode_always_off
        DarkModeOption.DARK -> R.string.dark_mode_always_on
    }

@get:StringRes
val AppLanguageOption.labelRes: Int
    get() = when (this) {
        AppLanguageOption.ENGLISH -> R.string.app_language_english
        AppLanguageOption.SIMPLIFIED_CHINESE -> R.string.app_language_simplified_chinese
        AppLanguageOption.TRADITIONAL_CHINESE -> R.string.app_language_traditional_chinese
        AppLanguageOption.CANTONESE -> R.string.app_language_cantonese
    }

@get:StringRes
val FirstDayOfWeekOption.menuLabelRes: Int
    get() = when (this) {
        FirstDayOfWeekOption.FOLLOW_SYSTEM -> R.string.first_day_of_week_follow_system
        FirstDayOfWeekOption.SATURDAY -> R.string.first_day_of_week_saturday
        FirstDayOfWeekOption.SUNDAY -> R.string.first_day_of_week_sunday
        FirstDayOfWeekOption.MONDAY -> R.string.first_day_of_week_monday
    }

@get:StringRes
val AppLockGracePeriodOption.labelRes: Int
    get() = when (this) {
        AppLockGracePeriodOption.IMMEDIATELY -> R.string.app_lock_grace_period_immediately
        AppLockGracePeriodOption.FIFTEEN_SECONDS -> R.string.app_lock_grace_period_15_seconds
        AppLockGracePeriodOption.THIRTY_SECONDS -> R.string.app_lock_grace_period_30_seconds
        AppLockGracePeriodOption.ONE_MINUTE -> R.string.app_lock_grace_period_1_minute
        AppLockGracePeriodOption.TWO_MINUTES -> R.string.app_lock_grace_period_2_minutes
        AppLockGracePeriodOption.FIVE_MINUTES -> R.string.app_lock_grace_period_5_minutes
        AppLockGracePeriodOption.FIFTEEN_MINUTES -> R.string.app_lock_grace_period_15_minutes
        AppLockGracePeriodOption.THIRTY_MINUTES -> R.string.app_lock_grace_period_30_minutes
    }

// F-PR1: HRT goal label for the settings dropdown. UNSET is the
// pre-F-PR1 default ("system" framing — the user hasn't picked) so it
// shows the same neutral label across first-launch and explicit reset.
@get:StringRes
val HrtGoal.menuLabelRes: Int
    get() = when (this) {
        HrtGoal.UNSET -> R.string.hrt_goal_unset
        HrtGoal.MTF -> R.string.hrt_goal_mtf
        HrtGoal.FTM -> R.string.hrt_goal_ftm
        HrtGoal.NON_BINARY -> R.string.hrt_goal_non_binary
    }
