package dev.inmo.tgbotapi

object AppConfig {
    fun botToken(): String =
        System.getenv("BOT_TOKEN")?.takeIf { it.isNotBlank() } ?: error("BOT_TOKEN env var is not set")

    /** ClickHouse logging is on only when this is set. */
    fun clickHouseJdbcUrl(): String? = System.getenv("CLICKHOUSE_JDBC_URL")?.takeIf { it.isNotBlank() }
}
