plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    kotlin("plugin.serialization") version "1.9.24"
}

android {
    namespace = "com.xenon.todolist"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.xenon.todolist"
        minSdk = 29
        targetSdk = 34
        versionCode = 5
        versionName = "1.4.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        jvmTarget = "21"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation (libs.dinico414.xenon.commons)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.core.ktx)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment.ktx)
    implementation(libs.navigation.ui.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.core.animation)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.recyclerview.swipedecorator)
    implementation(libs.androidx.recyclerview)
    implementation(libs.play.services.auth)

}
