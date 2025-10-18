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
import me.centralhardware.telegram.middleware.stdoutLogging

@OptIn(Warning::class)
suspend fun longPolling(
    middlewares: TelegramBotMiddlewaresPipelinesHandler.Builder.() -> Unit = {},
    subcontextInitialAction: CustomBehaviourContextAndTypeReceiver<BehaviourContext, Unit, Update> = {},
    block: BehaviourContextReceiver<Unit>,
): Pair<TelegramBot, Job> {
    configureLogger()
    val res =
        telegramBotWithBehaviourAndLongPolling(
            AppConfig.botToken(),
            CoroutineScope(Dispatchers.IO),
            defaultExceptionsHandler = { t: Throwable -> KSLog.warning("", t) },
            autoSkipTimeoutExceptions = true,
            builder = {
                includeMiddlewares {
                    addMiddleware { ExceptionsThrottlerTelegramBotMiddleware() }
                    stdoutLogging()
                    middlewares.invoke(this)
                }
            },
            subcontextInitialAction = subcontextInitialAction,
            block = block,
        )
    KSLog.info("${AppConfig.appName()} started")
    return res
}
