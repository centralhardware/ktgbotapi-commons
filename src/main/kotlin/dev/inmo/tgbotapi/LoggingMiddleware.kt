package dev.inmo.tgbotapi

import com.clickhouse.jdbc.ClickHouseDataSource
import com.google.gson.Gson
import dev.inmo.tgbotapi.bot.ktor.KtorCallFactory
import dev.inmo.tgbotapi.bot.ktor.KtorPipelineStepsHolder
import dev.inmo.tgbotapi.requests.GetUpdates
import dev.inmo.tgbotapi.requests.abstracts.Request
import kotliquery.queryOf
import kotliquery.sessionOf
import java.sql.SQLException
import java.time.LocalDateTime
import javax.sql.DataSource

class LoggingMiddleware: KtorPipelineStepsHolder {

    val dataSource: DataSource = try {
        ClickHouseDataSource(System.getenv("CLICKHOUSE_URL"))
    } catch (e: SQLException) {
        throw RuntimeException(e)
    }

    fun save(data: Any, income: Boolean) {
        sessionOf(dataSource).execute(
            queryOf(
                """
                              INSERT INTO bot_log
                              ( date_time,
                                appName,
                                type,
                                data
                              )
                              VALUES (
                                :date_time,
                                :appName,
                                :type,
                                :data)
            """,
                mapOf(
                    "date_time" to LocalDateTime.now(),
                    "appName" to AppConfig.appName(),
                    "type" to if (income) "IN" else "OUT",
                    "data" to gson.toJson(data)
                )
            )
        )
    }

    val gson = Gson()
    override suspend fun <T : Any> onRequestReturnResult(
        result: Result<T>,
        request: Request<T>,
        callsFactories: List<KtorCallFactory>
    ): T {
        if (result.isSuccess && request is GetUpdates) {
            save(result, true)
        } else if (result.isSuccess && request !is GetUpdates && request !is Boolean) {
            save(request, false)
        }

        return super.onRequestReturnResult(result, request, callsFactories)
    }

}