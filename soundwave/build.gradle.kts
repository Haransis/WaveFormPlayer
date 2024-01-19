plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

// set a specific Java version
kotlin {
    jvmToolchain(17)
}

android {
    namespace = "fr.haran.soundwave"
    compileSdk = 33

    defaultConfig {
        minSdk = 23
        targetSdk = 33
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("debug") {
            // allow debugging with a proxy
            manifestPlaceholders["usesCleartextTraffic"] = "true"
        }
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles (
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}

dependencies {
    // ---kotlin--- //
    //implementation(libs.kotlin.reflect)
    //implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version")

    // ---android--- //
    implementation(libs.androidx.appCompat)
    implementation(libs.androidx.coreKtx)
    coreLibraryDesugaring(libs.bundles.desugar)

    // ---data layer--- //
    implementation(libs.bundles.coroutines)

    // ---ui layer--- //
    implementation(libs.androidx.constraintLayout)
    implementation(libs.material)

    // ---development utils--- //
    debugImplementation(libs.leakCanary)
    implementation(libs.timber)

    // ---testing dependencies--- //
    androidTestImplementation(libs.androidx.testEspresso)
    androidTestImplementation(libs.androidx.testJUnit)
    testImplementation(libs.jUnit)
}