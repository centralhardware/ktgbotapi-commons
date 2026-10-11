plugins {
    kotlin("jvm") version "2.4.20" apply false
}

subprojects {
    version = "1.0-SNAPSHOT"

    repositories {
        mavenCentral()
        maven("https://jitpack.io")
    }

    // tgbotapi ships the CIO and Java Ktor engines; drop them so the default HttpClient() resolves to
    // OkHttp (added by ktgbotapi-commons-core). Configuration-level excludes are published, so
    // consumers inherit them.
    configurations.all {
        exclude(group = "io.ktor", module = "ktor-client-cio")
        exclude(group = "io.ktor", module = "ktor-client-cio-jvm")
        exclude(group = "io.ktor", module = "ktor-client-java")
        exclude(group = "io.ktor", module = "ktor-client-java-jvm")
    }
}
