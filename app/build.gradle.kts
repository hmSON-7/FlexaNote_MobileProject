plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "com.cookandroid.project_flexanote"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.cookandroid.project_flexanote"
        minSdk = 24
        targetSdk = 29
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.activity:activity:1.8.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // AndroidPdfViewer 추가
    implementation("com.github.barteksc:android-pdf-viewer:3.2.0-beta.3")
}
