package me.centralhardware.telegram.bot.common

import dev.inmo.tgbotapi.types.chat.CommonUser

class ClickhouseKt: BaseClickhouse() {

    suspend fun log(text: String, user: CommonUser?, botName: String, type: MessageType) {
        user?.let {
            insert(
                it.id.chatId,
                it.username?.full,
                it.firstName,
                it.lastName,
                it.isPremium,
                it.languageCode,
                text,
                botName,
                type)
        }
    }

}