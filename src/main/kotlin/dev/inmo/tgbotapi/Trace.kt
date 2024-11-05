package dev.inmo.tgbotapi

import com.clickhouse.jdbc.ClickHouseDataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import java.sql.SQLException
import javax.sql.DataSource

object Trace {
    private val dataSource: DataSource =
        try {
            ClickHouseDataSource(System.getenv("CLICKHOUSE_URL"))
        } catch (e: SQLException) {
            throw RuntimeException(e)
        }

    fun save(param: Map<String, String>) = sessionOf(dataSource).execute(queryOf("""
        INSERT INTO trace (date_time, appName, param) 
        VALUES (now(), :appName, :param)
    """, mapOf("appName" to AppConfig.appName(), "param" to param)
    ))
}