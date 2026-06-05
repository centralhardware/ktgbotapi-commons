pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

rootProject.name = "ktgbotapi-commons"

include("ktgbotapi-commons-core")
include("ktgbotapi-conversation")
include("ktgbotapi-stdout-logging-middleware")
include("ktgbotapi-restrict-access-middleware")
include("ktgbotapi-clickhouse-logging-middleware")
include("integration-test-bot")