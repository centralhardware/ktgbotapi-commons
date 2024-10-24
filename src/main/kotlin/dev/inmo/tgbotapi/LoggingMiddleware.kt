package dev.inmo.tgbotapi

import com.clickhouse.jdbc.ClickHouseDataSource
import com.google.gson.Gson
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.info
import dev.inmo.micro_utils.common.Warning
import dev.inmo.tgbotapi.bot.ktor.KtorCallFactory
import dev.inmo.tgbotapi.bot.ktor.TelegramBotPipelinesHandler
import dev.inmo.tgbotapi.bot.ktor.middlewares.TelegramBotMiddlewareBuilder
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

private fun save(data: String, clazz: KClass<*>, income: Boolean) {
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

fun<T: Any> getSerializer(data: T): SerializationStrategy<T> {
    val property = data::class.declaredMemberProperties
        .find { it.name == "requestSerializer" }

    return (property!!.call(data)) as SerializationStrategy<T>
}

private val dataSource: DataSource = try {
    ClickHouseDataSource(System.getenv("CLICKHOUSE_URL"))
} catch (e: SQLException) {
    throw RuntimeException(e)
}
@OptIn(Warning::class)
fun TelegramBotMiddlewareBuilder.addLogging() {
    val gson = Gson()
    val nonstrictJsonFormat = Json {
        isLenient = true
        ignoreUnknownKeys = true
        allowSpecialFloatingPointValues = true
        useArrayPolymorphism = true
        encodeDefaults = true
    }
    doOnRequestReturnResult { result, request, _ ->
        if (request !is GetUpdates && request !is DeleteWebhook && request !is GetMe) {
            runCatching {
                save(nonstrictJsonFormat.encodeToJsonElement(getSerializer(request), request).toString(), request::class, false)
            }.onFailure { KSLog.info("Failed to save request ${request::class.simpleName}") }
        }

        if (request is GetUpdates) {
            (result.getOrNull() as ArrayList<Any>).forEach { save(gson.toJson(it), it::class, true)}
        } else if (request !is DeleteWebhook && request !is GetMe) {
            save(gson.toJson(result), result::class, true)
        }
        null
    }
}