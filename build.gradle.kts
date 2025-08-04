plugins {
    java
    `maven-publish`
    kotlin("jvm") version "2.2.0"
}

group = "me.centralhardware"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation("org.apache.commons:commons-lang3:3.17.0")

    implementation("dev.inmo:kslog:1.5.0")
    implementation("dev.inmo:tgbotapi:27.1.1")
    implementation("com.github.centralhardware:ktgbotapi-clickhouse-logging-middleware:50869a92bf")
    implementation("com.github.centralhardware:ktgbotapi-stdout-logging-middleware:704657ed0d")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(24)
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "me.centralhardware"
            artifactId = "bot-common"
            version = "1.0-SNAPSHOT"
            from(components["java"])
        }
    }
}