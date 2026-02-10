plugins {
    alias(libs.plugins.android.application)
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("androidx.navigation.safeargs.kotlin")
}

android {
    namespace = "com.example.zaliczenie_projekt_01"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.zaliczenie_projekt_01"
        minSdk = 31
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // Podstawowe (z szablonu)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)

    // --- MOJE DODATKI (TEGO BRAKOWAŁO) ---

    // 1. Mapy (OSMDroid)
    implementation("org.osmdroid:osmdroid-android:6.1.18")

    // 2. Baza danych (Room)
    val room_version = "2.8.4"
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    kapt("androidx.room:room-compiler:$room_version")

    // 3. Zdjęcia (Glide)
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // 4. GPS
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // 5. Gson (do zapisu)
    implementation("com.google.code.gson:gson:2.10.1")

    // Testy
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}