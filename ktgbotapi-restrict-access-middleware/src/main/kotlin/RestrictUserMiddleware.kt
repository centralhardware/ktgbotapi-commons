import dev.inmo.kslog.common.KSLog
import dev.inmo.kslog.common.info
import dev.inmo.micro_utils.common.Warning
import dev.inmo.tgbotapi.bot.ktor.middlewares.TelegramBotMiddlewaresPipelinesHandler
import dev.inmo.tgbotapi.types.update.abstracts.UnknownUpdate
import dev.inmo.tgbotapi.types.update.abstracts.Update
import kotlinx.serialization.json.JsonObject

/**
 * Adds a middleware to the bot that restricts user access.
 *
 * This middleware intercepts all updates from Telegram and checks if the user who sent the update
 * is allowed to access the bot. If the user is not allowed, the update is replaced with an
 * [UnknownUpdate] containing an [AccessDeniedException], effectively preventing the bot from
 * processing the update.
 *
 * An update with no resolvable user is passed to the checker as `null`, so the checker decides;
 * [EnvironmentVariableUserAccessChecker] denies it (fail closed).
 *
 * @param accessChecker The [UserAccessChecker] implementation to use for checking user access.
 */
@OptIn(Warning::class)
fun TelegramBotMiddlewaresPipelinesHandler.Builder.restrictAccess(
    accessChecker: UserAccessChecker
) {
    addMiddleware {
        doOnRequestResultPresented { result, _, _, _ ->
            when (result) {
                !is ArrayList<*> -> result

                else -> {
                    @Suppress("UNCHECKED_CAST")
                    (result as? ArrayList<Update>)?.map { update -> filterUpdate(update, accessChecker) }
                        ?: result
                }
            }
        }
    }
    KSLog.info("Initialize restrict access middleware with access checker: ${accessChecker::class.simpleName}")
}

/** [update] itself when [accessChecker] lets its user in, a denied placeholder otherwise. */
internal fun filterUpdate(update: Update, accessChecker: UserAccessChecker): Update {
    val userId = update.chatId()
    if (accessChecker.checkAccess(userId)) return update
    KSLog.info("Filtering out ${update::class.simpleName} from unauthorized user ${userId ?: "(none)"}")
    return UnknownUpdate(update.updateId, JsonObject(mapOf()), AccessDeniedException())
}
