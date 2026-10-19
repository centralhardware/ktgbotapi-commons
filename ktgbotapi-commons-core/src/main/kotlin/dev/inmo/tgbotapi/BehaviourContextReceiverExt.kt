package dev.inmo.tgbotapi

import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.configureLogger
import dev.inmo.kslog.common.info
import dev.inmo.kslog.common.warning
import dev.inmo.micro_utils.common.Warning
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.bot.ktor.middlewares.TelegramBotMiddlewaresPipelinesHandler
import dev.inmo.tgbotapi.bot.ktor.middlewares.builtins.ExceptionsThrottlerTelegramBotMiddleware
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContextReceiver
import dev.inmo.tgbotapi.extensions.behaviour_builder.CustomBehaviourContextAndTypeReceiver
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndLongPolling
import dev.inmo.tgbotapi.types.update.abstracts.Update
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import me.centralhardware.telegram.middleware.clickHouseExceptionsHandler
import me.centralhardware.telegram.middleware.clickHouseLogging
import me.centralhardware.telegram.middleware.clickHouseRequestIdContext
import me.centralhardware.telegram.middleware.stdoutLogging

@OptIn(Warning::class)
suspend fun longPolling(
    botName: String,
    middlewares: TelegramBotMiddlewaresPipelinesHandler.Builder.() -> Unit = {},
    subcontextInitialAction: CustomBehaviourContextAndTypeReceiver<BehaviourContext, Unit, Update> = {},
    block: BehaviourContextReceiver<Unit>,
): Pair<TelegramBot, Job> {
    configureLogger(botName)
    // ClickHouse logging is optional: a bot without CLICKHOUSE_JDBC_URL logs to stdout only.
    val clickHouseUrl = AppConfig.clickHouseJdbcUrl()
    if (clickHouseUrl == null) KSLog.info("CLICKHOUSE_JDBC_URL not set: ClickHouse logging off")
    // Built once; called for every exception a handler throws.
    val clickHouseExceptions = clickHouseUrl?.let { clickHouseExceptionsHandler(botName, it) }
    val requestIdContext = clickHouseUrl?.let { clickHouseRequestIdContext(it) }
    val res =
        telegramBotWithBehaviourAndLongPolling(
            AppConfig.botToken(),
            CoroutineScope(Dispatchers.IO),
            defaultExceptionsHandler = { t: Throwable ->
                KSLog.warning("", t)
                clickHouseExceptions?.invoke(t)
            },
            autoSkipTimeoutExceptions = true,
            builder = {
                includeMiddlewares {
                    addMiddleware { ExceptionsThrottlerTelegramBotMiddleware() }
                    stdoutLogging()
                    clickHouseUrl?.let { clickHouseLogging(botName, it) }
                    middlewares.invoke(this)
                }
            },
            subcontextInitialAction = { update ->
                requestIdContext?.invoke(this, update)
                subcontextInitialAction.invoke(this, update)
            },
            block = block,
        )
    HealthCheck.register(botName, res.first)
    KSLog.info("$botName started")
    return res
}
