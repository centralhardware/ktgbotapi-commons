package me.centralhardware.telegram.bot.common

import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.User

class ClickhouseRuben: BaseClickhouse() {

    private fun Message.text(): String{
        return when{
            this.hasText() -> "text" + this.text
            this.hasDice() -> "dice" + this.dice.emoji
            this.hasPoll() -> "poll" + this.poll.question
            this.hasDocument() -> "document" + this.document.fileId
            this.hasAudio() -> "audio" + this.audio.fileId
            this.hasAnimation() -> "animation" + this.animation.fileId
            this.hasContact() -> "contact: ${this.contact.userId} ${this.contact.firstName} ${this.contact.lastName}"
            this.hasInvoice() -> "invoice: ${this.invoice.title} ${this.invoice.description} ${this.invoice.currency} ${this.invoice.totalAmount}"
            this.hasLocation() -> "location: ${this.location.latitude} ${this.location.longitude} ${this.location.heading}"
            this.hasPhoto() -> "photo" + this.photo.sortedBy { it.fileSize }.map { it.fileId }.first().orEmpty()
            this.hasPassportData() -> "passportdata: hided"
            this.hasVoice() -> "voice" + this.voice.fileId
            this.hasVideoNote() -> "videoNote" + this.videoNote.fileId
            this.hasSuccessfulPayment() -> "successfullPayment:" + this.successfulPayment.telegramPaymentChargeId
            else -> ""
        }
    }

    private fun Update.text(): String{
        return when{
            this.hasMessage() -> this.message.text()
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