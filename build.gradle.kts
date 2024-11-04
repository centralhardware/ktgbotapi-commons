plugins {
    java
    `maven-publish`
    kotlin("jvm") version "2.0.21"
    id("com.ncorti.ktfmt.gradle") version "0.21.0"
}

group = "me.centralhardware"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

val ktorVersion = "3.0.1"

dependencies {
    implementation("org.apache.commons:commons-lang3:3.17.0")

    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-core:$ktorVersion")

    implementation("dev.inmo:kslog:1.3.6")
    implementation("dev.inmo:tgbotapi:20.0.0")
    implementation("com.github.centralhardware:ktgbotapi-clickhouse-logging-middleware:305fea7fbf")
    implementation("com.github.centralhardware:ktgbotapi-stdout-logging-middleware:433b9934c3")
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

ktfmt {
    kotlinLangStyle()
}