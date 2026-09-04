plugins {
    // The build pins Java 21. Without this, a machine with no JDK 21 installed fails with
    // "No matching toolchains found" and no hint that it is a JDK problem; with it, Gradle
    // downloads a matching toolchain itself.
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

rootProject.name = "korea-job-intelligence-backend"
