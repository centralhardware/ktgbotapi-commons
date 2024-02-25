package me.centralhardware.telegram.bot.common

import dev.inmo.tgbotapi.types.chat.CommonUser

class ClickhouseKt: BaseClickhouse() {

    fun log(text: String, isInline: Boolean, user: CommonUser?, botName: String) {
        user?.let {
            insert(
                it.id.chatId,
                if (it.username != null) it.username!!.full else null,
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