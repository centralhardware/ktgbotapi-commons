plugins {
    java
    `maven-publish`
    kotlin("jvm") version "2.4.0"
}

group = "me.centralhardware"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

val ktgbotapiVersion = "33.1.0"
val middlewareVersion = "8cfeacd7"

dependencies {
    implementation("org.apache.commons:commons-lang3:3.20.0")

    implementation("dev.inmo:kslog:1.6.1")
    implementation("dev.inmo:tgbotapi:$ktgbotapiVersion")
    implementation("com.github.centralhardware.ktgbotapi-middlewars:ktgbotapi-stdout-logging-middleware:$middlewareVersion")
    implementation("com.github.centralhardware.ktgbotapi-middlewars:ktgbotapi-clickhouse-logging-middleware:$middlewareVersion")
    api("com.github.centralhardware.ktgbotapi-middlewars:ktgbotapi-restrict-access-middleware:$middlewareVersion")
}

tasks.test {
    useJUnitPlatform()
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