package dev.inmo.kslog.common

import kotlinx.coroutines.CancellationException
import org.apache.commons.lang3.BooleanUtils

fun KSLog.configure(appName: String) {
    val minLogLevel = if (BooleanUtils.toBooleanObject(System.getenv("DEBUG") ?: "false")) {
        LogLevel.DEBUG
    } else {
        LogLevel.INFO
    }
    KSLoggerDefaultPlatformLoggerLambda = fun(level, tag, message, throwable){
        if (throwable is CancellationException) return
        println(message)
        if (throwable != null) {
            println(throwable.stackTraceToString())
        }
    }
    setDefaultKSLog(
        KSLog(appName, minLoggingLevel = minLogLevel)
    )
}