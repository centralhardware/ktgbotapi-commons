plugins {
    kotlin("jvm")
    `maven-publish`
}

group = "me.centralhardware"

dependencies {
    implementation("dev.inmo:tgbotapi:37.1.0")

    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "conversation"
            from(components["java"])
        }
    }
}
