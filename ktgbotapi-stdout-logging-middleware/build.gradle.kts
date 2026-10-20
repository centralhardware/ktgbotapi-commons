plugins {
    kotlin("jvm")
    `maven-publish`
}

group = "me.centralhardware.telegram.middleware"

dependencies {
    implementation("dev.inmo:tgbotapi:37.1.0")
    implementation("dev.inmo:kslog:2.1.0")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "stdout-logging-middleware"
        }
    }
}
