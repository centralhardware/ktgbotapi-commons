plugins {
    java
    `maven-publish`
    kotlin("jvm")
}

group = "me.centralhardware"

val ktgbotapiVersion = "34.0.0"

dependencies {
    implementation("org.apache.commons:commons-lang3:3.20.0")

    api("dev.inmo:kslog:1.6.1")
    api("dev.inmo:tgbotapi:$ktgbotapiVersion")
    api(project(":ktgbotapi-conversation"))
    api(project(":ktgbotapi-stdout-logging-middleware"))
    api(project(":ktgbotapi-clickhouse-logging-middleware"))
    api(project(":ktgbotapi-restrict-access-middleware"))
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "commons"
            from(components["java"])
        }
    }
}
