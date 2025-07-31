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

val ktorVersion = "3.2.3"
val clickhouseVersion = "0.9.1"

dependencies {
    implementation("org.apache.commons:commons-lang3:3.18.0")

    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-core:$ktorVersion")

    implementation("dev.inmo:kslog:1.5.0")
    implementation("dev.inmo:tgbotapi:27.1.0")
    implementation("com.github.centralhardware:ktgbotapi-clickhouse-logging-middleware:86f2a4a41e")
    implementation("com.github.centralhardware:ktgbotapi-stdout-logging-middleware:260117ea6f")

    implementation("com.clickhouse:clickhouse-jdbc:$clickhouseVersion")
    implementation("com.clickhouse:clickhouse-http-client:$clickhouseVersion")
    implementation("com.github.seratch:kotliquery:1.9.1")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(21)
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