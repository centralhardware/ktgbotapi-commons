plugins {
    kotlin("jvm")
    `maven-publish`
}

group = "me.centralhardware"

dependencies {
    implementation("dev.inmo:tgbotapi:35.1.0")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "conversation"
            from(components["java"])
        }
    }
}
