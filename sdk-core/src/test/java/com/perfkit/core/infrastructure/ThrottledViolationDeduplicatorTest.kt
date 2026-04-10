package com.perfkit.core.infrastructure

import com.perfkit.api.domain.ViolationCategory
import com.perfkit.api.domain.ViolationEvent
import com.perfkit.api.domain.ViolationSeverity
import com.perfkit.api.domain.ViolationSource
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThrottledViolationDeduplicatorTest {

    private val dedupWindowMs = 2_000L
    private val deduplicator = ThrottledViolationDeduplicator(dedupWindowMs)

    // ------------------------------------------------------------------
    // Primeira emissão sempre passa
    // ------------------------------------------------------------------

    @Test
    fun `first emission of any event is always allowed`() {
        val event = buildEvent(category = ViolationCategory.DISK_READ)
        assertTrue(deduplicator.shouldEmit(event))
    }

    // ------------------------------------------------------------------
    // Deduplicação dentro da janela
    // ------------------------------------------------------------------

    @Test
    fun `identical event within dedup window is rejected`() {
        val event = buildEvent(category = ViolationCategory.NETWORK, stacktrace = "at com.example.Foo.bar(Foo.kt:10)")

        assertTrue("First emission must pass", deduplicator.shouldEmit(event))
        assertFalse("Second emission within window must be rejected", deduplicator.shouldEmit(event))
    }

    @Test
    fun `event with different category is not deduplicated`() {
        val diskEvent = buildEvent(category = ViolationCategory.DISK_READ)
        val networkEvent = buildEvent(category = ViolationCategory.NETWORK)

        assertTrue(deduplicator.shouldEmit(diskEvent))
        assertTrue("Different category must not be blocked", deduplicator.shouldEmit(networkEvent))
    }

    @Test
    fun `event with different stacktrace is not deduplicated`() {
        val eventA = buildEvent(category = ViolationCategory.DISK_READ, stacktrace = "at Foo.bar(Foo.kt:10)")
        val eventB = buildEvent(category = ViolationCategory.DISK_READ, stacktrace = "at Baz.qux(Baz.kt:42)")

        assertTrue(deduplicator.shouldEmit(eventA))
        assertTrue("Different stacktrace must not be blocked", deduplicator.shouldEmit(eventB))
    }

    // ------------------------------------------------------------------
    // Expiração da janela de deduplicação
    // ------------------------------------------------------------------

    @Test
    fun `same event after dedup window has expired is allowed again`() {
        // Cria deduplicator com janela de 100ms para testar expiração
        val shortWindowDeduplicator = ThrottledViolationDeduplicator(dedupWindowMs = 100L)
        val event = buildEvent(category = ViolationCategory.SLOW_CALL, stacktrace = "at Slow.doWork(Slow.kt:5)")

        assertTrue(shortWindowDeduplicator.shouldEmit(event))
        assertFalse("Must be rejected within window", shortWindowDeduplicator.shouldEmit(event))

        Thread.sleep(150) // aguarda expiração da janela

        assertTrue("Must be allowed after window expires", shortWindowDeduplicator.shouldEmit(event))
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private fun buildEvent(
        category: ViolationCategory,
        stacktrace: String? = null,
    ) = ViolationEvent(
        id = "test-id",
        timestamp = System.currentTimeMillis(),
        source = ViolationSource.THREAD_POLICY,
        category = category,
        severity = ViolationSeverity.MEDIUM,
        threadName = "main",
        message = "test message",
        stacktrace = stacktrace,
        className = "${category.name}Violation",
        policyLabel = "ThreadPolicy",
    )
}
