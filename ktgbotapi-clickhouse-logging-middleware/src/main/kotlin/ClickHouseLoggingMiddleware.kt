package me.centralhardware.telegram.middleware

import com.github.f4b6a3.tsid.TsidCreator
import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.error
import dev.inmo.kslog.common.info
import dev.inmo.micro_utils.common.Warning
import dev.inmo.tgbotapi.bot.ktor.middlewares.TelegramBotMiddlewaresPipelinesHandler
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.CustomBehaviourContextAndTypeReceiver
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.from
import dev.inmo.tgbotapi.requests.GetUpdates
import dev.inmo.tgbotapi.requests.bot.GetMe
import dev.inmo.tgbotapi.requests.abstracts.MultipartRequest
import dev.inmo.tgbotapi.requests.abstracts.Request
import dev.inmo.tgbotapi.requests.abstracts.SimpleRequest
import dev.inmo.tgbotapi.utils.toJsonWithoutNulls
import kotlinx.serialization.SerializationStrategy
import dev.inmo.tgbotapi.types.chat.User
import dev.inmo.tgbotapi.types.update.BusinessConnectionUpdate
import dev.inmo.tgbotapi.types.update.BusinessMessageUpdate
import dev.inmo.tgbotapi.types.update.CallbackQueryUpdate
import dev.inmo.tgbotapi.types.update.ChannelPostUpdate
import dev.inmo.tgbotapi.types.update.ChatBoostRemovedUpdate
import dev.inmo.tgbotapi.types.update.ChatBoostUpdatedUpdate
import dev.inmo.tgbotapi.types.update.ChatJoinRequestUpdate
import dev.inmo.tgbotapi.types.update.ChatMessageReactionUpdatedUpdate
import dev.inmo.tgbotapi.types.update.ChosenInlineResultUpdate
import dev.inmo.tgbotapi.types.update.CommonChatMemberUpdatedUpdate
import dev.inmo.tgbotapi.types.update.EditBusinessMessageUpdate
import dev.inmo.tgbotapi.types.update.EditChannelPostUpdate
import dev.inmo.tgbotapi.types.update.EditMessageUpdate
import dev.inmo.tgbotapi.types.update.InlineQueryUpdate
import dev.inmo.tgbotapi.types.update.ManagedBotUpdate
import dev.inmo.tgbotapi.types.update.MessageUpdate
import dev.inmo.tgbotapi.types.update.MyChatMemberUpdatedUpdate
import dev.inmo.tgbotapi.types.update.PaidMediaPurchasedUpdate
import dev.inmo.tgbotapi.types.update.PollAnswerUpdate
import dev.inmo.tgbotapi.types.update.PreCheckoutQueryUpdate
import dev.inmo.tgbotapi.types.update.ShippingQueryUpdate
import dev.inmo.tgbotapi.types.update.abstracts.Update
import dev.inmo.tgbotapi.utils.RiskFeature
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Timestamp
import java.time.Instant
import java.util.Collections
import java.util.IdentityHashMap
import java.util.Properties
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.coroutineContext

private const val PENDING_TTL_MS = 60_000L
private const val MAX_PENDING = 10_000
/** Rows waiting for the writer. Past this, new rows are dropped (and counted), never awaited. */
private const val ROW_BUFFER = 10_000
/** Rows per INSERT. */
private const val BATCH_SIZE = 500

// Log rows are written one per API call from many short-lived connections, which is exactly the
// shape async inserts exist for: the server batches them into bigger parts instead of one part per
// row. `wait_for_async_insert=0` makes the write fire-and-forget — a lost log row is cheaper than
// blocking a bot handler on the flush. Server settings travel as `clickhouse_setting_`-prefixed
// connection properties (needs clickhouse-jdbc >= 0.10.0, earlier versions pinned async_insert=0).
private val INSERT_PROPERTIES = Properties().apply {
    setProperty("clickhouse_setting_async_insert", "1")
    setProperty("clickhouse_setting_wait_for_async_insert", "0")
}

private data class UpdateContext(
    val updateId: Long,
    val userId: Long,
    val username: String,
    val firstName: String,
    val lastName: String,
) {
    companion object {
        val EMPTY = UpdateContext(0L, 0L, "", "", "")

        fun from(updateId: Long, user: User?): UpdateContext = UpdateContext(
            updateId = updateId,
            userId = user?.id?.chatId?.long ?: 0L,
            username = user?.username?.withoutAt.orEmpty(),
            firstName = user?.firstName.orEmpty(),
            lastName = user?.lastName.orEmpty(),
        )
    }
}

/**
 * Update ids are TSIDs (time-sorted 64-bit ids, Snowflake-shaped), made locally: unique across
 * bot instances in practice and no database round trip (they used to come from
 * `SELECT generateSnowflakeID()`, a connection per update).
 */
private fun nextUpdateId(): Long = TsidCreator.getTsid().toLong()

@OptIn(Warning::class, RiskFeature::class)
private fun Update.extractUser(): User? = when (this) {
    is MessageUpdate -> data.from
    is EditMessageUpdate -> data.from
    is ChannelPostUpdate -> data.from
    is EditChannelPostUpdate -> data.from
    is InlineQueryUpdate -> data.from
    is CallbackQueryUpdate -> data.from
    is ChosenInlineResultUpdate -> data.from
    is ShippingQueryUpdate -> data.from
    is PreCheckoutQueryUpdate -> data.from
    is BusinessMessageUpdate -> data.from
    is EditBusinessMessageUpdate -> data.from
    is MyChatMemberUpdatedUpdate -> data.user
    is CommonChatMemberUpdatedUpdate -> data.user
    is ChatJoinRequestUpdate -> data.user
    is BusinessConnectionUpdate -> data.user
    is PaidMediaPurchasedUpdate -> data.from
    is PollAnswerUpdate -> data.user
    is ChatMessageReactionUpdatedUpdate -> data.reactedUser
    is ChatBoostUpdatedUpdate -> data.boost.source.user
    is ChatBoostRemovedUpdate -> data.source.user
    is ManagedBotUpdate -> data.user
    else -> null  // PollUpdate, ChatMessageReactionsCountUpdatedUpdate, DeletedBusinessMessageUpdate, UnknownUpdate, RawUpdate — no associated user
}

private val pendingContexts = ConcurrentHashMap<Long, UpdateContext>()
private val jobToContext = ConcurrentHashMap<Job, UpdateContext>()
private val cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

private fun rememberPending(updateId: Long, ctx: UpdateContext) {
    if (pendingContexts.size >= MAX_PENDING) {
        pendingContexts.keys.firstOrNull()?.let { pendingContexts.remove(it) }
    }
    pendingContexts[updateId] = ctx
    cleanupScope.launch {
        delay(PENDING_TTL_MS)
        pendingContexts.remove(updateId, ctx)
    }
}

private fun Job.containsDescendant(target: Job): Boolean {
    if (this === target) return true
    for (child in children) {
        if (child.containsDescendant(target)) return true
    }
    return false
}

private fun currentContext(currentJob: Job?): UpdateContext {
    if (currentJob == null) return UpdateContext.EMPTY
    for ((parent, ctx) in jobToContext) {
        if (parent.containsDescendant(currentJob)) return ctx
    }
    return UpdateContext.EMPTY
}

// Updates consumed by `wait*` calls inside an existing handler don't trigger a new
// subcontext, so they bypass `clickHouseRequestIdContext`. We detect this by user: if
// any active handler context already exists for the same user, the new update is
// almost certainly part of that wait chain — reuse its id so the whole chain
// (incoming rows + outgoing requests) shares one update_id in ClickHouse.
private fun activeContextForUser(userId: Long): UpdateContext? {
    if (userId == 0L) return null
    for ((job, ctx) in jobToContext) {
        if (ctx.userId == userId && job.isActive) return ctx
    }
    return null
}

private fun loadDriver() {
    runCatching { Class.forName("com.clickhouse.jdbc.ClickHouseDriver") }
}

@Suppress("UNCHECKED_CAST")
private fun Request<*>.asJson(): String {
    if (this::class.isData) return toString()
    return runCatching {
        when (this) {
            is MultipartRequest.Common<*> -> paramsJson.toString()
            is MultipartRequest<*> -> paramsJson.toString()
            is SimpleRequest<*> -> toJsonWithoutNulls(requestSerializer as SerializationStrategy<Request<*>>).toString()
            else -> toString()
        }
    }.getOrElse { toString() }
}

private class Row(
    val timestamp: Instant,
    val botName: String,
    val ctx: UpdateContext,
    val method: String,
    val request: String,
    val response: String,
    val success: Boolean,
    val error: String,
    val durationMs: Long,
)

/**
 * One background writer per JDBC URL: rows go into a bounded channel and a single coroutine on
 * [Dispatchers.IO] inserts them in batches over one kept connection. Nothing on the bot's own
 * path ever waits on ClickHouse; when it is slow or down the buffer fills and rows are dropped.
 */
private class RowWriter(private val jdbcUrl: String) {
    private val rows = Channel<Row>(ROW_BUFFER)
    private val dropped = AtomicLong()
    private var connection: Connection? = null

    init {
        loadDriver()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            val batch = ArrayList<Row>(BATCH_SIZE)
            for (first in rows) {
                batch += first
                while (batch.size < BATCH_SIZE) batch += rows.tryReceive().getOrNull() ?: break
                runCatching { insert(batch) }.onFailure {
                    KSLog.error("ClickHouse insert of ${batch.size} row(s) failed", it)
                    runCatching { connection?.close() }
                    connection = null
                }
                batch.clear()
            }
        }
    }

    fun offer(row: Row) {
        if (rows.trySend(row).isFailure) {
            val n = dropped.incrementAndGet()
            if (n == 1L || n % 1000 == 0L) KSLog.error("ClickHouse log buffer full: $n row(s) dropped so far")
        }
    }

    private fun insert(batch: List<Row>) {
        val conn = connection?.takeIf { !it.isClosed }
            ?: DriverManager.getConnection(jdbcUrl, INSERT_PROPERTIES).also { connection = it }
        conn.prepareStatement(
            "INSERT INTO bot_requests " +
                "(timestamp, bot, update_id, user_id, username, first_name, last_name, " +
                "method, request, response, success, error, duration_ms) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)"
        ).use { ps ->
            for (r in batch) {
                ps.setTimestamp(1, Timestamp.from(r.timestamp))
                ps.setString(2, r.botName)
                ps.setLong(3, r.ctx.updateId)
                ps.setLong(4, r.ctx.userId)
                ps.setString(5, r.ctx.username)
                ps.setString(6, r.ctx.firstName)
                ps.setString(7, r.ctx.lastName)
                ps.setString(8, r.method)
                ps.setString(9, r.request)
                ps.setString(10, r.response)
                ps.setBoolean(11, r.success)
                ps.setString(12, r.error)
                ps.setLong(13, r.durationMs)
                ps.addBatch()
            }
            ps.executeBatch()
        }
    }

    companion object {
        private val writers = ConcurrentHashMap<String, RowWriter>()
        fun forUrl(jdbcUrl: String): RowWriter = writers.computeIfAbsent(jdbcUrl, ::RowWriter)
    }
}

private fun writeRow(
    jdbcUrl: String,
    botName: String,
    ctx: UpdateContext,
    method: String,
    request: String,
    response: String,
    success: Boolean,
    error: String,
    durationMs: Long,
) {
    RowWriter.forUrl(jdbcUrl).offer(
        Row(Instant.now(), botName, ctx, method, request, response, success, error, durationMs)
    )
}

/**
 * Install via `telegramBotWithBehaviourAndLongPolling(subcontextInitialAction = ...)` to auto-tag
 * every Telegram API call performed while handling an incoming `Update` with that update's
 * internal `update_id` and originating user, so handler code doesn't need to know about it.
 */
fun clickHouseRequestIdContext(
    jdbcUrl: String = System.getenv("CLICKHOUSE_JDBC_URL")
        ?: error("CLICKHOUSE_JDBC_URL env var is not set"),
): CustomBehaviourContextAndTypeReceiver<BehaviourContext, Unit, Update> =
    { update: Update ->
        // get(), not remove(): one Update can match several triggers (e.g. onCommand + onText) and
        // each invokes subcontextInitialAction independently — they must all see the same id.
        val ctx = pendingContexts[update.updateId.long]
            ?: UpdateContext.from(nextUpdateId(), update.extractUser())
        val job = coroutineContext[Job]
        if (job != null) {
            jobToContext[job] = ctx
            job.invokeOnCompletion { jobToContext.remove(job) }
        }
    }

/**
 * Adds a middleware that logs each Telegram API call to ClickHouse (`bot_requests` table). One row
 * per call, queued for a background writer that inserts in batches; the bot never waits on ClickHouse.
 *
 * `GetUpdates` itself is never logged; on success its result is decomposed into one row per
 * incoming `Update`. `GetMe` is never logged either — the health check polls it on a timer. Pair with [clickHouseRequestIdContext] for incoming-to-outgoing correlation
 * and [clickHouseExceptionsHandler] for exceptions thrown before the HTTP layer.
 *
 * Schema lives in `db/migration/V1__create_bot_requests.sql`.
 */
@OptIn(Warning::class)
fun TelegramBotMiddlewaresPipelinesHandler.Builder.clickHouseLogging(
    botName: String,
    jdbcUrl: String = System.getenv("CLICKHOUSE_JDBC_URL")
        ?: error("CLICKHOUSE_JDBC_URL env var is not set"),
) {
    // By identity: many requests are data classes, and two equal ones in flight at once (the
    // same sendMessage twice) must not share a start time.
    val starts: MutableMap<Request<*>, Long> = Collections.synchronizedMap(IdentityHashMap())

    addMiddleware {
        doOnBeforeCallFactoryMakeCall { request, _ ->
            if (request !is GetMe) starts[request] = System.nanoTime()
        }
        doOnRequestReturnResult { result, request, _ ->
            val startedNs = starts.remove(request)
            val durationMs = startedNs?.let { (System.nanoTime() - it) / 1_000_000 } ?: 0L
            val response = result.getOrNull()

            when {
                // The health-check endpoint polls getMe on a timer; those rows say nothing about
                // the bot's actual traffic and would swamp the log.
                request is GetMe -> Unit

                request is GetUpdates -> {
                    if (response is List<*>) {
                        response.filterIsInstance<Update>().forEach { update ->
                            val user = update.extractUser()
                            val userId = user?.id?.chatId?.long ?: 0L
                            val ctx = activeContextForUser(userId)
                                ?: UpdateContext.from(
                                    updateId = nextUpdateId(),
                                    user = user,
                                )
                            rememberPending(update.updateId.long, ctx)
                            writeRow(
                                jdbcUrl,
                                botName = botName,
                                ctx = ctx,
                                method = update::class.simpleName.orEmpty(),
                                request = update.updateId.toString(),
                                response = update.toString(),
                                success = true,
                                error = "",
                                durationMs = 0L,
                            )
                        }
                    }
                }

                else -> {
                    val throwable = result.exceptionOrNull()
                    writeRow(
                        jdbcUrl,
                        botName = botName,
                        ctx = currentContext(coroutineContext[Job]),
                        method = request.method(),
                        request = request.asJson(),
                        response = response?.toString().orEmpty(),
                        success = result.isSuccess,
                        error = throwable?.toString().orEmpty(),
                        durationMs = durationMs,
                    )
                }
            }
            null
        }
    }
    KSLog.info("Initialized ClickHouse logging middleware for bot '$botName'")
}

/**
 * `defaultExceptionsHandler` that writes one row per exception thrown inside behaviour handlers.
 * Catches things the HTTP middleware can't see (e.g. local validation in request constructors).
 */
fun clickHouseExceptionsHandler(
    botName: String,
    jdbcUrl: String = System.getenv("CLICKHOUSE_JDBC_URL")
        ?: error("CLICKHOUSE_JDBC_URL env var is not set"),
): suspend (Throwable) -> Unit {
    return { throwable ->
        writeRow(
            jdbcUrl,
            botName = botName,
            ctx = currentContext(coroutineContext[Job]),
            method = throwable::class.simpleName.orEmpty(),
            request = "",
            response = "",
            success = false,
            error = throwable.stackTraceToString(),
            durationMs = 0L,
        )
    }
}
