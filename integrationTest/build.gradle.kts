plugins {
    kotlin("jvm")
}

group = "me.centralhardware.telegram"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation(project(":"))
    implementation("dev.inmo:tgbotapi:24.0.1")
}

tasks.test {
    useJUnitPlatform()
}