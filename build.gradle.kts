plugins {
    id("com.android.library") version "9.3.2"
}

version = "2.1.0"

android {
    namespace = "com.imax.stompwebsocket"
    compileSdk = 37

    defaultConfig {
        minSdk = 21

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    // OkHttp (version 5.0.0-alpha.14)
    implementation("com.squareup.okhttp3:okhttp:5.5.0")

    // Kotlin Coroutines Core (latest stable)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")

    // AndroidX Annotation
    implementation("androidx.annotation:annotation:1.10.0")
}
