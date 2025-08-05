plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}
android {
    namespace = "com.example.petpals"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.petpals"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        // חשוב למפות את מפתח ה־API של המפות
        manifestPlaceholders["MAPS_API_KEY"] = project.findProperty("MAPS_API_KEY")?.toString() ?: ""
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

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":shared"))
    implementation ("androidx.compose.material:material-icons-extended:1.6.8")
    // Jetpack Compose
    implementation("androidx.compose.ui:ui:1.7.0")
    implementation("androidx.compose.material3:material3:1.2.0")
    implementation("androidx.navigation:navigation-compose:2.8.0")
    implementation("androidx.navigation:navigation-runtime-android:2.9.3")
    implementation("androidx.compose.runtime:runtime-livedata:1.8.3")

    // Accompanist
    implementation("com.google.accompanist:accompanist-permissions:0.32.0")

    // Coil
    implementation("io.coil-kt:coil-compose:2.5.0")

    // Google Material
    implementation("com.google.android.material:material:1.11.0")

    // Google Maps + Location
    implementation("com.google.maps.android:maps-compose:4.3.3")
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.2.0")

    // Firebase with BOM
    implementation(platform("com.google.firebase:firebase-bom:33.2.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
}

