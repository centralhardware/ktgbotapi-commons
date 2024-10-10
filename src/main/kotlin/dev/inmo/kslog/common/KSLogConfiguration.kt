package dev.inmo.kslog.common

import kotlinx.coroutines.CancellationException
import org.apache.commons.lang3.BooleanUtils
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

val formatter = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss.SSS")
fun getDateTime(): String = LocalDateTime.now().format(formatter)

fun KSLog.configure(appName: String) {
    val minLogLevel = if (BooleanUtils.toBooleanObject(System.getenv("DEBUG") ?: "false")) {
        LogLevel.DEBUG
    } else {
        LogLevel.INFO
    }
    KSLoggerDefaultPlatformLoggerLambda = fun(_, _, message, throwable){
        if (throwable is CancellationException) return
        println("${getDateTime()} $message")
        if (throwable != null) {
            println(throwable.stackTraceToString())
        }
    }
    setDefaultKSLog(
        KSLog(appName, minLoggingLevel = minLogLevel)
    )
}