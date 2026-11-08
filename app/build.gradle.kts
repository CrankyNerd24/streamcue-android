import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

// TMDB v4 read token, kept out of source control the same way the iOS
// app's Secrets.swift is — see local.properties.example.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

android {
    namespace = "com.streamcueapp.streamcue"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.streamcueapp.streamcue"
        minSdk = 31
        // Android 16. Compiled against 17 for the newest Compose libraries.
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        buildConfigField(
            "String",
            "TMDB_TOKEN",
            "\"${localProperties.getProperty("TMDB_TOKEN", "")}\""
        )
        buildConfigField(
            "String",
            "OMDB_KEY",
            "\"${localProperties.getProperty("OMDB_KEY", "")}\""
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.retrofit.core)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp.logging.interceptor)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    implementation(libs.billing.ktx)
    implementation(libs.review.ktx)

    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(libs.junit)
}
