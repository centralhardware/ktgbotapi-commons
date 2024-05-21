package me.centralhardware.telegram.bot.common

import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.User
import org.telegram.telegrambots.meta.api.objects.message.Message

class ClickhouseRuben: BaseClickhouse() {

    private fun Message.text(): String{
        return when{
            this.hasText() -> this.text
            this.hasDice() -> this.dice.emoji
            this.hasPoll() -> this.poll.question
            this.hasDocument() -> this.document.fileId
            this.hasAudio() -> this.audio.fileId
            this.hasAnimation() -> this.animation.fileId
            this.hasContact() -> "${this.contact.userId} ${this.contact.firstName} ${this.contact.lastName}"
            this.hasLocation() -> "${this.location.latitude} ${this.location.longitude} ${this.location.heading}"
            this.hasPhoto() -> this.photo.sortedBy { it.fileSize }.map { it.fileId }.first().orEmpty()
            this.hasVoice() -> this.voice.fileId
            this.hasVideoNote() -> this.videoNote.fileId
            this.hasSticker() -> "${this.sticker.emoji} ${this.sticker.setName}$"
            else -> ""
        }
    }

    private fun Message.type(): MessageType?{
        return when{
            this.hasText() -> MessageType.TEXT
            this.hasDice() ->  MessageType.DICE
            this.hasPoll() ->  MessageType.POLL
            this.hasDocument() ->  MessageType.DOCUMENT
            this.hasAudio() ->  MessageType.AUDIO
            this.hasAnimation() ->  MessageType.ANIMATION
            this.hasContact() ->  MessageType.CONTACT
            this.hasLocation() ->  MessageType.LOCATION
            this.hasPhoto() ->  MessageType.PHOTO
            this.hasVoice() ->  MessageType.VOICE
            this.hasVideoNote() ->  MessageType.VIDEO_NOTE
            this.hasSticker() ->  MessageType.STICKER
            else -> null
        }
    }

    private fun Update.text(): String{
        return when{
            this.hasMessage() -> this.message.text()
            this.hasEditedMessage() -> this.editedMessage.messageId.toString()
            this.hasInlineQuery() -> this.inlineQuery.query
            this.hasChosenInlineQuery() -> this.chosenInlineQuery.query
            this.hasCallbackQuery() -> this.callbackQuery.data
            else -> ""
        }
    }

    private fun Update.type(): MessageType?{
        return when{
            this.hasMessage() -> this.message.type()
            this.hasEditedMessage() -> MessageType.EDIT_MESSAGE
            this.hasInlineQuery() -> MessageType.INLINE
            this.hasChosenInlineQuery() -> MessageType.CHOSEN_INLINE
            this.hasCallbackQuery() -> MessageType.CALLBACK
            else -> null
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
        if (update.type() == null) return

        update.user()?.let {
            insert(
                it.id,
                it.userName?.let { "@${it}" },
                it.firstName,
                it.lastName,
                it.isPremium ?: false,
                it.languageCode,
                update.text(),
                botName,
                update.type()!!)
        }
    }
}