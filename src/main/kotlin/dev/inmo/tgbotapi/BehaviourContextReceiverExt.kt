package dev.inmo.tgbotapi

import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.configure
import dev.inmo.kslog.common.warning
import dev.inmo.micro_utils.common.Warning
import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.bot.ktor.middlewares.TelegramBotMiddlewaresPipelinesHandler
import dev.inmo.tgbotapi.bot.ktor.middlewares.builtins.ExceptionsThrottlerTelegramBotMiddleware
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContextReceiver
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndLongPolling
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import me.centralhardware.telegram.clickhouseLogging
import me.centralhardware.telegram.stdoutLogging

@OptIn(Warning::class)
suspend fun longPolling(
    middlewares: TelegramBotMiddlewaresPipelinesHandler.Builder.() -> Unit = {},
    block: BehaviourContextReceiver<Unit>,
): Pair<TelegramBot, Job> {
    HealthCheck.health()
    KSLog.configure()
    val res =
        telegramBotWithBehaviourAndLongPolling(
            AppConfig.botToken(),
            CoroutineScope(Dispatchers.IO),
            defaultExceptionsHandler = { t: Throwable -> KSLog.warning("", t) },
            autoSkipTimeoutExceptions = true,
            builder = {
                includeMiddlewares {
                    addMiddleware { ExceptionsThrottlerTelegramBotMiddleware() }
                    clickhouseLogging(AppConfig.appName())
                    stdoutLogging()
                    middlewares.invoke(this)
                }
            },
            block = block,
        )
    HealthCheck.addBot(res.first)
    return res
}
