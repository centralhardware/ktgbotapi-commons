package dev.inmo.kslog.common

import org.apache.commons.lang3.BooleanUtils
import org.apache.commons.lang3.StringUtils

fun KSLog.configure(appName: String) {
    val minLogLevel = if (BooleanUtils.toBooleanObject(System.getenv("DEBUG") ?: "false")) {
        LogLevel.DEBUG
    } else {
        LogLevel.INFO
    }
    KSLoggerDefaultPlatformLoggerLambda = fun(_, _, message, throwable){
        if (StringUtils.isBlank(message.toString()) && throwable == null) {
            return
        }
        println(message)
        if (throwable != null) {
            println(throwable.stackTraceToString())
        }
    }
    setDefaultKSLog(
        KSLog(appName, minLoggingLevel = minLogLevel)
    )
}