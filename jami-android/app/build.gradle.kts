import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

val kotlin_version: String by rootProject.extra
val hilt_version: String by rootProject.extra
val dokka_version: String by rootProject.extra
val buildFirebase = project.hasProperty("buildFirebase") || gradle.startParameter.taskRequests.toString().contains("Firebase")

plugins {
    id("com.android.application")
    kotlin("android")
    id("dagger.hilt.android.plugin")
    id("com.google.protobuf") version "0.9.4"
    id("com.google.devtools.ksp")
}

android {
    namespace = "cx.ring"
    compileSdk = 34
    buildToolsVersion = "34.0.0"
    ndkVersion = "26.3.11579264"
    defaultConfig {
        minSdk = 24
        targetSdk = 34
        // upstream version, patchlevel (last 3 digits)
        versionCode = 436005
        versionName = "20241126-01"
        val release = System.getenv("RELEASE_VERSION")
        if (release != null) {
            val numeric = release.slice(1 until release.length).toInt() // chop off the v
            versionCode = numeric
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
            versionName = Instant.now().atZone(ZoneId.of("UTC")).format(formatter) + ".$release"
        }
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    signingConfigs {
        create("config") {
            keyAlias = "app-release"
            storeFile = file("../app.keystore")
            storePassword = findProperty("jamiAppSigningKey") as? String?
            keyPassword = findProperty("jamiAppSigningKey") as? String?
        }
    }
    buildTypes {
        debug {
            isDebuggable = true
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("config")
        }
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
        aidl = true
    }
    flavorDimensions += "push"
    productFlavors {
        create("noPush") {
            dimension = "push"
        }
        create("withFirebase") {
            dimension = "push"
        }
        create("withUnifiedPush") {
            dimension = "push"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

val markwon_version = "4.6.2"

dependencies {
    implementation (project(":libjamiclient"))
    implementation ("org.jetbrains.kotlin:kotlin-stdlib:$kotlin_version")
    implementation ("androidx.core:core-ktx:1.13.1")
    implementation ("androidx.appcompat:appcompat:1.7.0")
    implementation ("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation ("androidx.cardview:cardview:1.0.0")
    implementation ("androidx.preference:preference-ktx:1.2.1")
    implementation ("androidx.recyclerview:recyclerview:1.3.2")
    implementation ("androidx.leanback:leanback:1.2.0-alpha04")
    implementation ("androidx.leanback:leanback-preference:1.2.0-alpha04")
    implementation ("androidx.car.app:app:1.4.0")
    implementation ("androidx.tvprovider:tvprovider:1.1.0-alpha01")
    implementation ("androidx.media:media:1.7.0")
    implementation ("androidx.sharetarget:sharetarget:1.2.0")
    implementation ("androidx.emoji2:emoji2:1.5.0")
    implementation ("androidx.viewpager2:viewpager2:1.1.0")
    implementation ("androidx.emoji2:emoji2-emojipicker:1.5.0")
    implementation ("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.6")
    implementation ("androidx.window:window:1.3.0")
    implementation ("com.google.android.material:material:1.12.0")
    implementation ("androidx.biometric:biometric:1.1.0")
    implementation ("com.google.android.flexbox:flexbox:3.0.0")
    implementation ("com.google.protobuf:protobuf-javalite:4.28.3")
    implementation("androidx.annotation:annotation-jvm:1.9.0")

    // ORM
    implementation ("com.j256.ormlite:ormlite-android:5.7")

    // Barcode scanning
    implementation("com.journeyapps:zxing-android-embedded:4.3.0") { isTransitive = false }
    implementation ("com.google.zxing:core:3.5.3")

    // Dagger dependency injection
    implementation("com.google.dagger:hilt-android:$hilt_version")
    implementation("androidx.lifecycle:lifecycle-service:2.8.7")
    ksp("com.google.dagger:hilt-android-compiler:$hilt_version")

    // Espresso Unit Tests
    androidTestImplementation("androidx.test.ext:junit-ktx:1.2.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.test.espresso:espresso-contrib:3.6.1")
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestImplementation("com.squareup.okhttp3:okhttp:4.11.0")
    androidTestImplementation("androidx.test.espresso:espresso-intents:3.6.1")
    androidTestImplementation("androidx.test:core:1.6.1")

    // Glide
    implementation ("com.github.bumptech.glide:glide:4.16.0")
    ksp("com.github.bumptech.glide:ksp:4.16.0")
    // Android SVG
    implementation ("com.caverock:androidsvg-aar:1.4")

    // RxAndroid
    implementation ("io.reactivex.rxjava3:rxandroid:3.0.2")
    implementation ("io.reactivex.rxjava3:rxjava:3.1.9")

    // Open Street Map
    implementation ("org.osmdroid:osmdroid-android:6.1.20")

    // Markwon (Markdown support)
    implementation ("io.noties.markwon:core:$markwon_version")
    implementation ("io.noties.markwon:linkify:$markwon_version")

    implementation ("com.jsibbold:zoomage:1.3.1")
    implementation ("com.googlecode.ez-vcard:ez-vcard:0.11.3") {
        exclude(group= "org.freemarker", module= "freemarker")
        exclude(group= "com.fasterxml.jackson.core", module= "jackson-core")
    }

    "withFirebaseImplementation"("com.google.firebase:firebase-messaging:24.0.3") {
        exclude(group= "com.google.firebase", module= "firebase-core")
        exclude(group= "com.google.firebase", module= "firebase-analytics")
        exclude(group= "com.google.firebase", module= "firebase-measurement-connector")
    }
    "withUnifiedPushImplementation"("com.github.UnifiedPush:android-connector:2.4.0")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:4.26.0"
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                create("java") {
                    option("lite")
                }
            }
        }
    }
}

if (buildFirebase) {
    println ("apply plugin $buildFirebase")
    apply(plugin = "com.google.gms.google-services")
}