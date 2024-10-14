package dev.inmo.tgbotapi

import dev.inmo.tgbotapi.bot.ktor.KtorCallFactory
import dev.inmo.tgbotapi.bot.ktor.KtorPipelineStepsHolder
import dev.inmo.tgbotapi.requests.abstracts.Request

class LoggingMiddleware: KtorPipelineStepsHolder {

    override suspend fun <T : Any> onRequestReturnResult(
        result: Result<T>,
        request: Request<T>,
        callsFactories: List<KtorCallFactory>
    ): T {
        return super.onRequestReturnResult(result, request, callsFactories)
    }

}