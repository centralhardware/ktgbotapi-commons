package dev.inmo.tgbotapi

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.info
import dev.inmo.kslog.common.warning
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.apache.commons.lang3.BooleanUtils
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/**
 * Liveness endpoint for containerized bots.
 *
 * Every bot started through [longPolling] registers itself here, and the first registration
 * starts a tiny HTTP server exposing `GET /health`. A probe pings the Bot API with `getMe`
 * for each registered bot: `200 OK` when every bot answers, `503` when any of them does not
 * (bad token, revoked bot, no connectivity to Telegram).
 *
 * Environment variables:
 * | Variable | Description | Default |
 * |----------|-------------|---------|
 * | `HEALTHCHECK_ENABLED` | Start the endpoint at all | `true` |
 * | `HEALTHCHECK_PORT` | Port to listen on | `8081` |
 * | `HEALTHCHECK_TIMEOUT_MS` | Per-bot `getMe` timeout | `5000` |
 */
object HealthCheck {
    const val DEFAULT_PORT = 8081
    private const val DEFAULT_TIMEOUT_MS = 5_000L
    private const val PATH = "/health"

    private val bots = ConcurrentHashMap<String, TelegramBot>()

    @Volatile
    private var server: HttpServer? = null

    val port: Int
        get() = System.getenv("HEALTHCHECK_PORT")?.toIntOrNull() ?: DEFAULT_PORT

    private val timeoutMs: Long
        get() = System.getenv("HEALTHCHECK_TIMEOUT_MS")?.toLongOrNull() ?: DEFAULT_TIMEOUT_MS

    private val enabled: Boolean
        get() = BooleanUtils.toBooleanObject(System.getenv("HEALTHCHECK_ENABLED") ?: "true") ?: true

    /** Registers [bot] under [botName] and starts the endpoint if it is not running yet. */
    @Synchronized
    fun register(botName: String, bot: TelegramBot) {
        bots[botName] = bot
        start()
    }

    @Synchronized
    fun start() {
        if (server != null || !enabled) return
        runCatching {
            HttpServer.create(InetSocketAddress(port), 0).apply {
                createContext(PATH, ::handle)
                executor = Executors.newFixedThreadPool(2)
                start()
            }
        }
            .onSuccess {
                server = it
                KSLog.info("health check listening on :$port$PATH")
            }
            .onFailure { KSLog.warning("failed to start health check on :$port", it) }
    }

    @Synchronized
    fun stop() {
        server?.stop(0)
        server = null
    }

    /** @return the names of the bots that failed the ping, with the reason. */
    fun failures(): Map<String, String> = runBlocking {
        bots.entries.mapNotNull { (name, bot) ->
            runCatching { withTimeout(timeoutMs) { bot.getMe() } }
                .fold({ null }, { name to (it.message ?: it::class.simpleName ?: "failed") })
        }.toMap()
    }

    private fun handle(exchange: HttpExchange) {
        try {
            if (exchange.requestMethod != "GET") {
                respond(exchange, 405, "method not allowed")
                return
            }
            when {
                bots.isEmpty() -> respond(exchange, 503, "no bots registered")
                else -> {
                    val failures = failures()
                    if (failures.isEmpty()) {
                        respond(exchange, 200, "OK")
                    } else {
                        respond(exchange, 503, failures.entries.joinToString("\n") { "${it.key}: ${it.value}" })
                    }
                }
            }
        } catch (t: Throwable) {
            KSLog.warning("health check request failed", t)
            runCatching { respond(exchange, 503, t.message ?: "failed") }
        } finally {
            exchange.close()
        }
    }

    private fun respond(exchange: HttpExchange, code: Int, body: String) {
        val bytes = "$body\n".toByteArray()
        exchange.responseHeaders.add("Content-Type", "text/plain; charset=utf-8")
        exchange.sendResponseHeaders(code, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
    }
}
