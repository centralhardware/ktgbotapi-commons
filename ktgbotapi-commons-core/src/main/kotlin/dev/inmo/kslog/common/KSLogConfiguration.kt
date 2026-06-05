package dev.inmo.kslog.common

import org.apache.commons.lang3.BooleanUtils
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.coroutines.cancellation.CancellationException

val formatter = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss.SSS")

fun getDateTime(): String = LocalDateTime.now().format(formatter)

private fun Throwable.isHttpRequestTimeout(): Boolean =
    this::class.qualifiedName == "io.ktor.client.plugins.HttpRequestTimeoutException"

fun configureLogger(botName: String) {
    val minLogLevel =
        if (BooleanUtils.toBooleanObject(System.getenv("DEBUG") ?: "false")) {
            LogLevel.DEBUG
        } else {
            LogLevel.INFO
        }
    KSLoggerDefaultPlatformLoggerLambda =
        fun(_, _, message, throwable) {
            if (throwable != null && throwable is CancellationException)
                return
            // Benign long-polling getUpdates timeouts — not worth logging.
            if (throwable != null && throwable.isHttpRequestTimeout())
                return
            println("${getDateTime()} $message")
            if (throwable != null) {
                println(throwable.stackTraceToString())
            }
        }
    setDefaultKSLog(KSLog(botName, minLoggingLevel = minLogLevel))
}
