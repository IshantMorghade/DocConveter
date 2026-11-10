plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.example.docconveter"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.docconveter"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    buildFeatures {
        compose = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)

    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))

    // Jetpack Compose UI
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")

    // Material Design 3
    implementation("androidx.compose.material3:material3")

    // Compose Preview
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.compose.material:material-icons-extended")

    implementation("com.tom-roush:pdfbox-android:2.0.27.0")

    // Advanced PDF Compression API
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}