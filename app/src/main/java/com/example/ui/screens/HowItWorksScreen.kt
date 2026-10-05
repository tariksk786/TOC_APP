package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class PipelineStage(
    val stageNumber: Int,
    val title: String,
    val subtitle: String,
    val description: String,
    val details: String,
    val color: Color
)

private val stages = listOf(
    PipelineStage(
        stageNumber = 1,
        title = "DFA Input",
        subtitle = "Manual, Compact Text, or Image OCR",
        description = "Ingests the 5-tuple specification: finite states Q, input alphabet Σ, transition table δ, unique start state q0, and accepting states F.",
        details = "The engine parses both structured form data, compact shorthand text (e.g. q0,a=q1), and OCR camera scans with interactive verification.",
        color = Color(0xFF2563EB)
    ),
    PipelineStage(
        stageNumber = 2,
        title = "Automata Validation",
        subtitle = "Deterministic & Structural Integrity",
        description = "Verifies that every transition points to valid states, guarantees strict determinism (no multiple outgoing transitions on the same symbol), and checks start/final state sanity.",
        details = "Unreachable final states generate warnings. Any missing start state or non-deterministic transition is rejected before algebraic processing.",
        color = Color(0xFF0EA5E9)
    ),
    PipelineStage(
        stageNumber = 3,
        title = "State Equation Setup",
        subtitle = "Linear Language Equations",
        description = "Formulates right-linear equations for every state: Ri = ∑(a Rj) + [ε if qi is final].",
        details = "Grouping transitions by target state factors common symbols (e.g. a R1 + b R1 becomes (a + b) R1). Final states receive the empty string token ε.",
        color = Color(0xFF14B8A6)
    ),
    PipelineStage(
        stageNumber = 4,
        title = "Arden's Lemma Resolution",
        subtitle = "Recursive Loop Elimination",
        description = "Whenever an equation contains itself (Ri = A Ri + B), Arden's Theorem is applied to yield Ri = A* B.",
        details = "Self-loops are factored cleanly. Uniqueness is guaranteed because in standard DFAs, transitions read non-empty symbols (ε ∉ L(A)).",
        color = Color(0xFFF59E0B)
    ),
    PipelineStage(
        stageNumber = 5,
        title = "Symbolic Substitution",
        subtitle = "Systematic Variable Elimination",
        description = "Replaces occurrences of resolved variables in all remaining active equations.",
        details = "This reduces an n-variable linear system down to a single equation for the start state variable R_start.",
        color = Color(0xFF8B5CF6)
    ),
    PipelineStage(
        stageNumber = 6,
        title = "Algebraic Reduction",
        subtitle = "Clean Canonical Form",
        description = "Applies algebraic simplification laws: R + ∅ = R, ε R = R, (∅)* = ε, and duplicate branch pruning.",
        details = "Simplification ensures the output regex is concise, readable, and mathematically minimal without changing language semantics.",
        color = Color(0xFFEC4899)
    ),
    PipelineStage(
        stageNumber = 7,
        title = "Exact Regular Expression",
        subtitle = "Mathematical Output",
        description = "Extracts the closed-form regular expression representing the exact language accepted by the DFA from its start state.",
        details = "The result can be copied, shared, or verified against test strings.",
        color = Color(0xFF10B981)
    ),
    PipelineStage(
        stageNumber = 8,
        title = "Dual-Model Verification",
        subtitle = "Automata vs. Regex Cross-Validation",
        description = "Generates a comprehensive test suite of strings and checks both DFA state traversal and the Regex AST matcher.",
        details = "Proves 100% language equivalence between the graphical transition diagram and the symbolic regular expression.",
        color = Color(0xFF6366F1)
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HowItWorksScreen(
    onBack: () -> Unit
) {
    var expandedIndex by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("How It Works: 8 Stages", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    text = "Interactive Architecture Pipeline: Tap any stage to expand inner mathematical details.",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B)
                )
            }

            itemsIndexed(stages) { idx, stage ->
                val isExpanded = expandedIndex == idx
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedIndex = if (isExpanded) null else idx }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(stage.color.copy(alpha = 0.12f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${stage.stageNumber}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = stage.color
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stage.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = stage.subtitle,
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                            Icon(
                                if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stage.description,
                            fontSize = 13.sp,
                            color = Color(0xFF334155),
                            lineHeight = 18.sp
                        )

                        AnimatedVisibility(visible = isExpanded) {
                            Column(modifier = Modifier.padding(top = 10.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = stage.details,
                                        fontSize = 12.sp,
                                        color = Color(0xFF475569),
                                        lineHeight = 18.sp,
                                        modifier = Modifier.padding(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
