package me.centralhardware.telegram.middleware

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SnowflakeIdsTest {
    @Test
    fun `ids are unique and increasing`() {
        val ids = List(100_000) { SnowflakeIds.next() }
        assertEquals(ids.size, ids.toSet().size)
        assertTrue(ids.zipWithNext().all { (a, b) -> b > a })
        assertTrue(ids.all { it > 0 })
    }

    @Test
    fun `unique across threads`() {
        val all = java.util.concurrent.ConcurrentHashMap.newKeySet<Long>()
        (1..8).map { Thread { repeat(20_000) { all.add(SnowflakeIds.next()) } } }
            .onEach { it.start() }
            .forEach { it.join() }
        assertEquals(160_000, all.size)
    }
}
