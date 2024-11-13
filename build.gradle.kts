plugins {
    java
    `maven-publish`
    kotlin("jvm") version "2.0.21"
}

group = "me.centralhardware"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

val ktorVersion = "3.0.1"
val clickhouseVersion = "0.7.1-patch1"

dependencies {
    implementation("org.apache.commons:commons-lang3:3.17.0")

    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-core:$ktorVersion")

    implementation("dev.inmo:kslog:1.3.6")
    implementation("dev.inmo:tgbotapi:20.0.1")
    implementation("com.github.centralhardware:ktgbotapi-clickhouse-logging-middleware:3cacb34eda")
    implementation("com.github.centralhardware:ktgbotapi-stdout-logging-middleware:90f47ea362")

    implementation("com.clickhouse:clickhouse-jdbc:$clickhouseVersion")
    implementation("com.clickhouse:clickhouse-http-client:$clickhouseVersion")
    implementation("com.github.seratch:kotliquery:1.9.0")
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