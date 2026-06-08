import java.util.Properties

group = "com.BNeoTech.installed_apps_android"
version = "1.0-SNAPSHOT"


allprojects {
    repositories {
        google()
        mavenCentral()
    }
}


plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.BNeoTech.installed_apps_android"

    compileSdk = 36

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlin {
        compilerOptions {
            jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11
        }
    }

    sourceSets {
        getByName("main") {
            java.srcDirs("src/main/kotlin")
        }
    }

    defaultConfig {
        minSdk = 21
    }

    val flutter: String by lazy {
        val flutterRoot = rootProject.findProperty("flutter.sdk") as? String
            ?: System.getenv("FLUTTER_ROOT")
            ?: System.getenv("FLUTTER_HOME")
            ?: error("flutter.sdk not found (set FLUTTER_ROOT or gradle property)")
        "$flutterRoot/bin/cache/artifacts/engine/android-arm64/flutter.jar"
    }

    dependencies {
        compileOnly(files(flutter))
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
        implementation("androidx.core:core-ktx:1.15.0")
    }

}
