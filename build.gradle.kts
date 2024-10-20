plugins {
    java
    `maven-publish`
    kotlin("jvm") version "2.0.20"
}

group = "me.centralhardware"
version = "1.0-SNAPSHOT"

repositories {
    maven("https://nexus.inmo.dev/repository/maven-releases/")
    mavenCentral()
}

val ktorVersion = "2.3.12";

dependencies {
    implementation("com.google.code.gson:gson:2.8.9")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    implementation("org.apache.commons:commons-lang3:3.17.0")

    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-core:$ktorVersion")

    implementation("dev.inmo:kslog:1.3.6") { isTransitive = true }
    implementation("dev.inmo:tgbotapi:18.2.2-branch_18.2.2-build2465") { isTransitive = true }
    implementation("dev.inmo:tgbotapi:18.2.2-branch_18.2.2-build2465")

    implementation("org.apache.httpcomponents.client5:httpclient5:5.3.1")
    implementation("com.clickhouse:clickhouse-jdbc:0.7.0")
    implementation("org.lz4:lz4-java:1.8.0")
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