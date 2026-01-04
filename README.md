# MockAuditExtension

A JUnit 5 extension for MockK that automatically detects unused stubs and unverified mocks, helping you maintain thorough test coverage and catch incomplete mock verification.

## Installation

### Gradle (Kotlin DSL)

```kotlin
dependencies {
    testImplementation("com.github.ninebolt6:mock-audit-extension:0.0.1")
}
```

### Maven

```xml
<dependency>
    <groupId>com.github.ninebolt6</groupId>
    <artifactId>mock-audit-extension</artifactId>
    <version>0.0.1</version>
    <scope>test</scope>
</dependency>
```

## Usage

Add the extension to your test class and annotate mocks with `@AuditMock`:

```kotlin
import com.github.ninebolt6.mockaudit.AuditMock
import com.github.ninebolt6.mockaudit.MockAuditExtension
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockAuditExtension::class)
class MyServiceTest {
    @AuditMock
    private val repository: Repository = mockk()

    @Test
    fun `should fetch data`() {
        // Arrange
        every { repository.getData() } returns "test data"

        // Act
        val result = repository.getData()

        // Assert
        verify { repository.getData() }
    }
}
```

The extension automatically verifies after each test that:
- All stubs were actually used
- All mock calls were verified

## What It Catches

- **Unused stubs**: Stub definitions that were never called during the test
- **Unverified mocks**: Mock calls that weren't explicitly verified

This helps prevent incomplete test coverage and ensures your mocks are properly verified.

## License

This project is licensed under the Eclipse Public License 2.0.
