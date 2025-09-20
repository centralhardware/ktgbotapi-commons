plugins {
    java
    `maven-publish`
    kotlin("jvm") version "2.2.20"
}

group = "me.centralhardware"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

val ktgbotapiVersion = "28.0.2"

dependencies {
    implementation("org.apache.commons:commons-lang3:3.18.0")

    implementation("dev.inmo:kslog:1.5.0")
    implementation("dev.inmo:tgbotapi:$ktgbotapiVersion")
    implementation("com.github.centralhardware.ktgbotapi-middlewars:ktgbotapi-clickhouse-logging-middleware:$ktgbotapiVersion")
    implementation("com.github.centralhardware.ktgbotapi-middlewars:ktgbotapi-stdout-logging-middleware:$ktgbotapiVersion")
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