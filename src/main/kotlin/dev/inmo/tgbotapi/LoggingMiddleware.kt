package dev.inmo.tgbotapi

import com.clickhouse.jdbc.ClickHouseDataSource
import com.google.gson.Gson
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.info
import dev.inmo.tgbotapi.bot.ktor.KtorCallFactory
import dev.inmo.tgbotapi.bot.ktor.TelegramBotPipelinesHandler
import dev.inmo.tgbotapi.requests.GetUpdates
import dev.inmo.tgbotapi.requests.abstracts.Request
import dev.inmo.tgbotapi.requests.bot.GetMe
import dev.inmo.tgbotapi.requests.webhook.DeleteWebhook
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.Json
import kotliquery.queryOf
import kotliquery.sessionOf
import java.net.InetAddress
import java.sql.SQLException
import java.time.LocalDateTime
import java.util.ArrayList
import javax.sql.DataSource
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties

class LoggingMiddleware: TelegramBotPipelinesHandler {

    val dataSource: DataSource = try {
        ClickHouseDataSource(System.getenv("CLICKHOUSE_URL"))
    } catch (e: SQLException) {
        throw RuntimeException(e)
    }

    fun save(data: String, clazz: KClass<*>, income: Boolean) {
        sessionOf(dataSource).execute(
            queryOf(
                """
                              INSERT INTO bot_log.bot_log
                              ( date_time,
                                appName,
                                type,
                                data,
                                className,
                                host
                              )
                              VALUES (
                                :date_time,
                                :appName,
                                :type,
                                :data,
                                :className,
                                :host)
            """,
                mapOf(
                    "date_time" to LocalDateTime.now(),
                    "appName" to AppConfig.appName(),
                    "type" to if (income) "IN" else "OUT",
                    "data" to data,
                    "className" to clazz.simpleName,
                    "host" to (System.getenv("HOST")?: InetAddress.getLocalHost().hostName)
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
    override suspend fun <T : Any> onRequestResultPresented(
        result: T,
        request: Request<T>,
        resultCallFactory: KtorCallFactory,
        callsFactories: List<KtorCallFactory>
    ): T? {
        if (request !is GetUpdates && request !is DeleteWebhook && request !is GetMe) {
            runCatching {
                save(nonstrictJsonFormat.encodeToJsonElement(getSerializer(request), request).toString(), request::class, false)
            }.onFailure { KSLog.info("Failed to save request ${request::class.simpleName}") }
        }

        if (request is GetUpdates) {
            (result as ArrayList<Any>).forEach { save(gson.toJson(it), it::class, true)}
        } else if (request !is DeleteWebhook && request !is GetMe) {
            save(gson.toJson(result), request::class, true)
        }

        return super.onRequestResultPresented(result, request, resultCallFactory, callsFactories)
    }

    fun<T: Any> getSerializer(data: T): SerializationStrategy<T> {
        val property = data::class.declaredMemberProperties
            .find { it.name == "requestSerializer" }

        return (property!!.call(data)) as SerializationStrategy<T>
    }

}