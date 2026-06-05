plugins {
    kotlin("jvm") version "2.3.21" apply false
}

subprojects {
    version = "1.0-SNAPSHOT"

    repositories {
        mavenCentral()
        maven("https://jitpack.io")
    }
}
