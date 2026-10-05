package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.solver.DFAParser
import com.example.solver.DFAValidator
import com.example.ui.navigation.NavRoutes
import com.example.ui.viewmodel.ArdenViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompactInputScreen(
    viewModel: ArdenViewModel,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    var rawText by remember {
        mutableStateOf(
            """
            States: q0, q1, q2
            Alphabet: a, b
            Start: q0
            Final: q2
            
            q0,a=q1
            q0,b=q0
            q1,a=q2
            q1,b=q0
            q2,a=q2
            q2,b=q2
            """.trimIndent()
        )
    }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Compact Text Input", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { rawText = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
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
                            try {
                                val dfa = DFAParser.parse(rawText)
                                val validation = DFAValidator.validate(dfa)
                                if (validation.isValid) {
                                    viewModel.setDFA(dfa)
                                    onNavigate(NavRoutes.DFA_VIEW)
                                } else {
                                    errorMessage = validation.errors.firstOrNull() ?: "Validation failed"
                                }
                            } catch (e: Exception) {
                                errorMessage = "Parse error: ${e.localizedMessage}"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("parse_compact_button")
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Parse, Validate & Visualize", fontWeight = FontWeight.Bold, fontSize = 15.sp)
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
            Text(
                text = "Enter or paste shorthand DFA grammar specification:",
                fontSize = 13.sp,
                color = Color(0xFF64748B)
            )

            OutlinedTextField(
                value = rawText,
                onValueChange = {
                    rawText = it
                    errorMessage = null
                },
                placeholder = {
                    Text("States: q0, q1\nAlphabet: a, b\nStart: q0\nFinal: q1\nq0,a=q1\nq0,b=q0")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .testTag("compact_text_input"),
                textStyle = LocalTextStyle.current.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp
                ),
                shape = RoundedCornerShape(12.dp)
            )

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFEFF6FF),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBFDBFE)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "SUPPORTED SYNTAX PATTERNS:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = Color(0xFF1D4ED8)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "• qi, a = qj  OR  qi --a--> qj  OR  δ(qi, a) = qj\n• Table rows: qi | a | qj\n• Start: qi | Final: qj, qk",
                        fontSize = 12.sp,
                        color = Color(0xFF1E3A8A),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}
