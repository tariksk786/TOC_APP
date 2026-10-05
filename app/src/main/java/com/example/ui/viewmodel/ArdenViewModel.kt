package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.SolutionEntity
import com.example.data.SolutionRepository
import com.example.model.*
import com.example.solver.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.example.ocr.*

enum class ImageStatus {
    IDLE,
    PROCESSING,
    DETECTED,
    PARTIAL,
    REJECTED,
    ERROR
}

enum class ProcessingStage(val label: String) {
    PREPARING_IMAGE("Preparing Image"),
    READING_TEXT("Reading Text"),
    DETECTING_DFA("Detecting DFA"),
    VALIDATING_DFA("Validating DFA")
}

class ArdenViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SolutionRepository = SolutionRepository(
        AppDatabase.getDatabase(application).solutionDao()
    )

    val historySolutions: StateFlow<List<SolutionEntity>> =
        MutableStateFlow<List<SolutionEntity>>(emptyList()).also { stateFlow ->
            viewModelScope.launch {
                repository.allSolutions.collect { list ->
                    stateFlow.value = list
                }
            }
        }

    // Current DFA state
    private val _currentDfa = MutableStateFlow(DFAExamples.demoProblem.dfa)
    val currentDfa: StateFlow<DFA> = _currentDfa.asStateFlow()

    // Validation Result
    private val _validationResult = MutableStateFlow<ValidationResult?>(null)
    val validationResult: StateFlow<ValidationResult?> = _validationResult.asStateFlow()

    // Arden Solution
    private val _solution = MutableStateFlow<ArdenSolution?>(null)
    val solution: StateFlow<ArdenSolution?> = _solution.asStateFlow()

    // Step Viewer Index (0-indexed)
    private val _currentStepIndex = MutableStateFlow(0)
    val currentStepIndex: StateFlow<Int> = _currentStepIndex.asStateFlow()

    // Verification results
    private val _verificationResults = MutableStateFlow<List<VerificationItem>>(emptyList())
    val verificationResults: StateFlow<List<VerificationItem>> = _verificationResults.asStateFlow()

    // Simulation State
    private val _simulationString = MutableStateFlow("a")
    val simulationString: StateFlow<String> = _simulationString.asStateFlow()

    private val _simulationSteps = MutableStateFlow<List<SimulationStep>>(emptyList())
    val simulationSteps: StateFlow<List<SimulationStep>> = _simulationSteps.asStateFlow()

    private val _currentSimStepIndex = MutableStateFlow(0)
    val currentSimStepIndex: StateFlow<Int> = _currentSimStepIndex.asStateFlow()

    private val _isSimPlaying = MutableStateFlow(false)
    val isSimPlaying: StateFlow<Boolean> = _isSimPlaying.asStateFlow()

    private var simJob: Job? = null

    // Message / Error
    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    // Real Image OCR States
    private val _imageStatus = MutableStateFlow(ImageStatus.IDLE)
    val imageStatus: StateFlow<ImageStatus> = _imageStatus.asStateFlow()

    private val _processingStage = MutableStateFlow(ProcessingStage.PREPARING_IMAGE)
    val processingStage: StateFlow<ProcessingStage> = _processingStage.asStateFlow()

    private val _ocrConfidence = MutableStateFlow(0f)
    val ocrConfidence: StateFlow<Float> = _ocrConfidence.asStateFlow()

    private val _dfaDetectionConfidence = MutableStateFlow(0f)
    val dfaDetectionConfidence: StateFlow<Float> = _dfaDetectionConfidence.asStateFlow()

    private val _ocrRawText = MutableStateFlow("")
    val ocrRawText: StateFlow<String> = _ocrRawText.asStateFlow()

    private val _detectedDfa = MutableStateFlow<DFA?>(null)
    val detectedDfa: StateFlow<DFA?> = _detectedDfa.asStateFlow()

    private val _selectedImageBitmap = MutableStateFlow<Bitmap?>(null)
    val selectedImageBitmap: StateFlow<Bitmap?> = _selectedImageBitmap.asStateFlow()

    private val _imageErrorMessage = MutableStateFlow<String?>(null)
    val imageErrorMessage: StateFlow<String?> = _imageErrorMessage.asStateFlow()

    init {
        // Automatically validate default demo DFA
        validateCurrentDfa()
    }

    fun setDFA(dfa: DFA) {
        _currentDfa.value = dfa
        validateCurrentDfa()
        _solution.value = null
        _currentStepIndex.value = 0
        _verificationResults.value = emptyList()
        resetSimulation()
    }

    fun validateCurrentDfa(): Boolean {
        val result = DFAValidator.validate(_currentDfa.value)
        _validationResult.value = result
        return result.isValid
    }

    fun solveDfa(): Boolean {
        if (!validateCurrentDfa()) return false
        try {
            val sol = ArdenSolver.solve(_currentDfa.value)
            _solution.value = sol
            _currentStepIndex.value = 0
            // Also pre-compute verification items
            _verificationResults.value = VerificationEngine.verify(sol.dfa, sol.finalRegex)
            // Auto save to history
            saveCurrentSolutionToHistory()
            return true
        } catch (e: Exception) {
            _userMessage.value = "Solver error: ${e.localizedMessage ?: "Unknown mathematical error"}"
            return false
        }
    }

    fun nextStep() {
        val total = _solution.value?.steps?.size ?: 0
        if (_currentStepIndex.value < total - 1) {
            _currentStepIndex.value += 1
        }
    }

    fun previousStep() {
        if (_currentStepIndex.value > 0) {
            _currentStepIndex.value -= 1
        }
    }

    fun jumpToStep(index: Int) {
        val total = _solution.value?.steps?.size ?: 0
        if (index in 0 until total) {
            _currentStepIndex.value = index
        }
    }

    // Simulation
    fun setSimulationString(str: String) {
        _simulationString.value = str
        resetSimulation()
    }

    fun prepareSimulation() {
        val dfa = _currentDfa.value
        val steps = DFASimulator.trace(dfa, _simulationString.value)
        _simulationSteps.value = steps
        _currentSimStepIndex.value = 0
    }

    fun nextSimStep() {
        val steps = _simulationSteps.value
        if (_currentSimStepIndex.value < steps.size - 1) {
            _currentSimStepIndex.value += 1
        } else {
            pauseSimulation()
        }
    }

    fun previousSimStep() {
        if (_currentSimStepIndex.value > 0) {
            _currentSimStepIndex.value -= 1
        }
    }

    fun resetSimulation() {
        pauseSimulation()
        prepareSimulation()
    }

    fun toggleSimPlay() {
        if (_isSimPlaying.value) {
            pauseSimulation()
        } else {
            startSimulation()
        }
    }

    private fun startSimulation() {
        _isSimPlaying.value = true
        simJob?.cancel()
        simJob = viewModelScope.launch {
            while (_isSimPlaying.value && _currentSimStepIndex.value < (_simulationSteps.value.size - 1)) {
                delay(800)
                nextSimStep()
            }
            _isSimPlaying.value = false
        }
    }

    private fun pauseSimulation() {
        _isSimPlaying.value = false
        simJob?.cancel()
        simJob = null
    }

    fun resetAll() {
        pauseSimulation()
        _currentDfa.value = DFAExamples.demoProblem.dfa
        _validationResult.value = null
        _solution.value = null
        _currentStepIndex.value = 0
        _verificationResults.value = emptyList()
        _simulationString.value = "a"
        _simulationSteps.value = emptyList()
        _currentSimStepIndex.value = 0
        validateCurrentDfa()
    }

    fun clearUserMessage() {
        _userMessage.value = null
    }

    fun loadExample(example: ExampleProblem) {
        setDFA(example.dfa)
        solveDfa()
    }

    fun loadFromHistory(history: SolutionEntity) {
        val parsed = DFAParser.parse(history.compactDfa)
        setDFA(parsed)
        solveDfa()
    }

    fun deleteHistory(id: Long) {
        viewModelScope.launch {
            repository.deleteSolution(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    private fun saveCurrentSolutionToHistory() {
        val sol = _solution.value ?: return
        viewModelScope.launch {
            val entity = SolutionEntity(
                title = "DFA (${sol.dfa.states.size} States)",
                stateCount = sol.dfa.states.size,
                alphabetString = sol.dfa.alphabet.joinToString(", "),
                finalRegex = sol.finalRegexString,
                compactDfa = DFAParser.toCompactString(sol.dfa)
            )
            repository.saveSolution(entity)
        }
    }

    // --- Real Image OCR / Camera DFA Detection Pipeline ---

    fun clearImageOcrData() {
        _imageStatus.value = ImageStatus.IDLE
        _processingStage.value = ProcessingStage.PREPARING_IMAGE
        _ocrConfidence.value = 0f
        _dfaDetectionConfidence.value = 0f
        _ocrRawText.value = ""
        _detectedDfa.value = null
        _selectedImageBitmap.value = null
        _imageErrorMessage.value = null
        // Clear previous solution, equations, solver steps, final regex, errors
        _solution.value = null
        _currentStepIndex.value = 0
        _verificationResults.value = emptyList()
        _simulationSteps.value = emptyList()
        _userMessage.value = null
    }

    fun processImageBitmap(bitmap: Bitmap) {
        clearImageOcrData()
        _selectedImageBitmap.value = bitmap
        _imageStatus.value = ImageStatus.PROCESSING

        viewModelScope.launch {
            _processingStage.value = ProcessingStage.PREPARING_IMAGE
            delay(150)

            _processingStage.value = ProcessingStage.READING_TEXT
            val ocrRes = OcrService.recognizeText(bitmap)
            if (ocrRes.isFailure) {
                _imageStatus.value = ImageStatus.ERROR
                _imageErrorMessage.value = "Failed to process image: ${ocrRes.exceptionOrNull()?.localizedMessage ?: "OCR error"}"
                return@launch
            }

            val ocrResult = ocrRes.getOrNull()
            if (ocrResult == null || ocrResult.text.isBlank()) {
                _imageStatus.value = ImageStatus.REJECTED
                _imageErrorMessage.value = "We could not detect a valid DFA in this image.\nPlease upload a DFA diagram, transition table, or automata question."
                return@launch
            }

            _ocrRawText.value = ocrResult.text
            _ocrConfidence.value = ocrResult.confidence

            _processingStage.value = ProcessingStage.DETECTING_DFA
            delay(150)
            val parseResult = DFAImageParser.parse(ocrResult.text)

            _processingStage.value = ProcessingStage.VALIDATING_DFA
            delay(150)

            _dfaDetectionConfidence.value = parseResult.confidence

            when (parseResult.status) {
                ParseStatus.REJECTED -> {
                    _imageStatus.value = ImageStatus.REJECTED
                    _detectedDfa.value = null
                    _imageErrorMessage.value = parseResult.message
                }
                ParseStatus.PARTIAL -> {
                    _imageStatus.value = ImageStatus.PARTIAL
                    _detectedDfa.value = parseResult.dfa
                }
                ParseStatus.DETECTED -> {
                    _imageStatus.value = ImageStatus.DETECTED
                    _detectedDfa.value = parseResult.dfa
                }
            }
        }
    }

    fun processImageUri(context: Context, uri: Uri) {
        clearImageOcrData()
        _imageStatus.value = ImageStatus.PROCESSING

        viewModelScope.launch {
            _processingStage.value = ProcessingStage.PREPARING_IMAGE
            delay(150)

            _processingStage.value = ProcessingStage.READING_TEXT
            val ocrRes = OcrService.recognizeText(context, uri)
            if (ocrRes.isFailure) {
                _imageStatus.value = ImageStatus.ERROR
                _imageErrorMessage.value = "Failed to process image: ${ocrRes.exceptionOrNull()?.localizedMessage ?: "OCR error"}"
                return@launch
            }

            val ocrResult = ocrRes.getOrNull()
            if (ocrResult == null || ocrResult.text.isBlank()) {
                _imageStatus.value = ImageStatus.REJECTED
                _imageErrorMessage.value = "We could not detect a valid DFA in this image.\nPlease upload a DFA diagram, transition table, or automata question."
                return@launch
            }

            _ocrRawText.value = ocrResult.text
            _ocrConfidence.value = ocrResult.confidence

            _processingStage.value = ProcessingStage.DETECTING_DFA
            delay(150)
            val parseResult = DFAImageParser.parse(ocrResult.text)

            _processingStage.value = ProcessingStage.VALIDATING_DFA
            delay(150)

            _dfaDetectionConfidence.value = parseResult.confidence

            when (parseResult.status) {
                ParseStatus.REJECTED -> {
                    _imageStatus.value = ImageStatus.REJECTED
                    _detectedDfa.value = null
                    _imageErrorMessage.value = parseResult.message
                }
                ParseStatus.PARTIAL -> {
                    _imageStatus.value = ImageStatus.PARTIAL
                    _detectedDfa.value = parseResult.dfa
                }
                ParseStatus.DETECTED -> {
                    _imageStatus.value = ImageStatus.DETECTED
                    _detectedDfa.value = parseResult.dfa
                }
            }
        }
    }

    fun updateDetectedDfa(dfa: DFA) {
        _detectedDfa.value = dfa
    }

    fun confirmDetectedDfa(dfa: DFA): Boolean {
        val valResult = DFAValidator.validate(dfa)
        if (!valResult.isValid) {
            _userMessage.value = valResult.errors.firstOrNull() ?: "Invalid DFA definition"
            return false
        }
        setDFA(dfa)
        return true
    }
}
