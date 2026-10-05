package com.example

import com.example.model.DFA
import com.example.model.DFAExamples
import com.example.model.DFAState
import com.example.model.Transition
import com.example.solver.ArdenSolver
import com.example.solver.DFAParser
import com.example.solver.DFAValidator
import com.example.solver.VerificationEngine
import org.junit.Assert.*
import org.junit.Test

class ArdenSolverTest {

    @Test
    fun testDfaValidatorValid() {
        val dfa = DFAExamples.demoProblem.dfa
        val res = DFAValidator.validate(dfa)
        assertTrue(res.isValid)
        assertTrue(res.errors.isEmpty())
    }

    @Test
    fun testDfaValidatorDetectsErrors() {
        // Missing start state
        val noStart = DFA(
            states = listOf(DFAState("q0", isStart = false, isFinal = true)),
            alphabet = listOf("a"),
            transitions = emptyList()
        )
        val res1 = DFAValidator.validate(noStart)
        assertFalse(res1.isValid)

        // Non-deterministic transitions
        val nfa = DFA(
            states = listOf(DFAState("q0", isStart = true, isFinal = true)),
            alphabet = listOf("a"),
            transitions = listOf(
                Transition("q0", "a", "q0"),
                Transition("q0", "a", "q0")
            )
        )
        val res2 = DFAValidator.validate(nfa)
        assertFalse(res2.isValid)
    }

    @Test
    fun testDfaParser() {
        val input = """
            States: q0, q1
            Alphabet: a, b
            Start: q0
            Final: q1
            
            q0,a=q1
            q0,b=q0
            q1,a=q1
            q1,b=q0
        """.trimIndent()

        val dfa = DFAParser.parse(input)
        assertEquals(2, dfa.states.size)
        assertEquals(listOf("a", "b"), dfa.alphabet)
        assertEquals("q0", dfa.startState?.id)
        assertEquals(listOf("q1"), dfa.finalStates.map { it.id })
        assertEquals(4, dfa.transitions.size)
    }

    @Test
    fun testArdenSolverDemo() {
        val dfa = DFAExamples.demoProblem.dfa
        val solution = ArdenSolver.solve(dfa)

        assertNotNull(solution)
        assertTrue(solution.steps.isNotEmpty())
        assertTrue(solution.finalRegexString.isNotEmpty())

        // Language of demo DFA is strings ending in 'a'
        // Test strings ending in 'a': "a", "ba", "aba", "bba"
        assertTrue(dfa.simulate("a"))
        assertTrue(dfa.simulate("ba"))
        assertTrue(dfa.simulate("aba"))
        assertTrue(dfa.simulate("bba"))

        // Test strings not ending in 'a': "", "b", "ab", "bb"
        assertFalse(dfa.simulate(""))
        assertFalse(dfa.simulate("b"))
        assertFalse(dfa.simulate("ab"))
        assertFalse(dfa.simulate("bb"))

        // Verify AST matcher matches DFA simulation
        val verifications = VerificationEngine.verify(dfa, solution.finalRegex)
        assertTrue(verifications.isNotEmpty())
        for (item in verifications) {
            assertEquals("Mismatch for string: '${item.testString}'", item.dfaAccepted, item.regexAccepted)
        }
    }

    @Test
    fun testEquationBuilderSimpleExample() {
        // q0 --a--> q0, q0 --b--> q1, q1 is final
        val dfa = DFA(
            states = listOf(
                DFAState("q0", isStart = true, isFinal = false),
                DFAState("q1", isStart = false, isFinal = true)
            ),
            alphabet = listOf("a", "b"),
            transitions = listOf(
                Transition("q0", "a", "q0"),
                Transition("q0", "b", "q1")
            )
        )

        val eqResult = com.example.solver.EquationBuilder.buildEquations(dfa)
        assertEquals(2, eqResult.stateInfoList.size)

        val r0 = eqResult.equationStrings["R0"]
        val r1 = eqResult.equationStrings["R1"]

        assertNotNull(r0)
        assertNotNull(r1)
        assertTrue("R0 equation must contain aR0 and bR1", r0!!.contains("aR0") && r0.contains("bR1"))
        assertEquals("R1 equation must be ε for empty transitions final state", "R1 = ε", r1)
    }

    @Test
    fun testEquationBuilderComplexExample() {
        // q0 --a--> q1, q0 --b--> q0
        // q1 --a--> q2, q1 --b--> q0
        // q2 --a--> q2, q2 --b--> q2, q2 final
        val dfa = DFA(
            states = listOf(
                DFAState("q0", isStart = true, isFinal = false),
                DFAState("q1", isStart = false, isFinal = false),
                DFAState("q2", isStart = false, isFinal = true)
            ),
            alphabet = listOf("a", "b"),
            transitions = listOf(
                Transition("q0", "a", "q1"),
                Transition("q0", "b", "q0"),
                Transition("q1", "a", "q2"),
                Transition("q1", "b", "q0"),
                Transition("q2", "a", "q2"),
                Transition("q2", "b", "q2")
            )
        )

        val eqResult = com.example.solver.EquationBuilder.buildEquations(dfa)
        val r0 = eqResult.equationStrings["R0"]!!
        val r1 = eqResult.equationStrings["R1"]!!
        val r2 = eqResult.equationStrings["R2"]!!

        assertTrue("R0 must contain aR1 and bR0", r0.contains("aR1") && r0.contains("bR0"))
        assertTrue("R1 must contain aR2 and bR0", r1.contains("aR2") && r1.contains("bR0"))
        assertTrue("R2 must contain (a + b)R2 and ε", (r2.contains("(a + b)R2") || r2.contains("aR2 + bR2")) && r2.contains("ε"))

        // Now test full Arden solve
        val solution = ArdenSolver.solve(dfa)
        assertNotNull(solution)
        assertTrue(solution.steps.size >= 4)
        assertTrue(solution.ardenApplicationsCount >= 1)

        // Strings reaching q2: must contain "aa"
        assertTrue(dfa.simulate("aa"))
        assertTrue(dfa.simulate("baa"))
        assertTrue(dfa.simulate("aab"))
        assertTrue(dfa.simulate("baabaab"))
        assertFalse(dfa.simulate(""))
        assertFalse(dfa.simulate("a"))
        assertFalse(dfa.simulate("b"))
        assertFalse(dfa.simulate("ab"))
    }

    @Test
    fun testRegexSimplifierIdentities() {
        val a = com.example.regex.Literal("a")

        // R + ∅ = R
        val unionEmpty = com.example.regex.RegexSimplifier.simplify(com.example.regex.Union(listOf(a, com.example.regex.EmptySet)))
        assertEquals(a, unionEmpty)

        // ε R = R
        val concatEps = com.example.regex.RegexSimplifier.simplify(com.example.regex.Concat(listOf(com.example.regex.Epsilon, a)))
        assertEquals(a, concatEps)

        // ∅ R = ∅
        val concatEmpty = com.example.regex.RegexSimplifier.simplify(com.example.regex.Concat(listOf(com.example.regex.EmptySet, a)))
        assertEquals(com.example.regex.EmptySet, concatEmpty)

        // (∅)* = ε
        val starEmpty = com.example.regex.RegexSimplifier.simplify(com.example.regex.Star(com.example.regex.EmptySet))
        assertEquals(com.example.regex.Epsilon, starEmpty)

        // (ε)* = ε
        val starEps = com.example.regex.RegexSimplifier.simplify(com.example.regex.Star(com.example.regex.Epsilon))
        assertEquals(com.example.regex.Epsilon, starEps)

        // R + R = R
        val unionIdem = com.example.regex.RegexSimplifier.simplify(com.example.regex.Union(listOf(a, a)))
        assertEquals(a, unionIdem)
    }
}
