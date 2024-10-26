package dev.inmo.tgbotapi

import dev.inmo.tgbotapi.bot.TelegramBot
import dev.inmo.tgbotapi.extensions.api.bot.getMe
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

class HealthCheck(bot: TelegramBot) {

    companion object {
        private val bots: MutableSet<TelegramBot> = mutableSetOf()

        fun addBot(bot: TelegramBot) {
            bots.add(bot)
        }

        init {
            embeddedServer(Netty, port = 81) {
                    routing {
                        get("/health") {
                            try {
                                bots.forEach { it.getMe() }
                                call.respond(HttpStatusCode.OK)
                            } catch (_: Throwable) {
                                call.respond(HttpStatusCode.BadRequest)
                            }
                        }
                    }
                }
                .start(wait = false)
        }
    }
}
