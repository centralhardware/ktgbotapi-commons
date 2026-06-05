plugins {
    kotlin("jvm")
    `maven-publish`
}

group = "me.centralhardware.telegram.middleware"

dependencies {
    implementation("dev.inmo:tgbotapi:33.1.0")
    implementation("dev.inmo:kslog:1.6.1")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "restrict-access-middleware"
        }
    }
}
