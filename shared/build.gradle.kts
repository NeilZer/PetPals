plugins {
    id("com.android.library")
    kotlin("multiplatform")
    kotlin("plugin.serialization") version "1.9.24" // ← החזרת הגרסה כאן
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

                // Ktor (משותף)
                implementation("io.ktor:ktor-client-core:2.3.12")
                implementation("io.ktor:ktor-client-content-negotiation:2.3.12")
                implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")
                implementation("io.ktor:ktor-client-logging:2.3.12")
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
                implementation("io.ktor:ktor-client-okhttp:2.3.12")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")
                implementation("com.google.android.gms:play-services-location:21.2.0")
                // אם מימושי actual באנדרואיד משתמשים בפיירבייס:
                implementation("com.google.firebase:firebase-auth-ktx:23.1.0")
                implementation("com.google.firebase:firebase-firestore-ktx:25.1.0")
                implementation("com.google.firebase:firebase-storage-ktx:21.0.0")
            }
        }
        val androidUnitTest by getting

        if (isMac) {
            val iosMain by getting {
                dependencies {
                    implementation("io.ktor:ktor-client-darwin:2.3.12")
                }
            }
            val iosTest by getting
        }
    }
}

android {
    namespace = "com.petpals.shared"
    compileSdk = 34
    defaultConfig { minSdk = 24 }
}
dependencies {
    implementation("com.google.android.gms:play-services-location:21.3.0")
}
