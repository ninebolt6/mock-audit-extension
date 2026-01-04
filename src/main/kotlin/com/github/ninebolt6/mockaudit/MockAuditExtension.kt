package com.github.ninebolt6.mockaudit

import io.mockk.checkUnnecessaryStub
import io.mockk.confirmVerified
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import java.lang.reflect.Modifier

/**
 * JUnit 5 Extension that performs mock verification after each test.
 *
 * This extension automatically verifies mocks annotated with [@AuditMock][AuditMock] after each test:
 * - [checkUnnecessaryStub]: Detects stub definitions that were never used during the test
 * - [confirmVerified]: Detects mock calls that were not explicitly verified
 *
 * The extension recursively collects mocks from nested classes and inner classes,
 * making it compatible with JUnit 5's [@Nested][org.junit.jupiter.api.Nested] test structure.
 *
 * Usage:
 * ```kotlin
 * @ExtendWith(MockAuditExtension::class)
 * class MyTest {
 *     @AuditMock
 *     private val repository: UserRepository = mockk()
 *
 *     @Test
 *     fun `should call repository`() {
 *         every { repository.findById(1) } returns User(1, "test")
 *
 *         val result = repository.findById(1)
 *
 *         verify { repository.findById(1) }
 *         // Extension will verify no other interactions and no unused stubs
 *     }
 *
 *     @Nested
 *     inner class NestedTest {
 *         @AuditMock
 *         private val service: UserService = mockk()
 *
 *         @Test
 *         fun `nested test`() {
 *             // Both 'repository' and 'service' mocks will be verified
 *         }
 *     }
 * }
 * ```
 *
 * @see AuditMock
 * @see checkUnnecessaryStub
 * @see confirmVerified
 */
class MockAuditExtension : AfterEachCallback {
    override fun afterEach(context: ExtensionContext) {
        val testInstance = context.requiredTestInstance
        val mocks = collectMocks(testInstance.javaClass, testInstance)

        if (mocks.isNotEmpty()) {
            checkUnnecessaryStub(*mocks.toTypedArray())
            confirmVerified(*mocks.toTypedArray())
        }
    }

    /**
     * Collects mocks annotated with [@AuditMock][AuditMock] from the given class and its enclosing classes.
     *
     * For inner classes (non-static member classes), this method recursively collects
     * mocks from the outer class as well, enabling proper verification in nested test structures.
     *
     * @param clazz The class to collect mocks from
     * @param instance The instance of the class
     * @return List of mock objects annotated with @AuditMock
     */
    private fun collectMocks(clazz: Class<*>, instance: Any): List<Any> {
        // Collect @AuditMock fields from this class
        val directMocks = clazz.declaredFields
            .filter { it.isAnnotationPresent(AuditMock::class.java) }
            .mapNotNull { field ->
                field.isAccessible = true
                field.get(instance)
            }

        // For inner classes, recursively collect mocks from the enclosing class
        val outerMocks = clazz.enclosingClass
            ?.takeIf { clazz.isMemberClass && !Modifier.isStatic(clazz.modifiers) }
            ?.let { enclosingClass ->
                val outerThis = clazz.getDeclaredField("this$0")
                    .apply { isAccessible = true }
                val outerInstance = outerThis.get(instance)
                collectMocks(enclosingClass, outerInstance)
            }
            ?: emptyList()

        return directMocks + outerMocks
    }
}
