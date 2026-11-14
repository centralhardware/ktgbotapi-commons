plugins {
    kotlin("jvm") version "2.4.21" apply false
}

subprojects {
    version = "1.0-SNAPSHOT"

    repositories {
        mavenCentral()
        maven("https://jitpack.io")
    }

    // tgbotapi ships the CIO and Java Ktor engines. Drop CIO so the default HttpClient() resolves to
    // OkHttp (added by ktgbotapi-commons-core). Keep Java: tgbotapi's defaultKtorEngine hard-references
    // io.ktor.client.engine.java.Java, so excluding it crashes every bot with NoClassDefFoundError.
    // Configuration-level excludes are published, so consumers inherit them.
    configurations.all {
        exclude(group = "io.ktor", module = "ktor-client-cio")
        exclude(group = "io.ktor", module = "ktor-client-cio-jvm")
    }
}
