package me.centralhardware.telegram.bot.common

import org.telegram.telegrambots.meta.api.objects.User

class ClickhouseRuben: BaseClickhouse() {

    fun log(text: String, isInline: Boolean, user: User?, botName: String){
        user?.let {
            insert(
                it.id,
                it.userName?.let { "@${it}" },
                it.firstName,
                it.lastName,
                it.isPremium ?: false,
                isInline,
                it.languageCode,
                text,
                botName)
        }
    }

}