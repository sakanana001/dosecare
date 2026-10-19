package com.dosecare.app.model.settings

import com.dosecare.app.model.bloodtest.BloodAnalyteKey
import com.dosecare.app.model.bloodtest.BloodTestCatalog
import com.dosecare.app.model.bloodtest.BloodUnitKey
import com.dosecare.app.model.pk.HomeE2ChartWindowOption
import java.time.DayOfWeek
import java.time.temporal.WeekFields
import java.util.Locale

enum class DarkModeOption {
    FOLLOW_SYSTEM,
    LIGHT,
    DARK;

    fun resolveDarkTheme(isSystemInDarkTheme: Boolean): Boolean = when (this) {
        FOLLOW_SYSTEM -> isSystemInDarkTheme
        LIGHT -> false
        DARK -> true
    }

    companion object {
        fun fromStorageValue(value: String?): DarkModeOption {
            return entries.firstOrNull { it.name == value } ?: FOLLOW_SYSTEM
        }
    }
}

enum class AppLanguageOption(val languageTag: String) {
    ENGLISH(languageTag = "en"),
    SIMPLIFIED_CHINESE(languageTag = "zh-Hans"),
    TRADITIONAL_CHINESE(languageTag = "zh-Hant"),
    CANTONESE(languageTag = "yue-Hant-HK"),
    JAPANESE(languageTag = "ja");

    companion object {
        fun fromLocale(locale: Locale): AppLanguageOption {
            return when {
                locale.language == "yue" -> CANTONESE
                locale.language == "ja" -> JAPANESE
                locale.language != "zh" -> ENGLISH
                locale.script == "Hans" -> SIMPLIFIED_CHINESE
                locale.script == "Hant" ||
                    locale.country == "HK" ||
                    locale.country == "MO" ||
                    locale.country == "TW" ->
                    TRADITIONAL_CHINESE
                else -> SIMPLIFIED_CHINESE
            }
        }
    }
}

enum class FirstDayOfWeekOption {
    FOLLOW_SYSTEM,
    SATURDAY,
    SUNDAY,
    MONDAY;

    fun resolve(systemLocale: Locale): DayOfWeek = when (this) {
        FOLLOW_SYSTEM -> WeekFields.of(systemLocale).firstDayOfWeek
        SATURDAY -> DayOfWeek.SATURDAY
        SUNDAY -> DayOfWeek.SUNDAY
        MONDAY -> DayOfWeek.MONDAY
    }

    companion object {
        fun fromStorageValue(value: String?): FirstDayOfWeekOption {
            return entries.firstOrNull { it.name == value } ?: FOLLOW_SYSTEM
        }
    }
}

enum class AppLockGracePeriodOption(val durationMillis: Long) {
    IMMEDIATELY(durationMillis = 0L),
    FIFTEEN_SECONDS(durationMillis = 15_000L),
    THIRTY_SECONDS(durationMillis = 30_000L),
    ONE_MINUTE(durationMillis = 60_000L),
    TWO_MINUTES(durationMillis = 2 * 60_000L),
    FIVE_MINUTES(durationMillis = 5 * 60_000L),
    FIFTEEN_MINUTES(durationMillis = 15 * 60_000L),
    THIRTY_MINUTES(durationMillis = 30 * 60_000L);

    fun shouldRelock(elapsedSinceLeavingMillis: Long): Boolean {
        return durationMillis == 0L || elapsedSinceLeavingMillis >= durationMillis
    }

    companion object {
        fun fromStorageValue(value: String?): AppLockGracePeriodOption {
            return entries.firstOrNull { it.name == value } ?: ONE_MINUTE
        }
    }
}

// F-PR1: HRT goal — drives which hormone the home chart defaults to
// (MtF → estradiol trend, FtM → testosterone trend) and gates clinical
// safety prompts (e.g. cyproterone acetate is contraindicated for FtM).
// MTF is the existing default; UNSET covers fresh installs and users who
// haven't picked one yet (the chart defaults to E2 in that case so the
// pre-FtM behavior is preserved for every existing user).
enum class HrtGoal {
    UNSET,
    MTF,
    FTM,
    NON_BINARY;

    companion object {
        fun fromStorageValue(value: String?): HrtGoal {
            return entries.firstOrNull { it.name == value } ?: UNSET
        }
    }
}

data class SettingsState(
    val darkModeOption: DarkModeOption = DarkModeOption.FOLLOW_SYSTEM,
    val adaptiveColorEnabled: Boolean = true,
    val pureBlackEnabled: Boolean = false,
    val cjkTextOffsetEnabled: Boolean = false,
    val hazeBlurEnabled: Boolean = true,
    val appLanguageOption: AppLanguageOption = AppLanguageOption.ENGLISH,
    val firstDayOfWeekOption: FirstDayOfWeekOption = FirstDayOfWeekOption.FOLLOW_SYSTEM,
    val calibrationDefaultUnits: Map<BloodAnalyteKey, BloodUnitKey> = emptyMap(),
    val homeE2DisplayUnit: BloodUnitKey = BloodTestCatalog.canonicalUnitFor(BloodAnalyteKey.E2),
    val homeE2ChartWindowOption: HomeE2ChartWindowOption = HomeE2ChartWindowOption.SEVEN_DAYS,
    val remindersEnabled: Boolean = true,
    val showArchivedGroupRecords: Boolean = true,
    val hideReferenceRanges: Boolean = false,
    val screenLockProtectionEnabled: Boolean = false,
    val appLockGracePeriodOption: AppLockGracePeriodOption =
        AppLockGracePeriodOption.ONE_MINUTE,
    val hideScreenContentEnabled: Boolean = false,
    val lastSeenTimeZoneId: String? = null,
    val hideMedicationDetails: Boolean = false,
    val groupNameCounter: Int = 0,
    // F-PR1: which HRT regimen the user is on. Drives the home chart's
    // default hormone, gates FtM-only safety prompts (CPA, finasteride),
    // and lets the F-PR3 work switch the default curve. UNSET keeps the
    // pre-F-PR1 behavior (E2 default, no safety gating) for users who
    // haven't picked a goal.
    val hrtGoal: HrtGoal = HrtGoal.UNSET,
    // PR-user-override: per-user parameter overrides for the T PK
    // engine. Null means "use the catalog default" (recommended for
    // most users — the catalog values are calibrated to Nebido /
    // AndroGel SPC data and the population mean). Non-null values
    // override kClearInjection and / or threeCompK31 in the engine.
    // Currently only the 3-comp RK4 path (testosterone undecanoate
    // / Nebido) consumes these overrides; other T esters (TE / TC)
    // and the gel path ignore them.
    val pkClearInjectionOverride: Double? = null,
    val pkK31Override: Double? = null,
)

fun SettingsState.calibrationDefaultUnitFor(analyteKey: BloodAnalyteKey): BloodUnitKey {
    return calibrationDefaultUnits[analyteKey]
        ?.takeIf { unit -> BloodTestCatalog.isUnitAllowed(analyteKey, unit) }
        ?: BloodTestCatalog.canonicalUnitFor(analyteKey)
}
