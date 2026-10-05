package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class TheoryCard(
    val title: String,
    val subtitle: String,
    val formula: String?,
    val body: String,
    val keyTakeaway: String
)

private val theoryCards = listOf(
    TheoryCard(
        title = "What is a DFA?",
        subtitle = "Deterministic Finite Automaton",
        formula = "M = (Q, Σ, δ, q0, F)",
        body = "A Deterministic Finite Automaton (DFA) is a 5-tuple mathematical model of computation consisting of a finite set of states (Q), an input alphabet (Σ), a transition function (δ: Q × Σ → Q), an initial start state (q0), and a set of accepting final states (F).\n\nDeterminism guarantees that for every state and input symbol, there is exactly one designated destination state.",
        keyTakeaway = "Every regular language accepted by a DFA can be equivalently expressed as a Regular Expression."
    ),
    TheoryCard(
        title = "What is a Regular Expression?",
        subtitle = "Formal Language Denotation",
        formula = "R ::= ∅ | ε | a | (R1 + R2) | (R1 R2) | (R*)",
        body = "A Regular Expression (Regex) algebraically specifies the search patterns and structure of words in a regular language.\n\nIt utilizes three fundamental operations:\n1. Union / Alternation (+ or |)\n2. Concatenation (juxtaposition)\n3. Kleene Star (*) representing zero or more repetitions.",
        keyTakeaway = "Kleene's Theorem proves that DFAs, NFAs, and Regular Expressions have identical expressive computational power."
    ),
    TheoryCard(
        title = "What is Arden's Theorem?",
        subtitle = "Fundamental Automata Theorem",
        formula = "X = AX + B  ⟹  X = A* B  (if ε ∉ L(A))",
        body = "Let P and Q be two regular expressions over alphabet Σ.\n\nIf P does not contain the empty string ε (i.e. ε ∉ L(P)), then the equation in regular language variable R:\n\nR = P R + Q\n\nhas a unique mathematical solution given by:\n\nR = P* Q.",
        keyTakeaway = "Arden's Theorem allows eliminating self-referential recursive state loops into a closed-form Kleene star expression."
    ),
    TheoryCard(
        title = "Why Arden's Theorem Works",
        subtitle = "Mathematical Proof & Expansion",
        formula = "R = P(PR + Q) + Q = P²R + PQ + Q = ... = P* Q",
        body = "Proof by repeated substitution:\n• R = PR + Q\n• Substitute R: R = P(PR + Q) + Q = P²R + PQ + Q\n• Substitute R again: R = P³R + P²Q + PQ + Q\n• Inductively after n steps: R = PⁿR + (Pⁿ⁻¹ + ... + P + ε)Q\n\nAs n tends to infinity, the infinite union (P* Q) forms the exact fixed-point minimal solution.",
        keyTakeaway = "The condition ε ∉ L(P) is essential to guarantee uniqueness; otherwise multiple solutions would exist."
    ),
    TheoryCard(
        title = "Setting Up State Equations",
        subtitle = "Transition System Translation",
        formula = "Ri = ∑(a Rj for qi --a--> qj) + [ε if qi ∈ F]",
        body = "For each state qi in the DFA, define variable Ri representing the language accepted starting from qi.\n\nInspect all outgoing transitions from qi:\n• For each transition qi --a--> qj, add term 'a Rj'.\n• If state qi is a final (accepting) state, also add term 'ε' because the empty string is accepted without reading any symbols.",
        keyTakeaway = "This yields a linear system of n equations in n regular language variables."
    ),
    TheoryCard(
        title = "Variable Elimination",
        subtitle = "Systematic Algebraic Solving",
        formula = "Substitute Ri = A* B into all Rk containing Ri",
        body = "1. Order states so start state R0 is solved last.\n2. For each intermediate state Rk, if it references itself on the RHS, apply Arden's Theorem to resolve Rk into a closed form.\n3. Substitute the resolved Rk expression into every other equation.\n4. Repeat until only the start state equation R0 remains, which is then resolved using Arden's Theorem.",
        keyTakeaway = "The final expression for start state R0 equals the exact language accepted by the entire DFA."
    ),
    TheoryCard(
        title = "Safe Regex Simplifications",
        subtitle = "Identity & Reduction Laws",
        formula = "R + ∅ = R  |  εR = R  |  (∅)* = ε  |  R + R = R",
        body = "Throughout the symbolic solving process, algebraic identities are applied to keep formulas clean and minimal:\n• Empty set identities: R + ∅ = R, R ∅ = ∅\n• Epsilon identities: ε R = R, R ε = R\n• Idempotence: R + R = R\n• Star properties: (∅)* = ε, (ε)* = ε, (R*)* = R*",
        keyTakeaway = "Strict algebraic reduction preserves exact language equality while eliminating visual clutter."
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LearnScreen(
    onBack: () -> Unit
) {
    var currentCardIndex by remember { mutableIntStateOf(0) }
    val card = theoryCards[currentCardIndex]
    val totalCards = theoryCards.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Arden's Theory Guide", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            Surface(
                color = Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { if (currentCardIndex > 0) currentCardIndex-- },
                        enabled = currentCardIndex > 0,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(48.dp).testTag("learn_prev_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Previous")
                    }

                    Button(
                        onClick = { if (currentCardIndex < totalCards - 1) currentCardIndex++ },
                        enabled = currentCardIndex < totalCards - 1,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF14B8A6)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(48.dp).testTag("learn_next_button")
                    ) {
                        Text("Next Card")
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        },
        containerColor = Color(0xFFF8FAFC)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Horizontal Card Indicator Pills
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                itemsIndexed(theoryCards) { idx, _ ->
                    val isSelected = idx == currentCardIndex
                    Surface(
                        shape = CircleShape,
                        color = if (isSelected) Color(0xFF14B8A6) else Color.White,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) Color(0xFF14B8A6) else Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            TextButton(
                                onClick = { currentCardIndex = idx },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(
                                    text = "${idx + 1}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else Color(0xFF475569)
                                )
                            }
                        }
                    }
                }
            }

            // Theory Card (Fits Screen Comfortably)
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFCCFBF1)
                        ) {
                            Text(
                                text = "CARD ${currentCardIndex + 1} OF $totalCards",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F766E),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = card.title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = card.subtitle,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF14B8A6)
                    )

                    if (card.formula != null) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFF1F5F9),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = card.formula,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF0F172A),
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Text(
                        text = card.body,
                        fontSize = 14.sp,
                        color = Color(0xFF334155),
                        lineHeight = 22.sp
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFF0FDF4),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Lightbulb, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Key Takeaway: ${card.keyTakeaway}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF166534),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
