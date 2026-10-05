package com.example.model

data class ExampleProblem(
    val id: String,
    val title: String,
    val description: String,
    val category: String,
    val dfa: DFA
)

object DFAExamples {

    // Demo Problem: Classic textbook problem: Strings ending in 'a' over {a, b}
    val demoProblem = ExampleProblem(
        id = "demo",
        title = "Demo: Strings Ending in 'a'",
        description = "Language over alphabet {a, b} containing all strings that terminate with symbol 'a'.",
        category = "Textbook Classic",
        dfa = DFA(
            states = listOf(
                DFAState(id = "q0", isStart = true, isFinal = false),
                DFAState(id = "q1", isStart = false, isFinal = true)
            ),
            alphabet = listOf("a", "b"),
            transitions = listOf(
                Transition("q0", "a", "q1"),
                Transition("q0", "b", "q0"),
                Transition("q1", "a", "q1"),
                Transition("q1", "b", "q0")
            )
        )
    )

    val allExamples = listOf(
        demoProblem,
        ExampleProblem(
            id = "ex_containing_ab",
            title = "Strings Containing 'ab'",
            description = "DFA recognizing any string over {a, b} that contains substring 'ab'.",
            category = "Pattern Search",
            dfa = DFA(
                states = listOf(
                    DFAState(id = "q0", isStart = true, isFinal = false),
                    DFAState(id = "q1", isStart = false, isFinal = false),
                    DFAState(id = "q2", isStart = false, isFinal = true)
                ),
                alphabet = listOf("a", "b"),
                transitions = listOf(
                    Transition("q0", "a", "q1"),
                    Transition("q0", "b", "q0"),
                    Transition("q1", "a", "q1"),
                    Transition("q1", "b", "q2"),
                    Transition("q2", "a", "q2"),
                    Transition("q2", "b", "q2")
                )
            )
        ),
        ExampleProblem(
            id = "ex_ending_01",
            title = "Binary Strings Ending in '01'",
            description = "DFA recognizing binary sequences {0, 1} ending with the suffix '01'.",
            category = "Binary Suffix",
            dfa = DFA(
                states = listOf(
                    DFAState(id = "q0", isStart = true, isFinal = false),
                    DFAState(id = "q1", isStart = false, isFinal = false),
                    DFAState(id = "q2", isStart = false, isFinal = true)
                ),
                alphabet = listOf("0", "1"),
                transitions = listOf(
                    Transition("q0", "0", "q1"),
                    Transition("q0", "1", "q0"),
                    Transition("q1", "0", "q1"),
                    Transition("q1", "1", "q2"),
                    Transition("q2", "0", "q1"),
                    Transition("q2", "1", "q0")
                )
            )
        ),
        ExampleProblem(
            id = "ex_even_zeros",
            title = "Even Number of 0s",
            description = "DFA over {0, 1} accepting all binary strings with an even count of '0's (including ε).",
            category = "Parity",
            dfa = DFA(
                states = listOf(
                    DFAState(id = "q0", isStart = true, isFinal = true),
                    DFAState(id = "q1", isStart = false, isFinal = false)
                ),
                alphabet = listOf("0", "1"),
                transitions = listOf(
                    Transition("q0", "0", "q1"),
                    Transition("q0", "1", "q0"),
                    Transition("q1", "0", "q0"),
                    Transition("q1", "1", "q1")
                )
            )
        ),
        ExampleProblem(
            id = "ex_two_state_toggle",
            title = "Simple Two-State Alternating",
            description = "Simple 2-state DFA accepting strings ending with 'b'.",
            category = "Basics",
            dfa = DFA(
                states = listOf(
                    DFAState(id = "q0", isStart = true, isFinal = false),
                    DFAState(id = "q1", isStart = false, isFinal = true)
                ),
                alphabet = listOf("a", "b"),
                transitions = listOf(
                    Transition("q0", "a", "q0"),
                    Transition("q0", "b", "q1"),
                    Transition("q1", "a", "q0"),
                    Transition("q1", "b", "q1")
                )
            )
        )
    )
}
