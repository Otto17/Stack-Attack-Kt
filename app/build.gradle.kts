plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.stackattackkt"
    compileSdk = 37 // SDK при сборке использован для Android 17 (Cinnamon Bun)

    defaultConfig {
        applicationId = "com.example.stackattackkt"
        minSdk = 23     // Минимум Android 6.0 (Marshmallow)
        targetSdk = 36  // Оптимизировано под Android 16 (Baklava)
        versionCode = 2
        versionName = "14.06.26"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Передача versionName в ресурсы
        resValue("string", "app_version_dynamic", "ver $versionName")
    }

    buildFeatures {
        viewBinding = true
        resValues = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}