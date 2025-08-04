plugins {
    id("com.android.library")
    kotlin("multiplatform")
}

kotlin {
    androidTarget() // מאפשר גרסת JVM לאנדרואיד
    jvmToolchain(17)

    sourceSets {
        val commonMain by getting {
            dependencies {
                // מאפשר שימוש ב-Flow ו-StateFlow בקוד משותף
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
            }
        }
        val commonTest by getting

        val androidMain by getting {
            dependencies {
                // מוסיף תמיכה בקורוטינות לאנדרואיד
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
            }
        }
        val androidUnitTest by getting
    }
}

android {
    namespace = "com.example.shared"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
    }
}
