package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.DFACanvasView
import com.example.ui.viewmodel.ArdenViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimulationScreen(
    viewModel: ArdenViewModel,
    onBack: () -> Unit
) {
    val dfa by viewModel.currentDfa.collectAsStateWithLifecycle()
    val simString by viewModel.simulationString.collectAsStateWithLifecycle()
    val steps by viewModel.simulationSteps.collectAsStateWithLifecycle()
    val currentStepIdx by viewModel.currentSimStepIndex.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isSimPlaying.collectAsStateWithLifecycle()

    var inputFieldText by remember(simString) { mutableStateOf(simString) }

    LaunchedEffect(Unit) {
        viewModel.prepareSimulation()
    }

    val currentStep = steps.getOrNull(currentStepIdx)
    val isComplete = currentStep != null && currentStepIdx == steps.size - 1

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Interactive Simulation", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // String input row
            Surface(
                shape = RoundedCornerShape(12.dp),
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
                        value = inputFieldText,
                        onValueChange = { inputFieldText = it },
                        label = { Text("Simulation Input String") },
                        placeholder = { Text("e.g. abba or empty") },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("sim_input_field")
                    )
                    Button(
                        onClick = {
                            viewModel.setSimulationString(inputFieldText)
                            viewModel.prepareSimulation()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        modifier = Modifier.testTag("apply_sim_string_button")
                    ) {
                        Text("Simulate")
                    }
                }
            }

            // Interactive DFA Canvas with live active state highlighted
            DFACanvasView(
                dfa = dfa,
                activeStateId = currentStep?.currentState,
                highlightTransition = if (currentStep?.symbol != null) Pair(currentStep.currentState, currentStep.symbol) else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            // Tape & Current Step Progress
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "String Tape (Position: $currentStepIdx/${steps.size - 1})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B)
                        )
                        if (isComplete && currentStep != null) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = if (currentStep.isAccepted) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                            ) {
                                Text(
                                    text = if (currentStep.isAccepted) "✓ ACCEPTED" else "✗ REJECTED",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (currentStep.isAccepted) Color(0xFF15803D) else Color(0xFFDC2626),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // Tape visualization
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (simString.isEmpty()) {
                            Text(
                                text = "ε (Empty Word)",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2563EB)
                            )
                        } else {
                            simString.forEachIndexed { idx, ch ->
                                val isCurrent = idx == (currentStepIdx - 1)
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (isCurrent) Color(0xFFFDE047) else if (idx < currentStepIdx) Color(0xFFE2E8F0) else Color(0xFFF8FAFC),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isCurrent) Color(0xFFD97706) else Color(0xFFCBD5E1)
                                    ),
                                    modifier = Modifier.padding(horizontal = 3.dp).size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = ch.toString(),
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = Color(0xFF0F172A)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Transition Status Text
                    if (currentStep != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF1F5F9),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (currentStepIdx == 0)
                                    "Start at initial state '${currentStep.currentState}'"
                                else
                                    "State '${currentStep.currentState}' read symbol '${currentStep.symbol}' ➔ reached '${currentStep.nextState}'",
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF1E293B),
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }

            // Controls Bar: Previous, Play/Pause, Next, Reset
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { viewModel.previousSimStep() },
                        enabled = currentStepIdx > 0,
                        modifier = Modifier.testTag("sim_prev_button")
                    ) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "Previous")
                    }

                    FilledIconButton(
                        onClick = { viewModel.toggleSimPlay() },
                        colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF2563EB)),
                        modifier = Modifier.size(44.dp).testTag("sim_play_pause_button")
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = { viewModel.nextSimStep() },
                        enabled = currentStepIdx < steps.size - 1,
                        modifier = Modifier.testTag("sim_next_button")
                    ) {
                        Icon(Icons.Default.SkipNext, contentDescription = "Next")
                    }

                    IconButton(
                        onClick = { viewModel.resetSimulation() },
                        modifier = Modifier.testTag("sim_reset_button")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Restart")
                    }
                }
            }
        }
    }
}
