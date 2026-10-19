package me.centralhardware.telegram.conversation

import kotlinx.coroutines.Job
import java.util.concurrent.ConcurrentHashMap

/**
 * Tracks which users currently have a running conversation, so a bot can refuse to start a
 * second concurrent flow for the same user and cancel a running one on demand.
 *
 * Conversations are launched and registered through [startConversation]; this object is the
 * shared, process-wide registry behind it.
 */
object ConversationState {

    private val active = ConcurrentHashMap<Long, Job>()

    fun hasActive(userId: Long): Boolean = active.containsKey(userId)

    /** Register [job] for [userId], atomically; returns false if one was already running. */
    fun start(userId: Long, job: Job): Boolean = active.putIfAbsent(userId, job) == null

    /** Unregister [job]; a no-op when another job holds [userId] (e.g. a rejected second start). */
    fun end(userId: Long, job: Job) {
        active.remove(userId, job)
    }

    /** Cancel the running conversation for [userId], if any. Returns true when one was cancelled. */
    fun cancel(userId: Long): Boolean {
        val job = active.remove(userId) ?: return false
        job.cancel()
        return true
    }
}
