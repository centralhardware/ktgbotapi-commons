package dev.inmo.tgbotapi

import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.warning

val KSLogExceptionsHandler = { t: Throwable -> KSLog.warning("", t) }
val botToken = System.getenv("BOT_TOKEN")