package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DFA
import com.example.model.DFAState
import com.example.model.Transition
import com.example.solver.DFAValidator
import com.example.ui.navigation.NavRoutes
import com.example.ui.viewmodel.ArdenViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualInputScreen(
    viewModel: ArdenViewModel,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    var statesText by remember { mutableStateOf("q0, q1") }
    var alphabetText by remember { mutableStateOf("a, b") }
    var startState by remember { mutableStateOf("q0") }
    var finalStates by remember { mutableStateOf(setOf("q1")) }

    var transitions by remember {
        mutableStateOf(
            listOf(
                Transition("q0", "a", "q1"),
                Transition("q0", "b", "q0"),
                Transition("q1", "a", "q1"),
                Transition("q1", "b", "q0")
            )
        )
    }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Derive parsed state list
    val parsedStates = statesText.split(",", " ").map { it.trim() }.filter { it.isNotEmpty() }.distinct()
    val parsedAlphabet = alphabetText.split(",", " ").map { it.trim() }.filter { it.isNotEmpty() }.distinct()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manual DFA Input", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            statesText = "q0, q1, q2"
                            alphabetText = "a, b"
                            startState = "q0"
                            finalStates = setOf("q2")
                            transitions = listOf(
                                Transition("q0", "a", "q1"),
                                Transition("q0", "b", "q0"),
                                Transition("q1", "a", "q2"),
                                Transition("q1", "b", "q0"),
                                Transition("q2", "a", "q2"),
                                Transition("q2", "b", "q2")
                            )
                        }
                    ) {
                        Text("Fill Sample", color = Color(0xFF2563EB))
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
                Column(modifier = Modifier.padding(16.dp)) {
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage ?: "",
                            color = Color(0xFFDC2626),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    Button(
                        onClick = {
                            val dfaStates = parsedStates.map { id ->
                                DFAState(
                                    id = id,
                                    isStart = id == startState,
                                    isFinal = finalStates.contains(id)
                                )
                            }
                            val candidateDFA = DFA(
                                states = dfaStates,
                                alphabet = parsedAlphabet,
                                transitions = transitions
                            )
                            val validation = DFAValidator.validate(candidateDFA)
                            if (validation.isValid) {
                                viewModel.setDFA(candidateDFA)
                                onNavigate(NavRoutes.DFA_VIEW)
                            } else {
                                errorMessage = validation.errors.firstOrNull() ?: "Invalid DFA"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("validate_continue_button")
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Validate & View Diagram", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        },
        containerColor = Color(0xFFF8FAFC)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // States & Alphabet Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("1. States & Alphabet", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))

                        OutlinedTextField(
                            value = statesText,
                            onValueChange = {
                                statesText = it
                                errorMessage = null
                            },
                            label = { Text("States (comma-separated)") },
                            placeholder = { Text("e.g. q0, q1, q2") },
                            modifier = Modifier.fillMaxWidth().testTag("states_input")
                        )

                        OutlinedTextField(
                            value = alphabetText,
                            onValueChange = {
                                alphabetText = it
                                errorMessage = null
                            },
                            label = { Text("Alphabet (comma-separated)") },
                            placeholder = { Text("e.g. a, b") },
                            modifier = Modifier.fillMaxWidth().testTag("alphabet_input")
                        )
                    }
                }
            }

            item {
                // Initial State & Final States Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("2. Initial & Final States", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))

                        Text("Select Initial / Start State:", fontSize = 13.sp, color = Color(0xFF475569))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            parsedStates.forEach { st ->
                                FilterChip(
                                    selected = startState == st,
                                    onClick = { startState = st },
                                    label = { Text(st) },
                                    leadingIcon = if (startState == st) {
                                        { Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Select Final / Accepting States:", fontSize = 13.sp, color = Color(0xFF475569))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            parsedStates.forEach { st ->
                                val isSelected = finalStates.contains(st)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        finalStates = if (isSelected) finalStates - st else finalStates + st
                                    },
                                    label = { Text(st) },
                                    leadingIcon = if (isSelected) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null
                                )
                            }
                        }
                    }
                }
            }

            item {
                // Transitions Card
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("3. Transitions (${transitions.size})", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TextButton(
                            onClick = {
                                val from = parsedStates.firstOrNull() ?: "q0"
                                val sym = parsedAlphabet.firstOrNull() ?: "a"
                                val to = parsedStates.lastOrNull() ?: "q0"
                                transitions = transitions + Transition(from, sym, to)
                            }
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Row")
                        }
                        TextButton(onClick = { transitions = emptyList() }) {
                            Text("Clear", color = Color(0xFFDC2626))
                        }
                    }
                }
            }

            itemsIndexed(transitions) { index, t ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = t.from,
                            onValueChange = { newVal ->
                                transitions = transitions.toMutableList().also {
                                    it[index] = it[index].copy(from = newVal.trim())
                                }
                            },
                            label = { Text("From") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = t.symbol,
                            onValueChange = { newVal ->
                                transitions = transitions.toMutableList().also {
                                    it[index] = it[index].copy(symbol = newVal.trim())
                                }
                            },
                            label = { Text("Sym") },
                            modifier = Modifier.weight(0.8f)
                        )
                        OutlinedTextField(
                            value = t.to,
                            onValueChange = { newVal ->
                                transitions = transitions.toMutableList().also {
                                    it[index] = it[index].copy(to = newVal.trim())
                                }
                            },
                            label = { Text("To") },
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                transitions = transitions.filterIndexed { i, _ -> i != index }
                            }
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Delete", tint = Color(0xFFEF4444), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
