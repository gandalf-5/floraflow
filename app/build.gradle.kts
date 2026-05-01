import java.util.Base64

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
}

android {
    namespace = "com.floraflow.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "ca.floraflow.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        // Production backend URL — not a secret (OpenAI key stays on the server).
        buildConfigField(
            "String",
            "FLORA_FLOW_API_URL",
            "\"https://flora-explorer.replit.app/api/\""
        )
        // PlantNet API key — injected from PLANTNET_API_KEY env var / GitHub secret.
        // Falls back to the public demo key so local builds and CI always compile.
        buildConfigField(
            "String",
            "PLANTNET_API_KEY",
            "\"${System.getenv("PLANTNET_API_KEY") ?: "2b10oiLgd0yalCBVTL5Rrq1Ee"}\""
        )
    }

    // Release signing — reads from GitHub Actions secrets via env vars.
    // If KEYSTORE_BASE64 is not set (local dev), release uses debug signing.
    val keystoreB64 = System.getenv("KEYSTORE_BASE64") ?: ""
    val releaseSigningConfig = if (keystoreB64.isNotBlank()) {
        val keystoreFile = file("${buildDir}/floraflow-release.p12")
        keystoreFile.parentFile.mkdirs()
        keystoreFile.writeBytes(Base64.getDecoder().decode(keystoreB64))
        signingConfigs.create("release") {
            storeFile = keystoreFile
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
            keyAlias = System.getenv("KEY_ALIAS") ?: ""
            keyPassword = System.getenv("KEY_PASSWORD") ?: ""
            storeType = "PKCS12"
        }
    } else null

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (releaseSigningConfig != null) {
                signingConfig = releaseSigningConfig
            }
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
    implementation(libs.play.review)
    implementation("com.android.billingclient:billing-ktx:6.2.1")
}
