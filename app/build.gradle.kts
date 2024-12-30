plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "offgrid.geogram"
    compileSdk = 34

    defaultConfig {
        applicationId = "offgrid.geogram"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        applicationIdSuffix = "geogram"
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

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.play.services.nearby)
    implementation(libs.spark.core)
    implementation(libs.car.ui.lib)
    implementation(libs.gson)
    testImplementation(libs.junit)
    testImplementation(libs.junit.jupiter)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // Add the nostr-java-api library
    //implementation("com.github.tcheeric:nostr-java:main-SNAPSHOT")
}
