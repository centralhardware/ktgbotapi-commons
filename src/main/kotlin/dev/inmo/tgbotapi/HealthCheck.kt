package dev.inmo.tgbotapi

import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import io.ktor.http.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

object HealthCheck {

    private val bots: MutableSet<TelegramBot> = mutableSetOf()

    fun addBot(bot: TelegramBot) {
        bots.add(bot)
    }

    init {
        embeddedServer(Netty, port = 81) {
                routing {
                    get("/health") {
                        if (health()) {
                            call.respond(HttpStatusCode.OK)
                        } else {
                            call.respond(HttpStatusCode.BadRequest)
                        }
                    }
                }
            }
            .start(wait = false)
    }

    suspend fun health(): Boolean = runCatching { bots.forEach { it.getMe() } }.isSuccess
}
