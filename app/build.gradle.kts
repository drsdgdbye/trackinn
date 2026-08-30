plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "pro.drsdgdbye.trackinn"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "pro.drsdgdbye.trackinn"
        minSdk = 28
        targetSdk = 37
        versionCode = 4
        versionName = "1.2.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val ksPassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
            val kPassword = System.getenv("KEY_PASSWORD") ?: ""
            val ksFile = rootProject.file("trackinn.jks")

            if (ksPassword.isNotEmpty() && ksFile.exists()) {
                storeFile = ksFile
                storePassword = ksPassword
                keyAlias = System.getenv("KEY_ALIAS") ?: "trackinn"
                keyPassword = kPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // Splash Screen
    implementation(libs.androidx.core.splashscreen)

    // Gson for JSON export/import
    implementation(libs.gson)

    // Reorderable for drag-and-drop
    implementation(libs.sh.calvin.reorderable)

    // Vico charts
    implementation(libs.vico.compose.m3)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.room.testing)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    // Debug-инструмент android-bridge: подключается только когда AAR лежит локально
    // (app/libs/app-embedded-bridge-debug.aar). На GitHub/CI файла нет — зависимость не добавляется.
    if (file("libs/app-embedded-bridge-debug.aar").exists()) {
        debugImplementation(files("libs/app-embedded-bridge-debug.aar"))
    }
}
