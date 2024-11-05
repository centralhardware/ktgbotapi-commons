package dev.inmo.tgbotapi

import com.clickhouse.jdbc.ClickHouseDataSource
import java.sql.SQLException
import javax.sql.DataSource
import kotliquery.queryOf
import kotliquery.sessionOf

object Trace {
    private val dataSource: DataSource =
        try {
            ClickHouseDataSource(System.getenv("CLICKHOUSE_URL"))
        } catch (e: SQLException) {
            throw RuntimeException(e)
        }

    fun save(event: String, param: Map<String, String>) =
        sessionOf(dataSource)
            .execute(
                queryOf(
                    """
        INSERT INTO bot_log.trace (date_time, appName, param, event) 
        VALUES (now(), :appName, :param, :event)
    """,
                    mapOf("appName" to AppConfig.appName(), "param" to param, "event" to event),
                )
            )
}
