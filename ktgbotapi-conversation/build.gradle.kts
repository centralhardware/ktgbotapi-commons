plugins {
    kotlin("jvm")
    `maven-publish`
}

group = "me.centralhardware"

dependencies {
    implementation("dev.inmo:tgbotapi:33.1.0")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "conversation"
            from(components["java"])
        }
    }
}
