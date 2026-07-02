plugins {
    kotlin("jvm")
    `maven-publish`
}

group = "me.centralhardware.telegram.middleware"

dependencies {
    implementation("dev.inmo:tgbotapi:35.1.0")
    implementation("dev.inmo:kslog:1.6.0")
    implementation("com.clickhouse:clickhouse-jdbc:0.9.8")
    implementation("org.apache.httpcomponents.client5:httpclient5:5.6.2")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            artifactId = "clickhouse-logging-middleware"
        }
    }
}
