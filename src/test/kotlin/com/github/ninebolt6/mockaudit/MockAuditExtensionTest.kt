package com.github.ninebolt6.mockaudit

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.*
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.platform.engine.discovery.DiscoverySelectors.selectClass
import org.junit.platform.testkit.engine.EngineTestKit
import org.junit.platform.testkit.engine.EventConditions.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MockAuditExtensionTest {
    @Test
    @DisplayName("正常系: stub設定->使用->verify でテストが成功すること")
    fun shouldPassWhenProperlyUsedAndVerified() {
        executeFixture(ProperUsageFixture::class.java)
            .assertThatEvents()
            .haveExactly(1, event(finishedSuccessfully()))
    }

    @Test
    @DisplayName("verify漏れ検出: stubを使用したがverifyしなかった場合、テストが失敗すること")
    fun shouldFailWhenVerifyForgotten() {
        executeFixture(VerifyForgottenFixture::class.java)
            .assertThatEvents()
            .haveExactly(1, event(finishedWithFailure()))
    }

    @Test
    @DisplayName("不要stub検出: stubを定義したが使用しなかった場合、テストが失敗すること")
    fun shouldFailWhenUnnecessaryStubDefined() {
        executeFixture(UnnecessaryStubFixture::class.java)
            .assertThatEvents()
            .haveExactly(1, event(finishedWithFailure()))
    }

    @Test
    @DisplayName("Nested対応: 外部クラスの@AuditMockフィールドも検証対象になること")
    fun shouldVerifyOuterClassMocksInNestedTest() {
        // 外部クラスのモックの verify を忘れたテストは失敗する
        executeFixture(NestedVerifyForgottenFixture::class.java)
            .assertThatEvents()
            .haveExactly(1, event(finishedWithFailure()))
    }

    @Test
    @DisplayName("Nested正常系: 外部・内部両方のモックを正しく使用すればテストが成功すること")
    fun shouldPassWhenBothOuterAndInnerMocksProperlyUsed() {
        executeFixture(NestedProperUsageFixture::class.java)
            .assertThatEvents()
            .haveExactly(1, event(finishedSuccessfully()))
    }

    @Test
    @DisplayName("多重Nested: 2階層以上のネストでも全ての外部クラスのモックが検証対象になること")
    fun shouldVerifyAllOuterClassMocksInDeeplyNestedTest() {
        // 最外部クラスのモックの verify を忘れたテストは失敗する
        executeFixture(DeeplyNestedVerifyForgottenFixture::class.java)
            .assertThatEvents()
            .haveExactly(1, event(finishedWithFailure()))
    }

    private fun executeFixture(fixtureClass: Class<*>) =
        EngineTestKit.engine("junit-jupiter")
            .configurationParameter(DEACTIVATE_DISABLED_CONDITION, DISABLED_CONDITION_CLASS)
            .selectors(selectClass(fixtureClass))
            .execute()
            .testEvents()

    companion object {
        // https://docs.junit.org/6.0.1/extensions/conditional-test-execution.html#deactivation
        private const val DEACTIVATE_DISABLED_CONDITION = "junit.jupiter.conditions.deactivate"
        private const val DISABLED_CONDITION_CLASS = "org.junit.*DisabledCondition"
    }
}

/** テスト用リポジトリインターフェース */
interface TestRepository {
    fun find(id: String): String?
    fun save(value: String)
}

/** 正常系: stub設定 -> 使用 -> verify */
@Disabled("EngineTestKit経由でのみ実行")
@ExtendWith(MockAuditExtension::class)
class ProperUsageFixture {
    @AuditMock
    private lateinit var mockRepository: TestRepository

    @BeforeEach
    fun setup() {
        mockRepository = mockk()
    }

    @Test
    fun test() {
        every { mockRepository.find("1") } returns "value"
        mockRepository.find("1")
        verify(exactly = 1) { mockRepository.find("1") }
    }
}

/** 異常系: 不要な stub */
@Disabled("EngineTestKit経由でのみ実行")
@ExtendWith(MockAuditExtension::class)
class UnnecessaryStubFixture {
    @AuditMock
    private lateinit var mockRepository: TestRepository

    @BeforeEach
    fun setup() {
        mockRepository = mockk()
    }

    @Test
    fun test() {
        every { mockRepository.find("1") } returns "value"
        // mockRepository.find() を呼ばない -> checkUnnecessaryStub で失敗すべき
    }
}

/** 異常系: verify 漏れ */
@Disabled("EngineTestKit経由でのみ実行")
@ExtendWith(MockAuditExtension::class)
class VerifyForgottenFixture {
    @AuditMock
    private lateinit var mockRepository: TestRepository

    @BeforeEach
    fun setup() {
        mockRepository = mockk()
    }

    @Test
    fun test() {
        every { mockRepository.find("1") } returns "value"
        mockRepository.find("1")
        // verify なし -> confirmVerified で失敗すべき
    }
}

/** Nested 正常系: 外部・内部両方を正しく使用 */
@Disabled("EngineTestKit経由でのみ実行")
@ExtendWith(MockAuditExtension::class)
class NestedProperUsageFixture {
    @AuditMock
    private lateinit var outerMock: TestRepository

    @BeforeEach
    fun setup() {
        outerMock = mockk()
    }

    @Nested
    inner class Inner {
        @AuditMock
        private lateinit var innerMock: TestRepository

        @BeforeEach
        fun setup() {
            innerMock = mockk()
        }

        @Test
        fun test() {
            // 外部クラスのモック
            every { outerMock.find("outer") } returns "outer-value"
            outerMock.find("outer")
            verify(exactly = 1) { outerMock.find("outer") }

            // 内部クラスのモック
            every { innerMock.save(any()) } returns Unit
            innerMock.save("inner")
            verify(exactly = 1) { innerMock.save("inner") }
        }
    }
}

/** Nested 異常系: 外部クラスのモックの verify 漏れ */
@Disabled("EngineTestKit経由でのみ実行")
@ExtendWith(MockAuditExtension::class)
class NestedVerifyForgottenFixture {
    @AuditMock
    private lateinit var outerMock: TestRepository

    @BeforeEach
    fun setup() {
        outerMock = mockk()
    }

    @Nested
    inner class Inner {
        @AuditMock
        private lateinit var innerMock: TestRepository

        @BeforeEach
        fun setup() {
            innerMock = mockk()
        }

        @Test
        fun test() {
            // 外部クラスのモック: 使用するが verify しない
            every { outerMock.find("outer") } returns "outer-value"
            outerMock.find("outer")
            // verify なし -> 失敗すべき

            // 内部クラスのモック: 正しく使用・検証
            every { innerMock.save(any()) } returns Unit
            innerMock.save("inner")
            verify(exactly = 1) { innerMock.save("inner") }
        }
    }
}

/** 多重 Nested 異常系: 最外部クラスのモックの verify 漏れ */
@Disabled("EngineTestKit経由でのみ実行")
@ExtendWith(MockAuditExtension::class)
class DeeplyNestedVerifyForgottenFixture {
    @AuditMock
    private lateinit var outerMock: TestRepository

    @BeforeEach
    fun setup() {
        outerMock = mockk()
    }

    @Nested
    inner class Middle {
        @AuditMock
        private lateinit var middleMock: TestRepository

        @BeforeEach
        fun setup() {
            middleMock = mockk()
        }

        @Nested
        inner class Inner {
            @AuditMock
            private lateinit var innerMock: TestRepository

            @BeforeEach
            fun setup() {
                innerMock = mockk()
            }

            @Test
            fun test() {
                // 最外部: 使用するが verify しない
                every { outerMock.find("outer") } returns "outer-value"
                outerMock.find("outer")
                // verify なし -> 失敗すべき

                // 中間: 正しく使用・検証
                every { middleMock.find("middle") } returns "middle-value"
                middleMock.find("middle")
                verify(exactly = 1) { middleMock.find("middle") }

                // 最内部: 正しく使用・検証
                every { innerMock.save(any()) } returns Unit
                innerMock.save("inner")
                verify(exactly = 1) { innerMock.save("inner") }
            }
        }
    }
}
