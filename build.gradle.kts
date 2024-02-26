plugins {
    java
    `maven-publish`
    kotlin("jvm") version "1.9.22"
}

group = "me.centralhardware"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.telegram:telegrambots:6.9.7.1"){
        isTransitive = true
    }
    implementation("dev.inmo:tgbotapi:10.1.0") {
        isTransitive = true
    }
    implementation("com.clickhouse:clickhouse-jdbc:0.6.0")
    implementation("org.apache.httpcomponents.client5:httpclient5:5.3.1")
    implementation("org.lz4:lz4-java:1.8.0")
    implementation("com.github.seratch:kotliquery:1.9.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
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