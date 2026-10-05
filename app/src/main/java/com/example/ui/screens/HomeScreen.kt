package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.DFAExamples
import com.example.ui.navigation.NavRoutes
import com.example.ui.viewmodel.ArdenViewModel

@Composable
fun HomeScreen(
    viewModel: ArdenViewModel,
    onNavigate: (String) -> Unit
) {
    val historyList by viewModel.historySolutions.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC))
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // App Header
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF2563EB), Color(0xFF0EA5E9))
                            ),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Functions,
                        contentDescription = "Automata",
                        tint = Color.White,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column {
                    Text(
                        text = "Arden's Theorem Solver",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "DFA to Regular Expression — Step by Step",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Hero Demo Action Banner
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFF1E3A8A),
            shadowElevation = 3.dp,
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    viewModel.loadExample(DFAExamples.demoProblem)
                    onNavigate(NavRoutes.STEP_SOLVER)
                }
                .testTag("demo_solver_card")
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF38BDF8).copy(alpha = 0.25f)
                        ) {
                            Text(
                                text = "TEACHER DEMO",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF7DD3FC),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Quick Demo Solver",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Instant 2-state DFA: Diagram ➔ Equations ➔ Arden ➔ Regex",
                        fontSize = 12.sp,
                        color = Color(0xFFBFDBFE),
                        lineHeight = 16.sp
                    )
                }
                FilledIconButton(
                    onClick = {
                        viewModel.loadExample(DFAExamples.demoProblem)
                        onNavigate(NavRoutes.STEP_SOLVER)
                    },
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color(0xFF38BDF8)),
                    modifier = Modifier.size(42.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Start Demo",
                        tint = Color(0xFF0F172A)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4 Main Action Cards Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ActionTile(
                title = "Solve DFA",
                subtitle = "Manual / Image",
                icon = Icons.Default.EditCalendar,
                tint = Color(0xFF2563EB),
                modifier = Modifier.weight(1f).testTag("action_solve_dfa")
            ) {
                onNavigate(NavRoutes.SOLVE_METHOD)
            }
            ActionTile(
                title = "Examples",
                subtitle = "5 Problems",
                icon = Icons.Default.MenuBook,
                tint = Color(0xFF0EA5E9),
                modifier = Modifier.weight(1f).testTag("action_examples")
            ) {
                onNavigate(NavRoutes.EXAMPLES)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ActionTile(
                title = "Learn Arden",
                subtitle = "Theory Cards",
                icon = Icons.Default.Lightbulb,
                tint = Color(0xFF14B8A6),
                modifier = Modifier.weight(1f).testTag("action_learn")
            ) {
                onNavigate(NavRoutes.LEARN)
            }
            ActionTile(
                title = "How It Works",
                subtitle = "8-Stage Pipeline",
                icon = Icons.Default.AccountTree,
                tint = Color(0xFF8B5CF6),
                modifier = Modifier.weight(1f).testTag("action_how_it_works")
            ) {
                onNavigate(NavRoutes.HOW_IT_WORKS)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Recent History Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Solutions",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )
            if (historyList.isNotEmpty()) {
                TextButton(
                    onClick = { onNavigate(NavRoutes.HISTORY) },
                    contentPadding = PaddingValues(horizontal = 4.dp)
                ) {
                    Text("View All (${historyList.size})", fontSize = 13.sp, color = Color(0xFF2563EB))
                }
            }
        }

        if (historyList.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "No saved solutions yet",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B)
                    )
                    Text(
                        text = "Tap 'Solve DFA' or 'Quick Demo' to get started",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(historyList.take(4)) { item ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White,
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.loadFromHistory(item)
                                onNavigate(NavRoutes.FINAL_RESULT)
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(Color(0xFFEFF6FF), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${item.stateCount}S",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2563EB)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Regex: ${item.finalRegex}",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 12.sp,
                                    color = Color(0xFF2563EB),
                                    maxLines = 1
                                )
                            }
                            IconButton(onClick = { viewModel.deleteHistory(item.id) }) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = "Delete",
                                    tint = Color(0xFF94A3B8),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shadowElevation = 1.dp,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(tint.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = tint, modifier = Modifier.size(20.dp))
            }
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = Color(0xFF64748B)
            )
        }
    }
}
