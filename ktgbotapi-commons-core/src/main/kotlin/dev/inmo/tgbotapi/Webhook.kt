package dev.inmo.tgbotapi

import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.configureLogger
import dev.inmo.kslog.common.info
import dev.inmo.kslog.common.warning
import dev.inmo.micro_utils.common.Warning
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.bot.ktor.middlewares.TelegramBotMiddlewaresPipelinesHandler
import dev.inmo.tgbotapi.bot.ktor.middlewares.builtins.ExceptionsThrottlerTelegramBotMiddleware
import dev.inmo.tgbotapi.extensions.api.utils.updateHandlerWithMediaGroupsAdaptation
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContextReceiver
import dev.inmo.tgbotapi.extensions.behaviour_builder.CustomBehaviourContextAndTypeReceiver
import dev.inmo.tgbotapi.extensions.behaviour_builder.buildBehaviour
import dev.inmo.tgbotapi.bot.ktor.telegramBot
import dev.inmo.tgbotapi.types.update.abstracts.Update
import dev.inmo.tgbotapi.types.update.abstracts.UpdateDeserializationStrategy
import dev.inmo.tgbotapi.updateshandlers.FlowsUpdatesFilter
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.http.HttpStatusCode
import io.ktor.server.cio.CIO
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.request.header
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import me.centralhardware.telegram.middleware.clickHouseExceptionsHandler
import me.centralhardware.telegram.middleware.stdoutLogging
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * Carries tg-webhook-relay's log id from an incoming update onto every Bot API call
 * the handler of that update makes.
 *
 * The relay forwards each update with `X-Log-Update-Id`; [webhook] remembers it by
 * Telegram update id, [subcontextAction] binds it to the handler's coroutine job,
 * and [plugin] puts it back on each outgoing request made from that job (or its
 * children). The relay logs those calls under the same id, so one update and
 * everything its handler did share one `update_id` in `bot_requests`.
 */
object LogUpdateId {
    const val HEADER = "X-Log-Update-Id"
    private const val PENDING_TTL_MS = 60_000L

    private val pending = ConcurrentHashMap<Long, Long>()
    private val byJob = ConcurrentHashMap<Job, Long>()
    private val cleanup = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun remember(telegramUpdateId: Long, logId: Long) {
        pending[telegramUpdateId] = logId
        cleanup.launch { delay(PENDING_TTL_MS); pending.remove(telegramUpdateId, logId) }
    }

    /** Use as (part of) subcontextInitialAction. */
    val subcontextAction: CustomBehaviourContextAndTypeReceiver<BehaviourContext, Unit, Update> = { update ->
        val id = pending[update.updateId.long]
        val job = currentCoroutineContext()[Job]
        if (id != null && job != null) {
            byJob[job] = id
            job.invokeOnCompletion { byJob.remove(job, id) }
        }
    }

    fun current(job: Job?): Long? {
        if (job == null) return null
        byJob[job]?.let { return it }
        return byJob.entries.firstOrNull { (parent, _) -> parent.hasDescendant(job) }?.value
    }

    private fun Job.hasDescendant(target: Job): Boolean =
        this === target || children.any { it.hasDescendant(target) }

    /** Ktor client plugin: ktgbotapi runs each request in the caller's coroutine. */
    val plugin = createClientPlugin("LogUpdateId") {
        onRequest { request, _ ->
            current(currentCoroutineContext()[Job])?.let { request.headers.append(HEADER, it.toString()) }
        }
    }
}

/** hex(sha256("tg-webhook-relay:" + token)) — the secret tg-webhook-relay sends. */
fun relaySecret(token: String): String =
    MessageDigest.getInstance("SHA-256").digest("tg-webhook-relay:$token".toByteArray())
        .joinToString("") { "%02x".format(it) }

private val updateJson = Json { ignoreUnknownKeys = true; isLenient = true; explicitNulls = false }

/**
 * Starts a bot behind tg-webhook-relay: Bot API calls go through the relay (which logs
 * them), and updates arrive as webhook calls from it on `POST /telegram/update`.
 * The bot itself writes nothing to ClickHouse except handler exceptions, which the
 * relay can't see.
 *
 * Environment variables:
 * | Variable | Description | Default |
 * |----------|-------------|---------|
 * | `TELEGRAM_RELAY_URL` | Relay base URL, used as the Bot API URL | required |
 * | `WEBHOOK_PORT` | Port for `POST /telegram/update` | `8080` |
 */
@OptIn(Warning::class)
suspend fun webhook(
    botName: String,
    middlewares: TelegramBotMiddlewaresPipelinesHandler.Builder.() -> Unit = {},
    subcontextInitialAction: CustomBehaviourContextAndTypeReceiver<BehaviourContext, Unit, Update> = {},
    block: BehaviourContextReceiver<Unit>,
): Pair<TelegramBot, Job> {
    configureLogger(botName)
    val token = AppConfig.botToken()
    val relayUrl = System.getenv("TELEGRAM_RELAY_URL")?.takeIf { it.isNotBlank() }?.trimEnd('/')
        ?: error("TELEGRAM_RELAY_URL is not set")
    val clickHouseExceptions = AppConfig.clickHouseJdbcUrl()?.let { clickHouseExceptionsHandler(botName, it) }

    val bot = telegramBot(token, apiUrl = relayUrl) {
        client = HttpClient(OkHttp) { install(LogUpdateId.plugin) }
        includeMiddlewares {
            addMiddleware { ExceptionsThrottlerTelegramBotMiddleware() }
            stdoutLogging()
            middlewares.invoke(this)
        }
    }
    val scope = CoroutineScope(Dispatchers.IO)
    val filter = FlowsUpdatesFilter()
    val context = bot.buildBehaviour(
        flowUpdatesFilter = filter,
        scope = scope,
        defaultExceptionsHandler = { t: Throwable ->
            KSLog.warning("", t)
            clickHouseExceptions?.invoke(t)
        },
        subcontextInitialAction = { update ->
            LogUpdateId.subcontextAction.invoke(this, update)
            subcontextInitialAction.invoke(this, update)
        },
        block = block,
    )
    // Same media-group merging long polling does, so onMediaGroup keeps working.
    val receiver = context.updateHandlerWithMediaGroupsAdaptation(filter.asUpdateReceiver)
    WebhookServer.start(relaySecret(token)) { update -> receiver(update) }
    HealthCheck.register(botName, bot)
    KSLog.info("$botName started (webhook via $relayUrl)")
    return bot to context.coroutineContext.job
}

private object WebhookServer {
    @Volatile private var server: EmbeddedServer<*, *>? = null

    @Synchronized
    fun start(secret: String, onUpdate: suspend (Update) -> Unit) {
        if (server != null) return
        val port = System.getenv("WEBHOOK_PORT")?.toIntOrNull() ?: 8080
        server = embeddedServer(CIO, port = port) {
            routing {
                post("/telegram/update") {
                    if (call.request.header("X-Telegram-Bot-Api-Secret-Token") != secret) {
                        call.respond(HttpStatusCode.Unauthorized); return@post
                    }
                    // A non-2xx makes the relay retry this update, in order.
                    val update = try {
                        updateJson.decodeFromString(UpdateDeserializationStrategy, call.receiveText())
                    } catch (e: Exception) {
                        KSLog.warning("webhook update rejected", e)
                        call.respond(HttpStatusCode.BadRequest); return@post
                    }
                    call.request.header(LogUpdateId.HEADER)?.toLongOrNull()
                        ?.let { LogUpdateId.remember(update.updateId.long, it) }
                    onUpdate(update)
                    call.respond(HttpStatusCode.OK)
                }
            }
        }.start(wait = false)
        KSLog.info("webhook receiver listening on :$port/telegram/update")
    }
}
