import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.jetbrains.compose)
    alias(libs.plugins.kotlin.compose)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

// The Windows app. It shares the session rules (:shared:data) and the design system (:shared:ui)
// with Android, and supplies its own everything-else: file-backed persistence instead of DataStore,
// a process watcher instead of an accessibility service, and a hand-written object graph instead of
// Hilt — which is the whole reason the shared modules were kept free of it.
dependencies {
    implementation(project(":shared:domain"))
    implementation(project(":shared:data"))
    implementation(project(":shared:ui"))
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.withType<Test>().configureEach {
    useJUnit()
    testLogging { events("passed", "failed", "skipped") }
}

compose.desktop {
    application {
        mainClass = "com.haoshield.desktop.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Msi)
            packageName = "Hao Shield"
            packageVersion = "1.0.0"
        }
    }
}
