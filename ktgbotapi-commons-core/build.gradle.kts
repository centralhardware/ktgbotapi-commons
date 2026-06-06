plugins {
    java
    `maven-publish`
    kotlin("jvm")
}

group = "me.centralhardware"

val ktgbotapiVersion = "33.1.0"

dependencies {
    api("org.apache.commons:commons-lang3:3.20.0")

    implementation("dev.inmo:kslog:1.6.1")
    implementation("dev.inmo:tgbotapi:$ktgbotapiVersion")
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
