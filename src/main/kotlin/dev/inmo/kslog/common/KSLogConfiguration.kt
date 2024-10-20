package dev.inmo.kslog.common

import dev.inmo.tgbotapi.AppConfig
import io.sentry.Sentry
import org.apache.commons.lang3.BooleanUtils
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

val formatter = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss.SSS")
fun getDateTime(): String = LocalDateTime.now().format(formatter)

fun KSLog.configure() {
    val minLogLevel = if (BooleanUtils.toBooleanObject(System.getenv("DEBUG") ?: "false")) {
        LogLevel.DEBUG
    } else {
        LogLevel.INFO
    }
    KSLoggerDefaultPlatformLoggerLambda = fun(_, _, message, throwable){
        if (throwable != null && throwable::class.simpleName == "JobCancellationException") return
        println("${getDateTime()} $message")
        if (throwable != null) {
            println(throwable.stackTraceToString())
        }
    }
    setDefaultKSLog(
        KSLog(AppConfig.appName(), minLoggingLevel = minLogLevel)
    )
}