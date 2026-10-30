plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "rocks.urzahil.confnav.wear"
    compileSdk { version = release(37) }

    defaultConfig {
        // Must match the phone app: the Data Layer only syncs between apps with the same
        // application ID that are signed with the same key.
        applicationId = "rocks.urzahil.confnav"
        minSdk = 30
        targetSdk = 36
        // Phone versionCode + 1,000,000, so the phone and watch builds never share a code on Play.
        versionCode = 1_000_013
        versionName = "1.11.2"
    }

    val releaseKeystorePath = providers.gradleProperty("releaseKeystorePath").orNull
        ?: providers.environmentVariable("KEYSTORE_PATH").orNull
    val releaseStorePassword = providers.gradleProperty("releaseStorePassword").orNull
        ?: providers.environmentVariable("STORE_PASSWORD").orNull
    val releaseKeyAlias = providers.gradleProperty("releaseKeyAlias").orNull
        ?: providers.environmentVariable("KEY_ALIAS").orNull
        ?: "upload"
    val releaseKeyPassword = providers.gradleProperty("releaseKeyPassword").orNull
        ?: providers.environmentVariable("KEY_PASSWORD").orNull
    val releaseSigningReady = !releaseKeystorePath.isNullOrBlank() &&
            !releaseStorePassword.isNullOrBlank() &&
            !releaseKeyPassword.isNullOrBlank() &&
            file(releaseKeystorePath).exists()

    signingConfigs {
        create("release") {
            if (releaseSigningReady) {
                storeFile = file(requireNotNull(releaseKeystorePath))
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isCrunchPngs = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (releaseSigningReady) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug { }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    dependenciesInfo {
        includeInApk = false
        includeInBundle = true
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.concurrent.futures.ktx)
    implementation(libs.androidx.wear.complications.data.source.ktx)
    implementation(libs.androidx.wear.protolayout)
    implementation(libs.androidx.wear.protolayout.material3)
    implementation(libs.androidx.wear.remote.interactions)
    implementation(libs.androidx.wear.tiles)
    implementation(libs.androidx.wear.tiles.tooling.preview)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    debugImplementation(libs.androidx.wear.tiles.tooling)
}
