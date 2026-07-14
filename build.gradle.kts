plugins {
    kotlin("jvm") version "2.4.10" apply false
}

subprojects {
    version = "1.0-SNAPSHOT"

    repositories {
        mavenCentral()
        maven("https://jitpack.io")
    }
}
