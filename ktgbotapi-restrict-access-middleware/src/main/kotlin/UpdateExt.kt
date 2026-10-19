import dev.inmo.micro_utils.common.Warning
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.from
import dev.inmo.tgbotapi.types.update.*
import dev.inmo.tgbotapi.types.update.abstracts.Update
import dev.inmo.tgbotapi.utils.RiskFeature

/**
 * The id of the user behind a Telegram update, or null when the update carries no user
 * (channel posts, polls, reaction counts, deleted business messages, unknown updates).
 *
 * Kept in step with `extractUser` in the ClickHouse logging middleware.
 */
@OptIn(Warning::class, RiskFeature::class)
fun Update.chatId(): Long? {
    val user = when (this) {
        is MessageUpdate -> data.from
        is EditMessageUpdate -> data.from
        is ChannelPostUpdate -> data.from
        is EditChannelPostUpdate -> data.from
        is InlineQueryUpdate -> data.from
        is CallbackQueryUpdate -> data.from
        is ChosenInlineResultUpdate -> data.from
        is ShippingQueryUpdate -> data.from
        is PreCheckoutQueryUpdate -> data.from
        is BusinessMessageUpdate -> data.from
        is EditBusinessMessageUpdate -> data.from
        is MyChatMemberUpdatedUpdate -> data.user
        is CommonChatMemberUpdatedUpdate -> data.user
        is ChatJoinRequestUpdate -> data.user
        is BusinessConnectionUpdate -> data.user
        is PaidMediaPurchasedUpdate -> data.from
        is PollAnswerUpdate -> data.user
        is ChatMessageReactionUpdatedUpdate -> data.reactedUser
        is ChatBoostUpdatedUpdate -> data.boost.source.user
        is ChatBoostRemovedUpdate -> data.source.user
        is ManagedBotUpdate -> data.user
        else -> null
    }
    return user?.id?.chatId?.long
}
