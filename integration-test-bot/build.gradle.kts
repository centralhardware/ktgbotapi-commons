plugins {
    kotlin("jvm")
    application
}

group = "me.centralhardware.telegram"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation(project(":ktgbotapi-commons-core"))
    implementation("dev.inmo:tgbotapi:34.0.0")
}

application {
    mainClass.set("me.centralhardware.telegram.MainKt")
}

tasks.test {
    useJUnitPlatform()
}
