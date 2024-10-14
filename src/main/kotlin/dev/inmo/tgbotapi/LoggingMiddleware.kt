package dev.inmo.tgbotapi

import com.google.gson.Gson
import dev.inmo.tgbotapi.bot.ktor.KtorCallFactory
import dev.inmo.tgbotapi.bot.ktor.KtorPipelineStepsHolder
import dev.inmo.tgbotapi.requests.GetUpdates
import dev.inmo.tgbotapi.requests.abstracts.Request

class LoggingMiddleware: KtorPipelineStepsHolder {

    val gson = Gson()
    override suspend fun <T : Any> onRequestReturnResult(
        result: Result<T>,
        request: Request<T>,
        callsFactories: List<KtorCallFactory>
    ): T {
        if (result.isSuccess && request is GetUpdates) {
            println(gson.toJson(result))
        } else if (result.isSuccess && request !is GetUpdates) {
            println(gson.toJson(result))
        }

        return super.onRequestReturnResult(result, request, callsFactories)
    }

}