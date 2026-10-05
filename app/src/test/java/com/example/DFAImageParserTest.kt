package com.example

import com.example.ocr.DFAImageParser
import com.example.ocr.ParseStatus
import org.junit.Assert.*
import org.junit.Test

class DFAImageParserTest {

    @Test
    fun testProperDFAImage_Detected() {
        val ocrText = """
            States: q0, q1, q2
            Alphabet: a, b
            Start: q0
            Final: q2
            
            q0, a = q1
            q0, b = q0
            q1, a = q2
            q1, b = q0
            q2, a = q2
            q2, b = q2
        """.trimIndent()

        val result = DFAImageParser.parse(ocrText)
        assertEquals("Status was: ${result.status}, msg: ${result.message}, conf: ${result.confidence}, states: ${result.detectedStates}, alpha: ${result.detectedAlphabet}, start: ${result.detectedStart}, finals: ${result.detectedFinals}, trans: ${result.detectedTransitions.size}", ParseStatus.DETECTED, result.status)
        assertNotNull(result.dfa)
        assertTrue(result.confidence >= 0.7f)
        assertEquals(3, result.detectedStates.size)
        assertEquals(listOf("a", "b"), result.detectedAlphabet)
        assertEquals("q0", result.detectedStart)
        assertEquals(listOf("q2"), result.detectedFinals)
        assertEquals(6, result.detectedTransitions.size)
    }

    @Test
    fun testTransitionTable_Detected() {
        val ocrText = """
            Transition Table:
            State | a  | b
            ->q0  | q1 | q0
            q1    | q2 | q0
            *q2   | q2 | q2
        """.trimIndent()

        val result = DFAImageParser.parse(ocrText)
        assertTrue(result.status == ParseStatus.DETECTED || result.status == ParseStatus.PARTIAL)
        assertNotNull(result.dfa)
        assertEquals("q0", result.detectedStart)
        assertTrue(result.detectedFinals.contains("q2"))
        assertTrue(result.detectedTransitions.isNotEmpty())
    }

    @Test
    fun testArrowNotationAndDeltaNotation() {
        val ocrText = """
            States: q0, q1
            Sigma: 0, 1
            Start: q0
            Final: q1
            δ(q0, 0) = q0
            q0 --1--> q1
            δ(q1, 0) = q0
            q1 --1--> q1
        """.trimIndent()

        val result = DFAImageParser.parse(ocrText)
        assertEquals(ParseStatus.DETECTED, result.status)
        assertNotNull(result.dfa)
        assertEquals(4, result.detectedTransitions.size)
    }

    @Test
    fun testSelfie_Rejected() {
        val selfieOcr = "Me and Sarah at Starbucks having iced latte! Best day ever #selfie #friends"
        val result = DFAImageParser.parse(selfieOcr)
        assertEquals(ParseStatus.REJECTED, result.status)
        assertNull(result.dfa)
    }

    @Test
    fun testNatureImage_Rejected() {
        val natureOcr = "Sunset over the Pacific Ocean. Majestic mountains and green forests."
        val result = DFAImageParser.parse(natureOcr)
        assertEquals(ParseStatus.REJECTED, result.status)
        assertNull(result.dfa)
    }

    @Test
    fun testBlankImage_Rejected() {
        val blankOcr = "   "
        val result = DFAImageParser.parse(blankOcr)
        assertEquals(ParseStatus.REJECTED, result.status)
        assertNull(result.dfa)
    }

    @Test
    fun testBlurredOrPartialDFA_PartialStatus() {
        val partialOcr = """
            States: q0, q1
            Alphabet: a, b
        """.trimIndent()

        val result = DFAImageParser.parse(partialOcr)
        assertEquals(ParseStatus.PARTIAL, result.status)
        assertNotNull(result.dfa)
        assertTrue(result.confidence < 0.7f)
    }

    @Test
    fun testFoodOrReceiptImage_Rejected() {
        val receiptOcr = """
            BURGER KING STORE #4102
            1 WHOOPER MEAL $9.99
            1 COCA COLA $1.99
            TOTAL: $11.98
            THANK YOU FOR VISITING
        """.trimIndent()

        val result = DFAImageParser.parse(receiptOcr)
        assertEquals(ParseStatus.REJECTED, result.status)
        assertNull(result.dfa)
    }

    @Test
    fun testReplaceOldDfaData() {
        val dfaAText = """
            States: q0, q1
            Alphabet: 0, 1
            Start: q0
            Final: q1
            q0, 0 = q0
            q0, 1 = q1
            q1, 0 = q0
            q1, 1 = q1
        """.trimIndent()

        val dfaBText = """
            States: A, B, C
            Alphabet: x, y
            Start: A
            Final: C
            A, x = B
            A, y = A
            B, x = C
            B, y = A
            C, x = C
            C, y = C
        """.trimIndent()

        val resA = DFAImageParser.parse(dfaAText)
        assertEquals(listOf("q0", "q1"), resA.detectedStates)
        assertEquals(listOf("0", "1"), resA.detectedAlphabet)

        val resB = DFAImageParser.parse(dfaBText)
        assertEquals(listOf("A", "B", "C"), resB.detectedStates)
        assertEquals(listOf("x", "y"), resB.detectedAlphabet)
        assertFalse(resB.detectedStates.contains("q0"))
        assertFalse(resB.detectedAlphabet.contains("0"))
    }
}
