package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.navigation.NavRoutes

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SolveMethodScreen(
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Choose Input Method", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "HOW WOULD YOU LIKE TO ENTER THE QUESTION?",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF64748B),
                letterSpacing = 0.5.sp
            )

            InputOptionCard(
                title = "Manual Input",
                subtitle = "Configure states, alphabet, initial/final states, and transition rows interactively",
                icon = Icons.Default.Tune,
                badge = "Recommended",
                color = Color(0xFF2563EB),
                tag = "method_manual"
            ) {
                onNavigate(NavRoutes.INPUT_MANUAL)
            }

            InputOptionCard(
                title = "Compact Text Input",
                subtitle = "Paste equations or shorthand transitions (e.g. q0,a=q1; Start: q0; Final: q2)",
                icon = Icons.Default.Code,
                badge = "Fastest",
                color = Color(0xFF0EA5E9),
                tag = "method_compact"
            ) {
                onNavigate(NavRoutes.INPUT_COMPACT)
            }

            InputOptionCard(
                title = "Image / Camera OCR",
                subtitle = "Upload or photograph a DFA transition table or diagram with editable verification",
                icon = Icons.Default.CameraAlt,
                badge = "Smart OCR",
                color = Color(0xFF14B8A6),
                tag = "method_ocr"
            ) {
                onNavigate(NavRoutes.INPUT_IMAGE_OCR)
            }

            InputOptionCard(
                title = "Load Example Problem",
                subtitle = "Select from 5 verified textbook DFA automata problems ready for step-by-step resolution",
                icon = Icons.Default.CollectionsBookmark,
                badge = "5 Presets",
                color = Color(0xFF8B5CF6),
                tag = "method_example"
            ) {
                onNavigate(NavRoutes.EXAMPLES)
            }
        }
    }
}

@Composable
private fun InputOptionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    badge: String,
    color: Color,
    tag: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
        shadowElevation = 1.5.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(color.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = title, tint = color, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = color.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = badge,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = color,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    lineHeight = 16.sp
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Select",
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
