package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.DFA
import com.example.model.DFAState
import com.example.model.Transition
import com.example.solver.DFAValidator
import com.example.ui.navigation.NavRoutes
import com.example.ui.viewmodel.ArdenViewModel
import com.example.ui.viewmodel.ImageStatus
import com.example.ui.viewmodel.ProcessingStage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageOcrScreen(
    viewModel: ArdenViewModel,
    onNavigate: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val imageStatus by viewModel.imageStatus.collectAsStateWithLifecycle()
    val processingStage by viewModel.processingStage.collectAsStateWithLifecycle()
    val ocrConfidence by viewModel.ocrConfidence.collectAsStateWithLifecycle()
    val dfaDetectionConfidence by viewModel.dfaDetectionConfidence.collectAsStateWithLifecycle()
    val ocrRawText by viewModel.ocrRawText.collectAsStateWithLifecycle()
    val detectedDfa by viewModel.detectedDfa.collectAsStateWithLifecycle()
    val selectedBitmap by viewModel.selectedImageBitmap.collectAsStateWithLifecycle()
    val imageErrorMessage by viewModel.imageErrorMessage.collectAsStateWithLifecycle()

    var showPermissionDeniedDialog by remember { mutableStateOf(false) }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview(),
        onResult = { bitmap ->
            if (bitmap != null) {
                viewModel.processImageBitmap(bitmap)
            }
        }
    )

    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            if (isGranted) {
                cameraLauncher.launch(null)
            } else {
                showPermissionDeniedDialog = true
            }
        }
    )

    // Gallery / Photos launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                viewModel.processImageUri(context, uri)
            }
        }
    )

    fun launchCamera() {
        val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
        if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
            cameraLauncher.launch(null)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    fun launchGallery() {
        photoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Image OCR / Scanner", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (imageStatus != ImageStatus.IDLE) {
                        IconButton(onClick = { viewModel.clearImageOcrData() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Reset Image Scan", tint = Color(0xFF64748B))
                        }
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (imageStatus) {
                ImageStatus.IDLE -> {
                    IdleStateView(
                        onTakePhoto = { launchCamera() },
                        onUploadImage = { launchGallery() }
                    )
                }

                ImageStatus.PROCESSING -> {
                    ProcessingStateView(stage = processingStage)
                }

                ImageStatus.REJECTED -> {
                    RejectedStateView(
                        errorMessage = imageErrorMessage ?: "We could not detect a valid DFA in this image.\nPlease upload a DFA diagram, transition table, or automata question.",
                        onChooseAnother = {
                            viewModel.clearImageOcrData()
                            launchGallery()
                        },
                        onTakePhoto = {
                            viewModel.clearImageOcrData()
                            launchCamera()
                        },
                        onEnterManually = {
                            onNavigate(NavRoutes.INPUT_MANUAL)
                        }
                    )
                }

                ImageStatus.ERROR -> {
                    ErrorStateView(
                        errorMessage = imageErrorMessage ?: "An error occurred during image processing.",
                        onRetry = { viewModel.clearImageOcrData() },
                        onEnterManually = { onNavigate(NavRoutes.INPUT_MANUAL) }
                    )
                }

                ImageStatus.DETECTED, ImageStatus.PARTIAL -> {
                    val dfa = detectedDfa
                    if (dfa != null) {
                        DetectedDfaVerificationView(
                            status = imageStatus,
                            dfa = dfa,
                            ocrConfidence = ocrConfidence,
                            dfaDetectionConfidence = dfaDetectionConfidence,
                            rawOcrText = ocrRawText,
                            previewBitmap = selectedBitmap,
                            onUpdateDfa = { updated -> viewModel.updateDetectedDfa(updated) },
                            onConfirm = { confirmedDfa ->
                                if (viewModel.confirmDetectedDfa(confirmedDfa)) {
                                    onNavigate(NavRoutes.DFA_VIEW)
                                }
                            },
                            onChangeImage = { viewModel.clearImageOcrData() }
                        )
                    } else {
                        // In case DFA was null despite status
                        RejectedStateView(
                            errorMessage = "No valid DFA could be constructed from the detected text.",
                            onChooseAnother = { viewModel.clearImageOcrData() },
                            onTakePhoto = { launchCamera() },
                            onEnterManually = { onNavigate(NavRoutes.INPUT_MANUAL) }
                        )
                    }
                }
            }
        }
    }

    if (showPermissionDeniedDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDeniedDialog = false },
            title = { Text("Camera Permission Required", fontWeight = FontWeight.Bold) },
            text = {
                Text("Camera access is needed to capture photos of DFA diagrams. You can also select an image directly from your gallery.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPermissionDeniedDialog = false
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                ) {
                    Text("Grant Permission")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPermissionDeniedDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// -----------------------------------------------------------------------------------------
// 1. IDLE STATE: No image selected, Take Photo, Upload Image
// -----------------------------------------------------------------------------------------
@Composable
private fun IdleStateView(
    onTakePhoto: () -> Unit,
    onUploadImage: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(Color(0xFFEFF6FF), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AddPhotoAlternate,
                    contentDescription = null,
                    tint = Color(0xFF2563EB),
                    modifier = Modifier.size(40.dp)
                )
            }

            Text(
                text = "No image selected",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color(0xFF0F172A)
            )

            Text(
                text = "Take a photo or upload an image of a DFA transition diagram, transition table, or automata textbook problem.",
                fontSize = 13.sp,
                color = Color(0xFF64748B),
                lineHeight = 18.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onTakePhoto,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("take_photo_button")
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Take Photo")
                }

                OutlinedButton(
                    onClick = onUploadImage,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF2563EB)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2563EB)),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("upload_image_button")
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Upload Image")
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// 2. PROCESSING STATE: 4-stage loader
// -----------------------------------------------------------------------------------------
@Composable
private fun ProcessingStateView(stage: ProcessingStage) {
    val stages = listOf(
        ProcessingStage.PREPARING_IMAGE,
        ProcessingStage.READING_TEXT,
        ProcessingStage.DETECTING_DFA,
        ProcessingStage.VALIDATING_DFA
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                color = Color(0xFF2563EB),
                strokeWidth = 3.dp,
                modifier = Modifier.size(48.dp)
            )

            Text(
                text = "Processing DFA Image...",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = Color(0xFF0F172A)
            )

            Text(
                text = "Analyzing characters, transition arrows, and state definitions.",
                fontSize = 12.sp,
                color = Color(0xFF64748B)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                stages.forEachIndexed { index, s ->
                    val isDone = s.ordinal < stage.ordinal
                    val isCurrent = s == stage

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(
                                    when {
                                        isDone -> Color(0xFF10B981)
                                        isCurrent -> Color(0xFF2563EB)
                                        else -> Color(0xFFE2E8F0)
                                    },
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isDone) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                            } else {
                                Text(
                                    text = "${index + 1}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isCurrent) Color.White else Color(0xFF64748B)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = s.label,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp,
                            color = when {
                                isDone -> Color(0xFF10B981)
                                isCurrent -> Color(0xFF0F172A)
                                else -> Color(0xFF94A3B8)
                            }
                        )
                    }
                }
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// 3. REJECTED STATE: Image not accepted, choose another or enter manually
// -----------------------------------------------------------------------------------------
@Composable
private fun RejectedStateView(
    errorMessage: String,
    onChooseAnother: () -> Unit,
    onTakePhoto: () -> Unit,
    onEnterManually: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFFECACA)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(Color(0xFFFEE2E2), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.ErrorOutline,
                    contentDescription = null,
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(36.dp)
                )
            }

            Text(
                text = "IMAGE NOT ACCEPTED",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = Color(0xFFDC2626)
            )

            Text(
                text = errorMessage,
                fontSize = 13.sp,
                color = Color(0xFF475569),
                lineHeight = 18.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Button(
                onClick = onChooseAnother,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("choose_another_image_button")
            ) {
                Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Choose Another Image")
            }

            OutlinedButton(
                onClick = onTakePhoto,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Take New Photo")
            }

            TextButton(
                onClick = onEnterManually,
                modifier = Modifier.testTag("enter_manually_button")
            ) {
                Text("Enter DFA Manually", color = Color(0xFF2563EB), fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// 4. ERROR STATE: Processing failure
// -----------------------------------------------------------------------------------------
@Composable
private fun ErrorStateView(
    errorMessage: String,
    onRetry: () -> Unit,
    onEnterManually: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFFECACA)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEA580C), modifier = Modifier.size(48.dp))
            Text("Image Processing Error", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF0F172A))
            Text(errorMessage, fontSize = 13.sp, color = Color(0xFF64748B), textAlign = androidx.compose.ui.text.style.TextAlign.Center)

            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Try Again")
            }

            TextButton(onClick = onEnterManually) {
                Text("Enter DFA Manually")
            }
        }
    }
}

// -----------------------------------------------------------------------------------------
// 5. DETECTED / PARTIAL STATE: Editable verification form
// -----------------------------------------------------------------------------------------
@Composable
private fun DetectedDfaVerificationView(
    status: ImageStatus,
    dfa: DFA,
    ocrConfidence: Float,
    dfaDetectionConfidence: Float,
    rawOcrText: String,
    previewBitmap: Bitmap?,
    onUpdateDfa: (DFA) -> Unit,
    onConfirm: (DFA) -> Unit,
    onChangeImage: () -> Unit
) {
    var statesList by remember(dfa) { mutableStateOf(dfa.states) }
    var alphabetList by remember(dfa) { mutableStateOf(dfa.alphabet) }
    var transitionsList by remember(dfa) { mutableStateOf(dfa.transitions) }

    var newStateName by remember { mutableStateOf("") }
    var alphabetText by remember(dfa) { mutableStateOf(dfa.alphabet.joinToString(", ")) }

    var newFromState by remember { mutableStateOf("") }
    var newSymbol by remember { mutableStateOf("") }
    var newToState by remember { mutableStateOf("") }

    var showRawOcr by remember { mutableStateOf(false) }

    fun syncDfa(
        newStates: List<DFAState> = statesList,
        newAlpha: List<String> = alphabetList,
        newTrans: List<Transition> = transitionsList
    ) {
        val updated = DFA(newStates, newAlpha, newTrans)
        onUpdateDfa(updated)
    }

    // Live validation check
    val currentConstructedDfa = DFA(statesList, alphabetList, transitionsList)
    val validationResult = DFAValidator.validate(currentConstructedDfa)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Status & Confidence Header
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (status == ImageStatus.DETECTED) Color(0xFFF0FDF4) else Color(0xFFFFFBEB),
                border = BorderStroke(1.dp, if (status == ImageStatus.DETECTED) Color(0xFFBBF7D0) else Color(0xFFFDE68A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (status == ImageStatus.DETECTED) Icons.Default.CheckCircle else Icons.Default.Info,
                                contentDescription = null,
                                tint = if (status == ImageStatus.DETECTED) Color(0xFF16A34A) else Color(0xFFD97706),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (status == ImageStatus.DETECTED) "DFA Detected Successfully" else "Partial DFA Detected",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = if (status == ImageStatus.DETECTED) Color(0xFF15803D) else Color(0xFFB45309)
                            )
                        }

                        TextButton(
                            onClick = onChangeImage,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Change Image", fontSize = 11.sp, color = Color(0xFF2563EB))
                        }
                    }

                    if (status == ImageStatus.PARTIAL) {
                        Text(
                            text = "Some properties or transitions could not be fully inferred. Please complete or adjust the details below before confirming.",
                            fontSize = 12.sp,
                            color = Color(0xFF92400E)
                        )
                    }

                    // Two separate confidence badges
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("OCR Text", fontSize = 10.sp, color = Color(0xFF64748B))
                                Text(
                                    text = "${(ocrConfidence * 100).toInt()}% Confidence",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("DFA Structure", fontSize = 10.sp, color = Color(0xFF64748B))
                                Text(
                                    text = "${(dfaDetectionConfidence * 100).toInt()}% Match",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (dfaDetectionConfidence >= 0.7f) Color(0xFF15803D) else Color(0xFFB45309)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Preview image & OCR text toggle
        item {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (previewBitmap != null) {
                        Image(
                            bitmap = previewBitmap.asImageBitmap(),
                            contentDescription = "Uploaded DFA Preview",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showRawOcr = !showRawOcr }
                    ) {
                        Text(
                            text = "Extracted OCR Text",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF64748B)
                        )
                        Icon(
                            if (showRawOcr) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8)
                        )
                    }

                    AnimatedVisibility(visible = showRawOcr) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFF1F5F9),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = rawOcrText.ifBlank { "No text extracted" },
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = Color(0xFF334155),
                                modifier = Modifier.padding(8.dp)
                            )
                        }
                    }
                }
            }
        }

        // 1. States Editor
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("1. States & Roles", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                    Text("Tap Radio to set Start State; tap Checkbox for Final States.", fontSize = 11.sp, color = Color(0xFF64748B))

                    statesList.forEach { state ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = state.id,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color(0xFF0F172A),
                                    modifier = Modifier.width(44.dp)
                                )

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable {
                                        statesList = statesList.map { it.copy(isStart = it.id == state.id) }
                                        syncDfa(newStates = statesList)
                                    }
                                ) {
                                    RadioButton(
                                        selected = state.isStart,
                                        onClick = {
                                            statesList = statesList.map { it.copy(isStart = it.id == state.id) }
                                            syncDfa(newStates = statesList)
                                        }
                                    )
                                    Text("Start", fontSize = 12.sp)
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable {
                                        statesList = statesList.map {
                                            if (it.id == state.id) it.copy(isFinal = !it.isFinal) else it
                                        }
                                        syncDfa(newStates = statesList)
                                    }
                                ) {
                                    Checkbox(
                                        checked = state.isFinal,
                                        onCheckedChange = { checked ->
                                            statesList = statesList.map {
                                                if (it.id == state.id) it.copy(isFinal = checked) else it
                                            }
                                            syncDfa(newStates = statesList)
                                        }
                                    )
                                    Text("Final", fontSize = 12.sp)
                                }

                                Spacer(modifier = Modifier.weight(1f))

                                IconButton(
                                    onClick = {
                                        statesList = statesList.filter { it.id != state.id }
                                        transitionsList = transitionsList.filter { it.from != state.id && it.to != state.id }
                                        syncDfa(newStates = statesList, newTrans = transitionsList)
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete State", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    // Add State Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newStateName,
                            onValueChange = { newStateName = it },
                            label = { Text("New State ID") },
                            placeholder = { Text("e.g. q3") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                val clean = newStateName.trim()
                                if (clean.isNotBlank() && statesList.none { it.id == clean }) {
                                    statesList = statesList + DFAState(clean, isStart = statesList.isEmpty(), isFinal = false)
                                    syncDfa(newStates = statesList)
                                    newStateName = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                        ) {
                            Text("Add")
                        }
                    }
                }
            }
        }

        // 2. Alphabet Editor
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("2. Alphabet (Σ)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))
                    OutlinedTextField(
                        value = alphabetText,
                        onValueChange = { text ->
                            alphabetText = text
                            val parsed = text.split("[,;\\s]+".toRegex())
                                .map { it.trim() }
                                .filter { it.isNotBlank() }
                                .distinct()
                            alphabetList = parsed
                            syncDfa(newAlpha = parsed)
                        },
                        label = { Text("Symbols (comma-separated)") },
                        placeholder = { Text("e.g. a, b or 0, 1") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // 3. Transitions Editor
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color.White,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("3. Transitions (δ)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF0F172A))

                    if (transitionsList.isEmpty()) {
                        Text("No transitions added yet.", fontSize = 12.sp, color = Color(0xFF94A3B8))
                    } else {
                        transitionsList.forEach { trans ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "δ(${trans.from}, ${trans.symbol}) = ${trans.to}",
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF2563EB),
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = {
                                            transitionsList = transitionsList.filter { it != trans }
                                            syncDfa(newTrans = transitionsList)
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete Transition", tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }

                    // Add Transition Form
                    Text("Add Transition:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF475569))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newFromState,
                            onValueChange = { newFromState = it },
                            label = { Text("From") },
                            placeholder = { Text("q0") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = newSymbol,
                            onValueChange = { newSymbol = it },
                            label = { Text("Sym") },
                            placeholder = { Text("a") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = newToState,
                            onValueChange = { newToState = it },
                            label = { Text("To") },
                            placeholder = { Text("q1") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                val from = newFromState.trim()
                                val sym = newSymbol.trim()
                                val to = newToState.trim()
                                if (from.isNotBlank() && sym.isNotBlank() && to.isNotBlank()) {
                                    // Remove any existing transition for same (from, sym) to enforce determinism
                                    val filtered = transitionsList.filterNot { it.from == from && it.symbol == sym }
                                    transitionsList = filtered + Transition(from, sym, to)
                                    syncDfa(newTrans = transitionsList)
                                    newFromState = ""
                                    newSymbol = ""
                                    newToState = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                        ) {
                            Text("+")
                        }
                    }
                }
            }
        }

        // Validation Errors (if any)
        if (!validationResult.isValid) {
            item {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFEF2F2),
                    border = BorderStroke(1.dp, Color(0xFFFECACA)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Error, contentDescription = null, tint = Color(0xFFDC2626), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Validation Errors Before Confirming:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFFDC2626))
                        }
                        validationResult.errors.forEach { err ->
                            Text("• $err", fontSize = 12.sp, color = Color(0xFF991B1B))
                        }
                    }
                }
            }
        }

        // Confirm Button
        item {
            Button(
                onClick = {
                    onConfirm(currentConstructedDfa)
                },
                enabled = validationResult.isValid,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("confirm_dfa_button")
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Confirm & View DFA Diagram", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
