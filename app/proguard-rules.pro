# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# kotlinx-serialization-json specific. Add this if you have java.lang.NoClassDefFoundError kotlinx.serialization.json.JsonObjectMapper
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Application classes that will be serialized/deserialized over Gson
-keep class com.dosecare.app.** { *; }

# v0.8d R8 修复: Tink 加密库 (来自 androidx.security.crypto 传递依赖) 引用的
# errorprone 注解只在编译期存在,运行时无,加 -dontwarn 让 R8 跳过
# 参考 app/build/outputs/mapping/release/missing_rules.txt
-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue
-dontwarn com.google.errorprone.annotations.CheckReturnValue
-dontwarn com.google.errorprone.annotations.Immutable
-dontwarn com.google.errorprone.annotations.RestrictedApi

# Hilt / Dagger: 生成代码需要 keep
-keep class dagger.hilt.** { *; }
-keep class hilt_aggregated_deps.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.* { *; }

# Room: 生成 *_Impl 类需要 keep
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class **_Impl { *; }
