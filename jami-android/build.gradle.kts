plugins {
    id("com.google.devtools.ksp") version "1.9.23-1.0.19" apply false
    id("co.uzzu.dotenv.gradle") version "4.0.0"
}

buildscript {
    repositories {
        google()
        maven { url = uri( "https://maven.google.com") }
        mavenCentral()
    }

    val kotlin_version by extra { "1.9.23" }
    val hilt_version by extra { "2.52" }

    dependencies {
        classpath ("com.android.tools.build:gradle:8.5.2")
        classpath ("com.google.gms:google-services:4.4.2")
        classpath ("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlin_version")
        classpath ("com.google.dagger:hilt-android-gradle-plugin:$hilt_version")
    }
}
allprojects {
    repositories {
        google()
        maven { url = uri("https://maven.google.com") }
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
