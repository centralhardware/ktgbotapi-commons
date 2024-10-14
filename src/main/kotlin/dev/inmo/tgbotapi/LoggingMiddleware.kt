package dev.inmo.tgbotapi

import com.clickhouse.jdbc.ClickHouseDataSource
import com.google.gson.Gson
import dev.inmo.tgbotapi.bot.ktor.KtorCallFactory
import dev.inmo.tgbotapi.bot.ktor.KtorPipelineStepsHolder
import dev.inmo.tgbotapi.requests.GetUpdates
import dev.inmo.tgbotapi.requests.abstracts.Request
import dev.inmo.tgbotapi.requests.bot.GetMe
import dev.inmo.tgbotapi.requests.webhook.DeleteWebhook
import kotliquery.queryOf
import kotliquery.sessionOf
import java.sql.SQLException
import java.time.LocalDateTime
import java.util.ArrayList
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
                              INSERT INTO bot_log.bot_log
                              ( date_time,
                                appName,
                                type,
                                data,
                                className
                              )
                              VALUES (
                                :date_time,
                                :appName,
                                :type,
                                :data,
                                :className)
            """,
                mapOf(
                    "date_time" to LocalDateTime.now(),
                    "appName" to AppConfig.appName(),
                    "type" to if (income) "IN" else "OUT",
                    "data" to gson.toJson(data),
                    "className" to data::class.simpleName
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
        if (result.isSuccess && validrequest(request) && result.getOrThrow() !is Boolean) {
            when (val response = result.getOrThrow()) {
                is ArrayList<*> -> (response as ArrayList<Any>).forEach { save(it, true) }
                else -> save(response, true)
            }
        }

        if (result.isSuccess && request !is GetUpdates && validrequest(request)) {
            save(request, false)
        }

        return super.onRequestReturnResult(result, request, callsFactories)
    }

    fun <T: Any> validrequest(request: Request<T>): Boolean = request !is DeleteWebhook && request !is GetMe

}