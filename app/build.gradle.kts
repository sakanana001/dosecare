plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    // v0.7 鏁版嵁搴撴爤
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.dosecare.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.dosecare.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.4.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Coroutines + Serialization
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    // v0.7 鏁版嵁搴撴爤: Hilt + KSP + Room + SQLCipher
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.sqlite)
    implementation(libs.androidx.sqlite.ktx)
    implementation(libs.sqlcipher.android)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // 鎻愰啋閫氱煡
    implementation(libs.androidx.work.runtime.ktx)

    // SQLCipher passphrase 鍔犲瘑瀛樺偍
    implementation(libs.androidx.security.crypto)

    // JVM Unit Test (DrugCatalogLoader 楠岃瘉 - 璇诲彇 test resources/drugs/v0.6.json)
    // v0.7 璐＄尞: in-memory Room 娴嬭瘯 deferred v0.8 (pre-built db.crypt asset)
    testImplementation("junit:junit:4.13.2")
}




