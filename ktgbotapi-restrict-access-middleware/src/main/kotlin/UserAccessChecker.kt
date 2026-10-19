/**
 * Interface for checking if a user has access to the bot.
 */
interface UserAccessChecker {

    /**
     * Checks if a user with the given ID has access to the bot.
     *
     * @param userId The Telegram user ID to check, or null when the update carries no user
     *        (e.g. a channel post or a poll). Return false for null unless such updates are wanted.
     * @return true if the user has access, false otherwise.
     */
    fun checkAccess(userId: Long?): Boolean
}