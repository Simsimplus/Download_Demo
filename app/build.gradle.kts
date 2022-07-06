import de.fayard.refreshVersions.core.versionFor

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.diffplug.spotless") version "6.8.0"
}
android {
    compileSdk = 32

    defaultConfig {
        applicationId = "io.simsim.download.demo"
        minSdk = 23
        targetSdk = 32
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            manifestPlaceholders["appName"] = "下载demo"
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            applicationIdSuffix = ".release"
            versionNameSuffix = ".release"
        }
        debug {
            manifestPlaceholders["appName"] = "下载demo.debug"
            applicationIdSuffix = ".debug"
            versionNameSuffix = ".debug"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = listOf("-Xopt-in=kotlin.RequiresOptIn")
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = versionFor(AndroidX.compose.ui)
    }
    packagingOptions {
        resources {
            excludes.add("/META-INF/{AL2.0,LGPL2.1}")
        }
    }
//    applicationVariants.all { variant ->
//        variant.outputs.all {
//            outputFileName = "下载demo_${variant.name}_${new Date().format("yyyyMMdd")}.apk"
//            println(outputFileName)
//        }
//    }
}

dependencies {

    implementation(AndroidX.core.ktx)
    implementation(AndroidX.compose.ui)
    implementation(AndroidX.compose.ui.toolingPreview)
    implementation(AndroidX.activity.compose)
    implementation(AndroidX.compose.material3)
    implementation(AndroidX.lifecycle.runtimeKtx)
    implementation(AndroidX.lifecycle.process)
    implementation(Splitties.pack.androidMdcWithViewsDsl)
    implementation(AndroidX.compose.material.icons.extended)
    testImplementation(Testing.junit4)
    androidTestImplementation(AndroidX.test.ext.junit)
    androidTestImplementation(AndroidX.test.espresso.core)
    androidTestImplementation(AndroidX.compose.ui.testJunit4)
    debugImplementation(AndroidX.compose.ui.tooling)
    debugImplementation(AndroidX.compose.ui.testManifest)
    debugImplementation(Splitties.stethoInit)
    // fdm
    implementation("com.liulishuo.filedownloader:library:1.7.7")
}

spotless {
    kotlin {
        target("**/*.kt")
        targetExclude("src/androidTest/**/*.kt")
        ktlint()
            .editorConfigOverride(mapOf("disabled_rules" to "no-wildcard-imports,filename"))
    }
    kotlinGradle {
        target("*.gradle.kts") // default target for kotlinGradle
        ktlint() // or ktfmt() or prettier()
    }
}