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

        // Packaging needs jpackage, which a full JDK ships and Android Studio's bundled JBR does
        // not. Point at one for a packaging run without disturbing the JDK everything else uses:
        //
        //   gradlew :desktopApp:packageMsi -Phaoshield.packagingJdk="C:\path\to\jdk-21"
        //
        // The MSI format additionally wants WiX Toolset v3 on PATH. Without either, the app still
        // runs from source with :desktopApp:run — packaging is for handing it to someone else.
        providers.gradleProperty("haoshield.packagingJdk").orNull?.let { javaHome = it }

        nativeDistributions {
            targetFormats(TargetFormat.Msi)
            packageName = "Hao Shield"
            packageVersion = "1.0.0"
            description = "Set this time aside."
            // Deliberately without the caron. An MSI's strings live in code page 1252, which has
            // no ǎ, and WiX fails the build outright rather than transliterating. The app wears
            // its own name properly everywhere it controls the encoding; the installer cannot.
            vendor = "Hao Shield"

            windows {
                // Without these the app installs correctly and then cannot be found: no Start menu
                // entry, nothing to search for, only a folder under Program Files.
                menuGroup = "Hao Shield"
                shortcut = true

                // Stable across versions, so installing 1.0.1 replaces 1.0.0 rather than sitting
                // beside it. Generated once and never to be changed.
                upgradeUuid = "6f3a1c58-9d24-4e77-b0a1-2c5f8e7d4a13"
            }
        }
    }
}
