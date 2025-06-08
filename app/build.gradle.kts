plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
    // Removed kotlin-kapt since we don't need Room anymore
}

android {
    namespace = "vcmsa.projects.prog3c"
    compileSdk = 35

    defaultConfig {
        applicationId = "vcmsa.projects.prog3c"
        minSdk = 32
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Modern UI Layout Support
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")

    // Lifecycle components for coroutines and ViewModels
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")

    // RecyclerView (used throughout your app)
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // Fragment support
    implementation("androidx.fragment:fragment-ktx:1.6.2")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Firebase (your main database now)
    implementation(platform("com.google.firebase:firebase-bom:32.6.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")

    // Color Picker library
    implementation("com.github.yukuku:ambilwarna:2.0.1")

    // Chart library for analytics graphs
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // Minimal Room dependencies just to avoid import errors (not actually used)
    compileOnly("androidx.room:room-common:2.5.0")
    compileOnly("androidx.room:room-runtime:2.5.0")
}