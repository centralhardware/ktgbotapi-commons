plugins {
    kotlin("jvm")
    `maven-publish`
}

group = "me.centralhardware.telegram.middleware"

dependencies {
    implementation("dev.inmo:tgbotapi:37.1.0")
    implementation("dev.inmo:kslog:2.1.0")
    implementation("com.clickhouse:clickhouse-jdbc:0.10.0")
    implementation("org.apache.httpcomponents.client5:httpclient5:5.6.5")

    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "clickhouse-logging-middleware"
        }
    }
}
