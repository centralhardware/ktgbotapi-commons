import kotlinx.coroutines.Job
import me.centralhardware.telegram.conversation.ConversationState
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConversationStateTest {
    @Test
    fun `a rejected second start does not unregister the first`() {
        val user = 101L
        val first = Job()
        val second = Job()
        assertTrue(ConversationState.start(user, first))
        assertFalse(ConversationState.start(user, second))
        ConversationState.end(user, second) // the rejected job's cleanup
        assertTrue(ConversationState.hasActive(user))
        assertTrue(ConversationState.cancel(user))
        assertTrue(first.isCancelled)
        assertFalse(ConversationState.hasActive(user))
    }

    @Test
    fun `end frees the user for a new conversation`() {
        val user = 102L
        val job = Job()
        assertTrue(ConversationState.start(user, job))
        ConversationState.end(user, job)
        assertFalse(ConversationState.hasActive(user))
        assertTrue(ConversationState.start(user, Job()))
        ConversationState.cancel(user)
    }

    @Test
    fun `concurrent starts let exactly one through`() {
        val user = 103L
        val wins = java.util.concurrent.atomic.AtomicInteger()
        (1..16).map { Thread { if (ConversationState.start(user, Job())) wins.incrementAndGet() } }
            .onEach { it.start() }
            .forEach { it.join() }
        kotlin.test.assertEquals(1, wins.get())
        ConversationState.cancel(user)
    }
}
