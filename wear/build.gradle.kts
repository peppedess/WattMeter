import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val runNumber = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()

android {
    namespace = "com.peppedess.wattmeter.wear"
    compileSdk = 36

    defaultConfig {
        // Deve coincidere ESATTAMENTE con quello del telefono: il Data Layer
        // riconosce telefono e orologio come la stessa app solo se applicationId
        // e certificato di firma sono identici. Il namespace resta distinto,
        // e riguarda solo il codice sorgente, non l'installazione.
        applicationId = "com.peppedess.wattmeter"
        minSdk = 30
        targetSdk = 36
        versionCode = runNumber
        versionName = "1.0.$runNumber"
    }

    signingConfigs {
        create("release") {
            // Stesso certificato del telefono: nessun file aggiuntivo nel repo.
            storeFile = file("../app/wattmeter.jks")
            storePassword = "wattmeter2026"
            keyAlias = "wattmeter"
            keyPassword = "wattmeter2026"
            storeType = "PKCS12"
            enableV1Signing = true
            enableV2Signing = true
            enableV3Signing = true
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("release")
        }
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.compose.foundation)

    implementation(libs.play.services.wearable)
    implementation(libs.androidx.wear.ongoing)
}
