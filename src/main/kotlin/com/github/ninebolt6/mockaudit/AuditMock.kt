package com.github.ninebolt6.mockaudit

/**
 * Annotation to mark mock fields for verification by [MockAuditExtension].
 *
 * Apply this annotation to mock fields that should be verified after each test.
 * The extension will check for:
 * - Unnecessary stub definitions (stubs that were never used)
 * - Unverified mock interactions (calls that were not explicitly verified)
 *
 * Usage:
 * ```kotlin
 * @ExtendWith(MockAuditExtension::class)
 * class MyTest {
 *     @AuditMock
 *     private val myMock: MyService = mockk()
 *
 *     @Test
 *     fun `test something`() {
 *         every { myMock.doSomething() } returns "result"
 *         // ... test code ...
 *         verify { myMock.doSomething() }
 *     }
 * }
 * ```
 *
 * @see MockAuditExtension
 */
@Target(AnnotationTarget.FIELD)
@Retention(AnnotationRetention.RUNTIME)
annotation class AuditMock
