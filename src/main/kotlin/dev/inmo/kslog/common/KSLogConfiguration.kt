package dev.inmo.kslog.common

import dev.inmo.tgbotapi.AppConfig
import io.ktor.utils.io.CancellationException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import org.apache.commons.lang3.BooleanUtils

val formatter = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss.SSS")

fun getDateTime(): String = LocalDateTime.now().format(formatter)

fun KSLog.configure() {
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
            println("${getDateTime()} $message")
            if (throwable != null) {
                println(throwable.stackTraceToString())
            }
        }
    setDefaultKSLog(KSLog(AppConfig.appName(), minLoggingLevel = minLogLevel))
}
