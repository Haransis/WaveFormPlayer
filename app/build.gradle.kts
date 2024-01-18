plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

// set a specific Java version
kotlin {
    jvmToolchain(17)
}

android {
    namespace = "fr.haran.example"
    compileSdk = 33

    defaultConfig {
        minSdk = 23
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"
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

    // ---ui layer--- //
    implementation(libs.androidx.constraintLayout)
    implementation(libs.material)
    implementation(project(":soundwave"))

    // ---development utils--- //
    debugImplementation(libs.leakCanary)
    implementation(libs.timber)

    // ---testing dependencies--- //
    androidTestImplementation(libs.androidx.testEspresso)
    androidTestImplementation(libs.androidx.testJUnit)
    testImplementation(libs.jUnit)
}