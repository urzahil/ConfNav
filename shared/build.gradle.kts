plugins {
    alias(libs.plugins.android.library)
}

// Code shared by the phone app and the Wear OS app: the agenda sync contract and the
// "what's on next" timeline the watch tile and complication are built from.
android {
    namespace = "rocks.urzahil.confnav.shared"
    compileSdk { version = release(37) }

    defaultConfig {
        // Uses java.time, which the phone app provides on API 24-25 through core library desugaring.
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    api(libs.play.services.wearable)
    testImplementation(libs.junit)
}
