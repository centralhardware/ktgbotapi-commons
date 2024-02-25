package me.centralhardware.telegram.bot.common

import org.telegram.telegrambots.meta.api.objects.User

class ClickhouseRuben: BaseClickhouse() {

    fun log(text: String, isInline: Boolean, user: User?, botName: String){
        user?.let {
            insert(
                it.id,
                if (it.userName != null) "@${it.userName}" else null,
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