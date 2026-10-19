import dev.inmo.tgbotapi.types.UpdateId
import dev.inmo.tgbotapi.types.update.abstracts.UnknownUpdate
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class RestrictAccessTest {
    @Test
    fun `allowed users parsing`() {
        val checker = EnvironmentVariableUserAccessChecker(" 1, 2 ,abc,, 3 ")
        assertTrue(checker.checkAccess(1))
        assertTrue(checker.checkAccess(3))
        assertFalse(checker.checkAccess(4))
        assertFalse(checker.checkAccess(null), "an update with no user is denied")
        assertFalse(EnvironmentVariableUserAccessChecker(null).checkAccess(1))
        assertFalse(EnvironmentVariableUserAccessChecker("  ").checkAccess(1))
    }

    @Test
    fun `an update without a user fails closed`() {
        val update = UnknownUpdate(UpdateId(1), JsonObject(mapOf()), null)
        val filtered = filterUpdate(update, EnvironmentVariableUserAccessChecker("1"))
        assertTrue(filtered is UnknownUpdate && filtered.throwable is AccessDeniedException)
    }

    @Test
    fun `a checker that allows null lets it through`() {
        val update = UnknownUpdate(UpdateId(1), JsonObject(mapOf()), null)
        val open = object : UserAccessChecker { override fun checkAccess(userId: Long?) = true }
        assertSame(update, filterUpdate(update, open))
    }
}
