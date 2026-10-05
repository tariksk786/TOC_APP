package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.components.DFACanvasView
import com.example.ui.components.ResetConfirmDialog
import com.example.ui.navigation.NavRoutes
import com.example.ui.viewmodel.ArdenViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DfaVisualizerScreen(
    viewModel: ArdenViewModel,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    val dfa by viewModel.currentDfa.collectAsStateWithLifecycle()
    val validationResult by viewModel.validationResult.collectAsStateWithLifecycle()
    var showResetDialog by remember { mutableStateOf(false) }

    if (showResetDialog) {
        ResetConfirmDialog(
            onConfirm = {
                showResetDialog = false
                viewModel.resetAll()
                onNavigate(NavRoutes.HOME)
            },
            onDismiss = { showResetDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DFA Diagram", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showResetDialog = true }, modifier = Modifier.testTag("reset_button")) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reset", tint = Color(0xFF64748B))
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
                        onClick = {
                            viewModel.prepareSimulation()
                            onNavigate(NavRoutes.SIMULATION)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("simulate_dfa_button")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Simulate", fontSize = 14.sp)
                    }

                    Button(
                        onClick = {
                            viewModel.solveDfa()
                            onNavigate(NavRoutes.STATE_EQUATIONS)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1.4f)
                            .height(50.dp)
                            .testTag("start_arden_solution_button")
                    ) {
                        Text("Start Arden Solution", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
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
            // Validation Pill
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (validationResult?.isValid == true) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (validationResult?.isValid == true) Color(0xFF86EFAC) else Color(0xFFFCA5A5)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = if (validationResult?.isValid == true) Color(0xFF16A34A) else Color(0xFFDC2626),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = validationResult?.message ?: "DFA Validated",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = if (validationResult?.isValid == true) Color(0xFF15803D) else Color(0xFF991B1B)
                    )
                }
            }

            // Interactive DFA Canvas View
            DFACanvasView(
                dfa = dfa,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )

            // Legend Information
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LegendItem(label = "Start: → q0", color = Color(0xFF0EA5E9))
                    LegendItem(label = "Final: ((q))", color = Color(0xFF2563EB))
                    LegendItem(label = "Alphabet: {${dfa.alphabet.joinToString()}}", color = Color(0xFF14B8A6))
                }
            }
        }
    }
}

@Composable
private fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, RoundedCornerShape(4.dp))
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF475569))
    }
}
