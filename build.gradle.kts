plugins {
    java
    `maven-publish`
    kotlin("jvm") version "2.2.0"
}

val tgbotapiVersion = "27.1.2"

group = "me.centralhardware"
version = tgbotapiVersion

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    implementation("org.apache.commons:commons-lang3:3.18.0")

    implementation("dev.inmo:kslog:1.5.0")
    implementation("dev.inmo:tgbotapi:$tgbotapiVersion")
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
            version = tgbotapiVersion
            from(components["java"])
        }
    }
}

