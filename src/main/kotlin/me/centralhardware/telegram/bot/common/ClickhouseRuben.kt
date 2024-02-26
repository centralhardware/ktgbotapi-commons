package me.centralhardware.telegram.bot.common

import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.User

class ClickhouseRuben: BaseClickhouse() {

    private fun Update.text(): String{
        return when{
            this.hasMessage() -> this.message.text
            this.hasEditedMessage() -> "editMessage: ${this.editedMessage.messageId}"
            this.hasInlineQuery() -> "inline: ${this.inlineQuery.query}"
            this.hasChosenInlineQuery() -> "chosenInline: ${this.chosenInlineQuery.query}"
            this.hasCallbackQuery() -> "callback: ${this.callbackQuery.data}"
            else -> ""
        }
    }

    private fun Update.user(): User?{
        return when{
            this.hasMessage() -> this.message.from
            this.hasEditedMessage() -> this.editedMessage.from
            this.hasInlineQuery() -> this.inlineQuery.from
            this.hasChosenInlineQuery() -> this.chosenInlineQuery.from
            this.hasCallbackQuery() -> this.callbackQuery.from
            else -> null
        }
    }

    fun log(update: Update,botName: String){
        update.user()?.let {
            insert(
                it.id,
                it.userName?.let { "@${it}" },
                it.firstName,
                it.lastName,
                it.isPremium ?: false,
                update.hasInlineQuery(),
                it.languageCode,
                update.text(),
                botName)
        }
    }

}