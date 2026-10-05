package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
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
import com.example.model.VerificationItem
import com.example.ui.viewmodel.ArdenViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerificationScreen(
    viewModel: ArdenViewModel,
    onBack: () -> Unit
) {
    val verificationList by viewModel.verificationResults.collectAsStateWithLifecycle()
    val solution by viewModel.solution.collectAsStateWithLifecycle()
    val dfa by viewModel.currentDfa.collectAsStateWithLifecycle()

    var customTestString by remember { mutableStateOf("") }
    var customResult by remember { mutableStateOf<VerificationItem?>(null) }

    val allPassed = verificationList.all { it.passed }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Dual-Model Verification", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                // Summary Verification Banner
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (allPassed) Color(0xFFDCFCE7) else Color(0xFFFEE2E2),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (allPassed) Color(0xFF86EFAC) else Color(0xFFFCA5A5)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(if (allPassed) Color(0xFF16A34A) else Color(0xFFDC2626), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (allPassed) Icons.Default.Check else Icons.Default.Close,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (allPassed) "VERIFICATION PASSED (100%)" else "VERIFICATION INCONSISTENCY",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (allPassed) Color(0xFF15803D) else Color(0xFF991B1B)
                            )
                            Text(
                                text = if (allPassed)
                                    "Both DFA Transition Simulation and Derived Regular Expression yield identical language acceptance."
                                else
                                    "Discrepancies found between DFA state simulation and AST regex.",
                                fontSize = 12.sp,
                                color = if (allPassed) Color(0xFF166534) else Color(0xFF7F1D1D),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            item {
                // Custom String Quick Live Tester
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Test Custom String Live",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = customTestString,
                                onValueChange = {
                                    customTestString = it
                                    customResult = null
                                },
                                placeholder = { Text("e.g. abba, 0101, or empty (ε)") },
                                modifier = Modifier.weight(1f).testTag("custom_test_string_input"),
                                singleLine = true
                            )
                            Button(
                                onClick = {
                                    val regex = solution?.finalRegex
                                    if (regex != null) {
                                        val dfaAcc = dfa.simulate(customTestString)
                                        val regAcc = regex.matchesString(customTestString)
                                        customResult = VerificationItem(
                                            testString = if (customTestString.isEmpty()) "ε" else customTestString,
                                            dfaAccepted = dfaAcc,
                                            regexAccepted = regAcc,
                                            passed = dfaAcc == regAcc
                                        )
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                                modifier = Modifier.testTag("run_custom_test_button")
                            ) {
                                Text("Check")
                            }
                        }

                        if (customResult != null) {
                            val r = customResult!!
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (r.passed) Color(0xFFF0FDF4) else Color(0xFFFEF2F2),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (r.passed) Color(0xFFBBF7D0) else Color(0xFFFECACA)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "String: '${r.testString}'",
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "DFA: ${if (r.dfaAccepted) "Accepted" else "Rejected"} | Regex: ${if (r.regexAccepted) "Matched" else "Not Matched"}",
                                            fontSize = 12.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                    Text(
                                        text = if (r.passed) "✓ MATCH" else "✗ MISMATCH",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (r.passed) Color(0xFF16A34A) else Color(0xFFDC2626)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "Systematic Test Suite (${verificationList.size} Strings)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF334155)
                )
            }

            items(verificationList) { item ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "String: \"${item.testString}\"",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(
                                    text = "DFA: ${if (item.dfaAccepted) "Accept" else "Reject"}",
                                    fontSize = 12.sp,
                                    color = if (item.dfaAccepted) Color(0xFF16A34A) else Color(0xFF64748B)
                                )
                                Text(
                                    text = "Regex: ${if (item.regexAccepted) "Match" else "No Match"}",
                                    fontSize = 12.sp,
                                    color = if (item.regexAccepted) Color(0xFF2563EB) else Color(0xFF64748B)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (item.passed) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                        ) {
                            Text(
                                text = if (item.passed) "✓ PASS" else "✗ FAIL",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (item.passed) Color(0xFF15803D) else Color(0xFFDC2626),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
