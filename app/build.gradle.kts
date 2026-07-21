plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.hilt.android)
}

android {
    namespace = "com.example.aaaaketahuan"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.aaaaketahuan"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("release") {
            storeFile = file(System.getenv("RELEASE_KEYSTORE") ?: rootProject.file("release-keystore.jks"))
            storePassword = System.getenv("STORE_PASSWORD") ?: "android"
            keyAlias = System.getenv("KEY_ALIAS") ?: "release2"
            keyPassword = System.getenv("KEY_PASSWORD") ?: "android"
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    useLibrary("org.apache.http.legacy")

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/DEPENDENCIES"
            excludes += "/META-INF/LICENSE"
            excludes += "/META-INF/LICENSE.txt"
            excludes += "/META-INF/NOTICE"
            excludes += "/META-INF/NOTICE.txt"
            excludes += "/META-INF/INDEX.LIST"
        }
    }

}

// Google HTTP Client membawa httpclient:4.5 sebagai transitive dependency,
// yang kelasnya duplikat dengan org.apache.http.legacy dari Android SDK.
// Exclude global memastikan semua konfigurasi mewarisi pengecualian ini.
configurations.all {
    exclude(group = "org.apache.httpcomponents")
}

dependencies {
    // Compose BOM
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.compose.preview)
    debugImplementation(libs.compose.ui.tooling)

    // Activity & Core
    implementation(libs.activity.compose)
    implementation(libs.core.ktx)

    // Lifecycle
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)

    // Navigation
    implementation(libs.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Gson
    implementation(libs.gson)

    // MPAndroidChart
    implementation(libs.mpandroidchart)

    // Coroutines
    implementation(libs.coroutines.android)

    // Google Sheets API
    implementation(libs.google.api.client.android) {
        exclude(group = "org.apache.httpcomponents")
    }
    implementation(libs.google.api.services.sheets) {
        exclude(group = "org.apache.httpcomponents")
    }
    implementation(libs.google.auth.oauth2)
    implementation(libs.google.http.client.gson)

    // Google Sign-In (OAuth 2.0 for users)
    implementation(libs.play.services.auth)

    // ─── Testing ──────────────────────────────────────────────────────
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.turbine)
    testImplementation(libs.mockk)
}

kapt {
    correctErrorTypes = true
}
