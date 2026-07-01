import java.time.LocalDate
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("kotlin-parcelize")
}

android {
    namespace = "com.kylecorry.trail_sense.plugin.aurora"
    compileSdk = 37

    defaultConfig {
        vectorDrawables.useSupportLibrary = true
        applicationId = "com.kylecorry.trail_sense.plugin.aurora"
        minSdk = 23
        targetSdk = 37
        versionCode = 2
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    androidResources {
        // Support for auto-generated locales for per-app language settings
        generateLocaleConfig = true
    }
    buildFeatures {
        buildConfig = true
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            applicationIdSuffix = ".release"
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        create("nightly") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".nightly"
            versionNameSuffix = "-nightly-${LocalDate.now()}"
        }
    }
    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    packaging {
        resources.merges += "META-INF/LICENSE.md"
        resources.merges += "META-INF/LICENSE-notice.md"
        jniLibs {
            useLegacyPackaging = true
        }
    }
    lint {
        abortOnError = false
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.material)
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Sol
    implementation(libs.sol)

    // Andromeda
    implementation(libs.andromeda.core)
    implementation(libs.andromeda.files)
    implementation(libs.andromeda.fragments)
    implementation(libs.andromeda.ipc)
    implementation(libs.andromeda.json)
    implementation(libs.andromeda.net)
    implementation(libs.andromeda.permissions)
    implementation(libs.gson)

    // Luna
    implementation(libs.luna)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    testImplementation(libs.junit.platform.runner)
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.junit.jupiter.params)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.mockito.kotlin)
}
