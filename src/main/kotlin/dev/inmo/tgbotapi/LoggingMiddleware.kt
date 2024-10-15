package dev.inmo.tgbotapi

import com.clickhouse.jdbc.ClickHouseDataSource
import com.google.gson.Gson
import dev.inmo.tgbotapi.bot.ktor.KtorCallFactory
import dev.inmo.tgbotapi.bot.ktor.KtorPipelineStepsHolder
import dev.inmo.tgbotapi.requests.GetUpdates
import dev.inmo.tgbotapi.requests.abstracts.Request
import dev.inmo.tgbotapi.requests.bot.GetMe
import dev.inmo.tgbotapi.requests.webhook.DeleteWebhook
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import kotliquery.queryOf
import kotliquery.sessionOf
import java.sql.SQLException
import java.time.LocalDateTime
import java.util.ArrayList
import javax.sql.DataSource
import kotlin.reflect.full.declaredMemberProperties

class LoggingMiddleware: KtorPipelineStepsHolder {

    val dataSource: DataSource = try {
        ClickHouseDataSource(System.getenv("CLICKHOUSE_URL"))
    } catch (e: SQLException) {
        throw RuntimeException(e)
    }

    fun save(data: String, clazz: String, income: Boolean) {
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
                    "data" to data,
                    "className" to clazz
                )
            )
        )
    }

    override suspend fun <T : Any> onAfterCallFactoryMakeCall(
        result: T?,
        request: Request<T>,
        potentialFactory: KtorCallFactory
    ): T? {
        return super.onAfterCallFactoryMakeCall(result, request, potentialFactory)
    }


    val gson = Gson()
    internal val nonstrictJsonFormat = Json {
        isLenient = true
        ignoreUnknownKeys = true
        allowSpecialFloatingPointValues = true
        useArrayPolymorphism = true
        encodeDefaults = true
    }
    override suspend fun <T : Any> onRequestReturnResult(
        result: Result<T>,
        request: Request<T>,
        callsFactories: List<KtorCallFactory>
    ): T {
        if (result.isSuccess && request is GetUpdates) {
            (result.getOrNull() as ArrayList<Any>).forEach { save(gson.toJson(it), it::class.simpleName!!, true)}
        }
        if (result.isSuccess && request !is GetUpdates && request !is DeleteWebhook && request !is GetMe) {
            save(nonstrictJsonFormat.encodeToJsonElement(getSerializer(request), request).toString(), request::class.simpleName!!, false)
        }

        return super.onRequestReturnResult(result, request, callsFactories)
    }

    fun<T: Any> getSerializer(data: T): SerializationStrategy<T> {
        val property = data::class.declaredMemberProperties
            .find { it.name == "requestSerializer" }

        return (property!!.call(data)) as SerializationStrategy<T>
    }

}