plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    kotlin("plugin.serialization") version "1.9.10"
}

val versionName = "0.4.1-beta"

android {
    namespace = "com.xenon.todolist"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.xenon.todolist"
        minSdk = 31
        targetSdk = 35
        versionCode = 7
        versionName = versionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
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
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.core.ktx)
    implementation(libs.accesspoint)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment.ktx)
    implementation(libs.navigation.ui.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.recyclerview.swipedecorator)
    implementation(libs.androidx.recyclerview)
    implementation (libs.play.services.auth)

}

tasks.register("commitAndPushToGH", Exec::class.java) {
    group = "xenon"
    description = "Commits and pushes to github"
    commandLine("git", "add", ".")
    commandLine("git", "commit", "-m", "\"release commit\"")
    commandLine("git", "tag", versionName)
    commandLine("git", "push", "origin")
}