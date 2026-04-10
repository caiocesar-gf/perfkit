package com.perfkit.core.domain

import com.perfkit.api.domain.ViolationCategory
import com.perfkit.api.domain.ViolationSeverity
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Testes unitários para [DefaultViolationClassifier.classifyByClassName] —
 * o path legado (API 24–27), testável sem o framework Android.
 *
 * O path moderno (API 28+) depende de [android.os.strictmode.*] que exigiria
 * Robolectric; coberto por testes instrumentados em módulo separado se necessário.
 */
class DefaultViolationClassifierTest {

    // Instancia com sdkVersion=24 para forçar o path classifyByClassName
    private val classifier = DefaultViolationClassifier(sdkVersion = 24)

    // ------------------------------------------------------------------
    // Thread Policy violations
    // ------------------------------------------------------------------

    @Test
    fun `DiskReadViolation maps to DISK_READ with MEDIUM severity`() {
        val result = classifier.classifyByClassName("DiskReadViolation")
        assertEquals(ViolationCategory.DISK_READ, result.category)
        assertEquals(ViolationSeverity.MEDIUM, result.severity)
    }

    @Test
    fun `DiskWriteViolation maps to DISK_WRITE with MEDIUM severity`() {
        val result = classifier.classifyByClassName("DiskWriteViolation")
        assertEquals(ViolationCategory.DISK_WRITE, result.category)
        assertEquals(ViolationSeverity.MEDIUM, result.severity)
    }

    @Test
    fun `NetworkViolation maps to NETWORK with HIGH severity`() {
        val result = classifier.classifyByClassName("NetworkViolation")
        assertEquals(ViolationCategory.NETWORK, result.category)
        assertEquals(ViolationSeverity.HIGH, result.severity)
    }

    @Test
    fun `SlowCallViolation maps to SLOW_CALL with MEDIUM severity`() {
        val result = classifier.classifyByClassName("SlowCallViolation")
        assertEquals(ViolationCategory.SLOW_CALL, result.category)
        assertEquals(ViolationSeverity.MEDIUM, result.severity)
    }

    // ------------------------------------------------------------------
    // VM Policy violations
    // ------------------------------------------------------------------

    @Test
    fun `LeakedClosableViolation maps to LEAKED_RESOURCE with HIGH severity`() {
        val result = classifier.classifyByClassName("LeakedClosableViolation")
        assertEquals(ViolationCategory.LEAKED_RESOURCE, result.category)
        assertEquals(ViolationSeverity.HIGH, result.severity)
    }

    @Test
    fun `LeakedRegistrationObjectsViolation maps to LEAKED_RESOURCE`() {
        val result = classifier.classifyByClassName("LeakedRegistrationObjectsViolation")
        assertEquals(ViolationCategory.LEAKED_RESOURCE, result.category)
    }

    @Test
    fun `LeakedSqlLiteObjectsViolation maps to LEAKED_RESOURCE`() {
        val result = classifier.classifyByClassName("LeakedSqlLiteObjectsViolation")
        assertEquals(ViolationCategory.LEAKED_RESOURCE, result.category)
    }

    @Test
    fun `CleartextNetworkViolation maps to CLEARTEXT_NETWORK with CRITICAL severity`() {
        val result = classifier.classifyByClassName("CleartextNetworkViolation")
        assertEquals(ViolationCategory.CLEARTEXT_NETWORK, result.category)
        assertEquals(ViolationSeverity.CRITICAL, result.severity)
    }

    @Test
    fun `ResourceMismatchViolation maps to RESOURCE_MISMATCH`() {
        val result = classifier.classifyByClassName("ResourceMismatchViolation")
        assertEquals(ViolationCategory.RESOURCE_MISMATCH, result.category)
    }

    @Test
    fun `UntaggedSocketViolation maps to UNTAGGED_SOCKET with LOW severity`() {
        val result = classifier.classifyByClassName("UntaggedSocketViolation")
        assertEquals(ViolationCategory.UNTAGGED_SOCKET, result.category)
        assertEquals(ViolationSeverity.LOW, result.severity)
    }

    @Test
    fun `CustomViolation maps to CUSTOM`() {
        val result = classifier.classifyByClassName("CustomViolation")
        assertEquals(ViolationCategory.CUSTOM, result.category)
    }

    @Test
    fun `unknown class name maps to UNKNOWN with LOW severity`() {
        val result = classifier.classifyByClassName("SomeFutureAndroidViolation")
        assertEquals(ViolationCategory.UNKNOWN, result.category)
        assertEquals(ViolationSeverity.LOW, result.severity)
    }

    @Test
    fun `classification is case-insensitive`() {
        // Os subtipos de Violation têm CamelCase — mas garantimos robustez
        assertEquals(ViolationCategory.DISK_READ, classifier.classifyByClassName("diskreadviolation").category)
        assertEquals(ViolationCategory.NETWORK, classifier.classifyByClassName("NETWORKVIOLATION").category)
    }
}
