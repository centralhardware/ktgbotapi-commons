package me.centralhardware.telegram

import dev.inmo.kslog.common.configureLogger
import dev.inmo.tgbotapi.AppConfig
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

suspend fun main() {
    AppConfig.init("integrationTest")
    configureLogger()
    telegramBotWithBehaviourAndLongPolling(
        System.getenv("BOT_TOKEN"),
        CoroutineScope(Dispatchers.IO),
    ) {
        onText {
        }
    }.second.join()
}