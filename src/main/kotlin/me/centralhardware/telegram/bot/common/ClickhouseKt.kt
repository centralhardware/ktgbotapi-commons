package me.centralhardware.telegram.bot.common

import dev.inmo.tgbotapi.types.chat.CommonUser

class ClickhouseKt: BaseClickhouse() {

    fun log(text: String, isInline: Boolean, user: CommonUser?, botName: String) {
        user?.let {
            insert(
                it.id.chatId,
                it.username?.full,
                it.firstName,
                it.lastName,
                it.isPremium,
                isInline,
                it.languageCode,
                text,
                botName)
        }
    }



}