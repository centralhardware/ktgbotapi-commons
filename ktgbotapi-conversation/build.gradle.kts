plugins {
    kotlin("jvm")
    `maven-publish`
}

group = "me.centralhardware"

dependencies {
    implementation("dev.inmo:tgbotapi:35.0.0")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "conversation"
            from(components["java"])
        }
    }
}
