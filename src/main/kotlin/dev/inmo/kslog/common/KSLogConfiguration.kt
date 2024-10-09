package dev.inmo.kslog.common

import kotlinx.coroutines.CancellationException
import org.apache.commons.lang3.BooleanUtils
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

fun getCallerMethodName(): String {
    val stacktrace = Thread.currentThread().stackTrace
        .filterNot { it.className.startsWith("dev.inmo.kslog.common") };
    val e = stacktrace[2];
    return e.toString();
}

val formatter = DateTimeFormatter.ofPattern("dd.MM HH:mm:ss:MI")
fun getDateTime(): String = LocalDateTime.now().format(formatter)

fun KSLog.configure(appName: String) {
    val minLogLevel = if (BooleanUtils.toBooleanObject(System.getenv("DEBUG") ?: "false")) {
        LogLevel.DEBUG
    } else {
        LogLevel.INFO
    }
    KSLoggerDefaultPlatformLoggerLambda = fun(level, tag, message, throwable){
        if (throwable is CancellationException) return
        println("${getDateTime()} ${getCallerMethodName()} $message")
        if (throwable != null) {
            println(throwable.stackTraceToString())
        }
    }
    setDefaultKSLog(
        KSLog(appName, minLoggingLevel = minLogLevel)
    )
}