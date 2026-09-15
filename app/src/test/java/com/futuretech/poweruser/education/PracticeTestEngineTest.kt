package com.futuretech.poweruser.education

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PracticeTestEngineTest {
    @Test
    fun extractsStringsNumbersAndBooleansWithoutReadingComments() {
        val code = """
            name = 'alpha'
            count = -3
            enabled = True
            # ignored = 'comment'
            print(name, count, enabled)
        """.trimIndent()

        val values = PracticeTestEngine.extractEditableLiterals(code)

        assertTrue(values.any { it.kind == EditableLiteralKind.STRING && it.displayValue == "alpha" })
        assertTrue(values.any { it.kind == EditableLiteralKind.NUMBER && it.raw == "-3" })
        assertTrue(values.any { it.kind == EditableLiteralKind.BOOLEAN && it.raw == "True" })
        assertFalse(values.any { it.raw.contains("comment") })
    }

    @Test
    fun userOverrideChangesOnlyScratchCopyAndPreservesLiteralType() {
        val original = "price = 10\nprint(price)"
        val literal = PracticeTestEngine.extractEditableLiterals(original).first()

        val changed = PracticeTestEngine.applyUserOverride(original, literal, "-5")

        assertNotNull(changed)
        assertEquals("price = 10\nprint(price)", original)
        assertEquals("price = -5\nprint(price)", changed)
        assertEquals(null, PracticeTestEngine.applyUserOverride(original, literal, "not-a-number"))
    }

    @Test
    fun emptyStringIsAValidCustomRunValue() {
        val original = "name = 'hello'\nprint(name)"
        val literal = PracticeTestEngine.extractEditableLiterals(original).first()

        val changed = PracticeTestEngine.applyUserOverride(original, literal, "")

        assertEquals("name = ''\nprint(name)", changed)
    }

    @Test
    fun hiddenEdgeVariantsCoverEmptyDuplicateAndLongStringWithoutExposingInEvidence() {
        val variants = PracticeTestEngine.edgeVariants("name = 'x'\nprint(name)")

        assertEquals(3, variants.size)
        assertTrue(variants.any { it.code.contains("name = ''") })
        assertTrue(variants.all { it.code != "name = 'x'\nprint(name)" })
        assertEquals(
            "숨은 경계조건에서 실패했습니다. 입력값 자체는 공개하지 않습니다.",
            PracticeTestEngine.hiddenEvidence(false)
        )
    }

    @Test
    fun numericHiddenVariantsCoverZeroNegativeAndLargeValue() {
        val variants = PracticeTestEngine.edgeVariants("value = 7\nprint(value)")
            .map { it.code }

        assertTrue(variants.any { it.contains("value = 0") })
        assertTrue(variants.any { it.contains("value = -1") })
        assertTrue(variants.any { it.contains("value = 999999") })
    }

    @Test
    fun pairedHiddenVariantsApplySameEdgeValueToLearnerAndReference() {
        val learner = "value = 7\nprint(value)"
        val reference = "value = 7\nprint(value)"
        val variants = PracticeTestEngine.pairedEdgeVariants(learner, reference)

        assertEquals(3, variants.size)
        variants.forEach {
            assertEquals(
                PracticeTestEngine.normalizeOutput(it.learnerCode),
                PracticeTestEngine.normalizeOutput(it.referenceCode)
            )
        }
    }

    @Test
    fun normalizeOutputIgnoresOnlyLineEndingAndTrailingWhitespace() {
        val left = "a  \r\nb\r\n"
        val right = "a\nb"

        assertEquals(
            PracticeTestEngine.normalizeOutput(left),
            PracticeTestEngine.normalizeOutput(right)
        )
        assertNotEquals(
            PracticeTestEngine.normalizeOutput("a b"),
            PracticeTestEngine.normalizeOutput("ab")
        )
    }

    @Test
    fun submissionPassRequiresEveryPublicAndHiddenTest() {
        val pass = PracticeSubmissionResult(
            listOf(
                PracticeTestCaseResult("p", PracticeTestVisibility.PUBLIC, "p", true, "ok"),
                PracticeTestCaseResult("h", PracticeTestVisibility.HIDDEN, "h", true, "ok")
            )
        )
        val fail = PracticeSubmissionResult(
            listOf(
                PracticeTestCaseResult("p", PracticeTestVisibility.PUBLIC, "p", true, "ok"),
                PracticeTestCaseResult("h", PracticeTestVisibility.HIDDEN, "h", false, "hidden")
            )
        )

        assertTrue(pass.passed)
        assertFalse(fail.passed)
        assertEquals(1, pass.publicPassed)
        assertEquals(1, pass.hiddenPassed)
    }
}
