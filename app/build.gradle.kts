// AGP 9 note: kotlin-android NOT applied here — AGP 9.4.1 registers the kotlin{} extension
// itself. Applying kotlin-android separately causes "extension already registered" conflict.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.example.mavlinkapplication"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.mavlinkapplication"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }

    packaging {
        jniLibs {
            // libmavsdk_server.so links libc++_shared.so at runtime; legacy packaging
            // extracts the libs to the file system so the dynamic linker can find them.
            useLegacyPackaging = true
        }
    }
}

dependencies {
    implementation(project(":domain"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.activity.compose)

    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)

    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    // MAVSDK + RxJava 2 (MAVSDK-Java uses RxJava 2 throughout)
    implementation(libs.mavsdk)
    implementation(libs.mavsdk.server)
    implementation(libs.rxjava2)
    implementation(libs.rxandroid2)
    implementation(libs.rxkotlin2)
    implementation(libs.kotlinx.coroutines.rx2)
}
