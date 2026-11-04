import com.lagradost.cloudstream3.gradle.CloudstreamExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("com.lagradost.cloudstream3.gradle")
}

// Configure Cloudstream single-file plugin metadata
cloudstream {
    // Repository URL where your plugin builds or source is hosted
    setRepo("https://github.com/onlinework861/cloudstream-plugins")
    
    // Plugin bundle metadata
    description = "Single-file bundle featuring Movi.pk and HDHub4u high-speed streaming scrapers"
    authors = listOf("onlinework861")
    
    // Status flag: 1 = Down, 2 = Maintenance, 3 = Beta, 4 = Working
    status = 4
    
    // Versioning
    version = 1
}

repositories {
    google()
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    val cloudstream by configurations
    
    // Core Cloudstream 3 dependency provided at runtime by the Android app
    cloudstream("com.lagradost:cloudstream3:pre-release")
    
    // Note: Jsoup, Jackson JSON parser, OkHttp, and Android framework classes 
    // are already provided by Cloudstream core at runtime and should not be bundled.
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_1_8)
        freeCompilerArgs.addAll(
            "-Xopt-in=kotlin.RequiresOptIn",
            "-Xno-param-assertions",
            "-Xno-call-assertions"
        )
    }
}
