plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
}

android {
    namespace = "com.floraflow.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.floraflow.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        // URL of the FloraFlow backend API server (set via FLORA_FLOW_API_URL env var / GitHub secret).
        // Must end with /api/ — e.g. "https://your-api-server.replit.app/api/"
        buildConfigField(
            "String",
            "FLORA_FLOW_API_URL",
            "\"${System.getenv("FLORA_FLOW_API_URL") ?: "https://placeholder.example.com/api/"}\""
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/versions/9/previous-compilation-data.bin"
            excludes += "META-INF/*.kotlin_module"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.livedata)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui)
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)
    implementation(libs.glide)
    kapt(libs.glide.compiler)
    implementation(libs.workmanager)
    implementation(libs.datastore.preferences)
    implementation(libs.coroutines.android)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    kapt(libs.room.compiler)
    implementation("androidx.palette:palette-ktx:1.0.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
}
