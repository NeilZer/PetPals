import org.gradle.api.JavaVersion

plugins {
    id("com.android.library")
    kotlin("multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.24" // התאימי לגרסת הקוטלין שלך
}

kotlin {
    androidTarget()
    jvmToolchain(17)

    val isMac = System.getProperty("os.name").lowercase().contains("mac")
    if (isMac) {
        iosX64()
        iosArm64()
        iosSimulatorArm64()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
            }
        }
        val androidMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
                // Firebase לאנדרואיד – אם את משתמשת ב־expect/actual ושמה את המימוש באנדרואיד
                implementation("com.google.firebase:firebase-auth-ktx:23.1.0")
                implementation("com.google.firebase:firebase-firestore-ktx:25.1.0")
                implementation("com.google.firebase:firebase-storage-ktx:21.0.0")
            }
        }
        val androidUnitTest by getting
        // val iosMain by getting { }  // יופעל רק אם isMac=true
    }
}

android {
    namespace = "com.petpals.shared" // או com.example.shared – העיקר להיות עקבית עם ה-packages
    compileSdk = 34
    defaultConfig { minSdk = 24 }
}
