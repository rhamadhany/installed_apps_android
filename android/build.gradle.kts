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

    kotlinOptions {
        jvmTarget = "11"
    }

    sourceSets {
        getByName("main") {
            java.srcDirs("src/main/kotlin")
        }
    }

    defaultConfig {
        minSdk = 21
    }

    val flutter = Properties().let { propeties ->
        file("local.properties").inputStream().use {
            propeties.load(it).run {
                propeties.getProperty("flutter.sdk") + "/bin/cache/artifacts/engine/android-arm64/flutter.jar"
            }
        }
    }

    dependencies {
        compileOnly(files(flutter))
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    }

}
