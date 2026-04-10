package com.perfkit.core.infrastructure

import com.perfkit.api.domain.ViolationCategory
import com.perfkit.api.domain.ViolationEvent
import com.perfkit.api.domain.ViolationSeverity
import com.perfkit.api.domain.ViolationSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CircularViolationBufferTest {

    // ------------------------------------------------------------------
    // Operações básicas
    // ------------------------------------------------------------------

    @Test
    fun `buffer starts empty`() {
        val buffer = CircularViolationBuffer(maxSize = 10)
        assertTrue(buffer.getAll().isEmpty())
    }

    @Test
    fun `added event is retrievable`() {
        val buffer = CircularViolationBuffer(maxSize = 10)
        val event = buildEvent("1")

        buffer.add(event)

        assertEquals(1, buffer.getAll().size)
        assertEquals("1", buffer.getAll().first().id)
    }

    @Test
    fun `multiple events are stored in insertion order`() {
        val buffer = CircularViolationBuffer(maxSize = 10)
        val ids = listOf("a", "b", "c")

        ids.forEach { buffer.add(buildEvent(it)) }

        assertEquals(ids, buffer.getAll().map { it.id })
    }

    // ------------------------------------------------------------------
    // Comportamento circular: maxSize
    // ------------------------------------------------------------------

    @Test
    fun `buffer does not exceed maxSize`() {
        val maxSize = 3
        val buffer = CircularViolationBuffer(maxSize = maxSize)

        repeat(5) { buffer.add(buildEvent(it.toString())) }

        assertEquals(maxSize, buffer.getAll().size)
    }

    @Test
    fun `oldest event is evicted when maxSize is exceeded`() {
        val buffer = CircularViolationBuffer(maxSize = 3)

        buffer.add(buildEvent("old-1"))
        buffer.add(buildEvent("old-2"))
        buffer.add(buildEvent("new-1"))
        buffer.add(buildEvent("new-2")) // deve evict "old-1"

        val ids = buffer.getAll().map { it.id }
        assertTrue("old-1 should have been evicted", "old-1" !in ids)
        assertTrue("old-2 should still be present", "old-2" in ids)
        assertTrue("new-2 should be present", "new-2" in ids)
    }

    @Test
    fun `buffer retains last maxSize events in order`() {
        val buffer = CircularViolationBuffer(maxSize = 3)

        (1..6).forEach { buffer.add(buildEvent(it.toString())) }

        assertEquals(listOf("4", "5", "6"), buffer.getAll().map { it.id })
    }

    // ------------------------------------------------------------------
    // Clear
    // ------------------------------------------------------------------

    @Test
    fun `clear removes all events`() {
        val buffer = CircularViolationBuffer(maxSize = 10)
        repeat(5) { buffer.add(buildEvent(it.toString())) }

        buffer.clear()

        assertTrue(buffer.getAll().isEmpty())
    }

    @Test
    fun `buffer is usable after clear`() {
        val buffer = CircularViolationBuffer(maxSize = 10)
        buffer.add(buildEvent("before-clear"))
        buffer.clear()
        buffer.add(buildEvent("after-clear"))

        assertEquals(1, buffer.getAll().size)
        assertEquals("after-clear", buffer.getAll().first().id)
    }

    // ------------------------------------------------------------------
    // getAll retorna snapshot imutável
    // ------------------------------------------------------------------

    @Test
    fun `getAll returns independent snapshot`() {
        val buffer = CircularViolationBuffer(maxSize = 10)
        buffer.add(buildEvent("snap-1"))

        val snapshot = buffer.getAll()
        buffer.add(buildEvent("snap-2"))

        assertEquals("Snapshot must not reflect subsequent additions", 1, snapshot.size)
    }

    // ------------------------------------------------------------------
    // Helper
    // ------------------------------------------------------------------

    private fun buildEvent(id: String) = ViolationEvent(
        id = id,
        timestamp = System.currentTimeMillis(),
        source = ViolationSource.THREAD_POLICY,
        category = ViolationCategory.DISK_READ,
        severity = ViolationSeverity.MEDIUM,
        threadName = "test",
        message = "test",
        stacktrace = null,
        className = "DiskReadViolation",
        policyLabel = "ThreadPolicy",
    )
}
