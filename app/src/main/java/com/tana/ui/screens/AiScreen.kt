package com.tana.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentSize
import java.text.NumberFormat
import java.util.Locale
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.tana.data.model.AgentRoutineEntity
import com.tana.data.model.AgentRoutineType
import com.tana.data.model.AiMessageEntity
import com.tana.data.model.TransactionType
import com.tana.ui.theme.AccentExpense
import com.tana.ui.theme.AccentIncome
import com.tana.ui.theme.AccentSavings
import com.tana.ui.theme.BrandPrimaryBright
import com.tana.ui.viewmodel.FinanceViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiScreen(
    aiMessages: List<AiMessageEntity>,
    isAiLoading: Boolean,
    openRouterApiKey: String,
    openRouterModel: String,
    agentRoutines: List<AgentRoutineEntity> = emptyList(),
    onSendMessage: (prompt: String, mediaName: String?, mediaType: String?, attachedImageUri: String) -> Unit,
    onClearHistory: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onAddGoalFromAi: ((title: String, target: Double, initial: Double, planAmt: Double, planFreq: String, activeDays: Set<Int>, reminder: String, note: String) -> Unit)? = null,
    onDeleteGoalFromAi: ((goalName: String) -> Unit)? = null,
    onUpdateGoalFromAi: ((goalName: String, newTarget: Double, newPlan: Double) -> Unit)? = null,
    onFixSavingsTransactionFromAi: ((goalName: String, newAmount: Double, newNote: String) -> Unit)? = null,
    onDepositFromAi: ((goalName: String, amount: Double, note: String) -> Unit)? = null,
    onWithdrawFromAi: ((goalName: String, amount: Double, note: String) -> Unit)? = null,
    onAddTransactionFromAi: ((type: TransactionType, amount: Double, category: String, note: String) -> Unit)? = null,
    onCreateAgentRoutine: ((type: AgentRoutineType, title: String, targetGoalName: String?, amount: Double, activeDaysCsv: String, desc: String) -> Unit)? = null,
    onStopAgentRoutine: ((query: String) -> Unit)? = null,
    onToggleAgentRoutine: ((id: Long, enabled: Boolean) -> Unit)? = null,
    onDeleteAgentRoutine: ((id: Long) -> Unit)? = null,
    onExecuteRoutinesNow: (() -> Unit)? = null,
    onUpdateAiMessageContent: ((Long, String) -> Unit)? = null,
    onStopAiGeneration: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }

    var inputText by remember { mutableStateOf("") }
    var attachedMediaName by remember { mutableStateOf<String?>(null) }
    var attachedImageUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var attachedImageBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var attachedMediaType by remember { mutableStateOf<String?>(null) }
    var showAttachmentSheet by remember { mutableStateOf(false) }
    var showAgentRoutinesSheet by remember { mutableStateOf(false) }
    var isRecordingVoice by remember { mutableStateOf(false) }
    var recordedVoiceText by remember { mutableStateOf("") }
    var recordingDurationSeconds by remember { mutableIntStateOf(0) }
    var currentSoundLevel by remember { mutableStateOf(0f) }
    var speechStatusText by remember { mutableStateOf("Mendengarkan suara...") }

    val savedGoalMessageIds = remember { mutableStateMapOf<Long, Boolean>() }
    val savedDepositMessageIds = remember { mutableStateMapOf<Long, Boolean>() }
    val savedWithdrawMessageIds = remember { mutableStateMapOf<Long, Boolean>() }
    val savedTransactionMessageIds = remember { mutableStateMapOf<Long, Boolean>() }
    val savedRoutineMessageIds = remember { mutableStateMapOf<Long, Boolean>() }
    val savedStopRoutineMessageIds = remember { mutableStateMapOf<Long, Boolean>() }
    val savedDeleteGoalMessageIds = remember { mutableStateMapOf<Long, Boolean>() }
    val savedUpdateGoalMessageIds = remember { mutableStateMapOf<Long, Boolean>() }
    val savedFixTxMessageIds = remember { mutableStateMapOf<Long, Boolean>() }

    val cancelledGoalMessageIds = remember { mutableStateMapOf<Long, Boolean>() }
    val cancelledDepositMessageIds = remember { mutableStateMapOf<Long, Boolean>() }
    val cancelledWithdrawMessageIds = remember { mutableStateMapOf<Long, Boolean>() }
    val cancelledTransactionMessageIds = remember { mutableStateMapOf<Long, Boolean>() }
    val cancelledRoutineMessageIds = remember { mutableStateMapOf<Long, Boolean>() }
    val cancelledStopRoutineMessageIds = remember { mutableStateMapOf<Long, Boolean>() }
    val cancelledDeleteGoalMessageIds = remember { mutableStateMapOf<Long, Boolean>() }
    val cancelledUpdateGoalMessageIds = remember { mutableStateMapOf<Long, Boolean>() }
    val cancelledFixTxMessageIds = remember { mutableStateMapOf<Long, Boolean>() }

    val activeRoutinesCount = agentRoutines.count { it.isEnabled && it.isApproved }

    BackHandler(enabled = showAttachmentSheet || isRecordingVoice || showAgentRoutinesSheet) {
        showAttachmentSheet = false
        showAgentRoutinesSheet = false
        isRecordingVoice = false
    }

    // Permission launcher for in-app voice recording
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudioPermission = granted
        if (granted) {
            isRecordingVoice = true
        }
    }

    // Camera Photo Capture Launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            try {
                val cacheFile = java.io.File(context.cacheDir, "camera_photo_${System.currentTimeMillis()}.jpg")
                val out = java.io.FileOutputStream(cacheFile)
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                out.flush()
                out.close()
                attachedImageUri = Uri.fromFile(cacheFile)
                attachedImageBitmap = bitmap
                attachedMediaType = "image"
                attachedMediaName = cacheFile.name
            } catch (e: Exception) {
                attachedImageBitmap = bitmap
                attachedImageUri = null
                attachedMediaType = "image"
                attachedMediaName = "Foto_Kamera_Struk_${System.currentTimeMillis() % 1000}.jpg"
            }
            showAttachmentSheet = false
        }
    }

    // Image Gallery Picker Launcher
    val imageGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            attachedImageUri = uri
            attachedImageBitmap = null
            attachedMediaType = "image"
            attachedMediaName = "Struk_Galeri_${System.currentTimeMillis() % 1000}.jpg"
            showAttachmentSheet = false
        }
    }

    // Document / File Picker Launcher
    val documentPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            attachedImageUri = uri
            attachedMediaType = "document"
            attachedMediaName = "Dokumen_Keuangan_${System.currentTimeMillis() % 1000}.pdf"
            showAttachmentSheet = false
        }
    }

    // Video Gallery Picker Launcher
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            attachedImageUri = uri
            attachedMediaType = "video"
            attachedMediaName = "Video_Finansial_${System.currentTimeMillis() % 1000}.mp4"
            showAttachmentSheet = false
        }
    }

    // Voice recording timer effect
    LaunchedEffect(isRecordingVoice) {
        if (isRecordingVoice) {
            recordingDurationSeconds = 0
            while (isRecordingVoice) {
                delay(1000)
                recordingDurationSeconds++
            }
        }
    }

    // Background In-App Speech Recognizer (No Google Dialog)
    DisposableEffect(isRecordingVoice) {
        
        if (isRecordingVoice && SpeechRecognizer.isRecognitionAvailable(context)) {
            speechStatusText = "Mendengarkan suara..."
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        speechStatusText = "Mikrofon siap, silakan bicara..."
                    }
                    override fun onBeginningOfSpeech() {
                        speechStatusText = "Mendeteksi suara..."
                    }
                    override fun onRmsChanged(rmsdB: Float) {
                        // Normalize sound level 0..1f for UI waveform animation
                        val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                        currentSoundLevel = normalized
                    }
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        speechStatusText = "Memproses ucapan..."
                    }
                    override fun onError(error: Int) {
                        val errorMsg = when (error) {
                            SpeechRecognizer.ERROR_NO_MATCH -> "Suara tidak terdeteksi, coba lagi"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Waktu bicara habis"
                            SpeechRecognizer.ERROR_AUDIO -> "Gangguan audio mikrofon"
                            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Gangguan koneksi pengenalan suara"
                            else -> "Siap menerima suara"
                        }
                        speechStatusText = errorMsg
                        if (recordedVoiceText.isBlank()) {
                            recordedVoiceText = "Berapa total pengeluaran dan sisa saldo saya?"
                        }
                    }
                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            recordedVoiceText = matches[0]
                            speechStatusText = "Ucapan berhasil ditangkap!"
                        }
                    }
                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            recordedVoiceText = matches[0]
                            speechStatusText = "Mendengarkan: \"${matches[0]}\""
                        }
                    }
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "id-ID")
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                }
                try {
                    startListening(intent)
                } catch (_: Exception) {}
            }
        }

        onDispose {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.destroy()
            } catch (_: Exception) {}
        }
    }

    // Auto scroll to bottom when new messages arrive, streaming content updates, or loading state changes
    val lastMessageContentLength = aiMessages.lastOrNull()?.content?.length ?: 0
    LaunchedEffect(aiMessages.size, isAiLoading, lastMessageContentLength) {
        if (aiMessages.isNotEmpty()) {
            listState.scrollToItem(aiMessages.size - 1)
        }
    }

    val quickPrompts = listOf(
        "Analisis kebiasaan pengeluaran dan arus kas saya",
        "Bagaimana cara memotong 20% pengeluaran terbesar?",
        "Buatkan rencana alokasi anggaran 50/30/20 dari saldo saya",
        "Berapa lama target tabungan aktif saya bisa tercapai?",
        "Analisis struk pembelian dan buatkan target tabungan baru"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // ChatGPT Minimalist Top Bar with Title, Autonomous Agent Badge, and Tune Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = "AI",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "TANA AI",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Autonomous AI Agent",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Agent Routines Quick Chip
                    Surface(
                        onClick = { showAgentRoutinesSheet = true },
                        color = if (activeRoutinesCount > 0) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SmartToy,
                                contentDescription = "Agen",
                                tint = if (activeRoutinesCount > 0) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "$activeRoutinesCount Agen Aktif",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.5.sp),
                                color = if (activeRoutinesCount > 0) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onNavigateToSettings,
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
                            .testTag("ai_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Tune,
                            contentDescription = "Pengaturan",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

                // Messages List
                Box(modifier = Modifier.weight(1f)) {
                    // Compiled ONCE per composition lifetime (not per message, not per
                    // recomposition) - previously these 10 Regex objects were rebuilt and
                    // re-run for every visible chat message on every recomposition
                    // (including every single keystroke in the input field below, since
                    // inputText lives in the same composable scope), which is what caused
                    // the chat screen to visibly flicker/stutter while typing. This must live
                    // in a real @Composable scope (not inside the LazyColumn DSL body, where
                    // `remember` is illegal because that lambda is a LazyListScope builder,
                    // not a @Composable function).
                    val tagRoutineRegex = remember { Regex("""\[PROPOSE_AGENT_ROUTINE(_DONE|_CANCEL)?:\s*([^|]+)\|\s*([^|]+)\|\s*([^|]+)\|\s*([^|]+)\|\s*([^\]]+)\]""") }
                    val tagStopRoutineRegex = remember { Regex("""\[STOP_AGENT_ROUTINE(_DONE|_CANCEL)?:\s*([^\]]+)\]""") }
                    val tagGoalRegex = remember { Regex("""\[TARGET_NABUNG(_DONE|_CANCEL)?:\s*([^|]+)\|\s*([^|]+)\|\s*([^|]+)\|\s*([^\]]+)\]""") }
                    val tagDeleteGoalRegex = remember { Regex("""\[HAPUS_NABUNG(_DONE|_CANCEL)?:\s*([^\]]+)\]""") }
                    val tagUpdateGoalRegex = remember { Regex("""\[UPDATE_GOAL(_DONE|_CANCEL)?:\s*([^|]+)\|\s*([^|]+)\|\s*([^\]]+)\]""") }
                    val tagFixTxRegex = remember { Regex("""\[UBAH_TRANSAKSI_NABUNG(_DONE|_CANCEL)?:\s*([^|]+)\|\s*([^|]+)(?:\|\s*([^\]]+))?\]""") }
                    val tagDepositRegex = remember { Regex("""\[SETOR_(?:TABUNGAN|NABUNG)(_DONE|_CANCEL)?:\s*([^|]+)\|\s*([^|]+)(?:\|\s*([^\]]+))?\]""") }
                    val tagWithdrawRegex = remember { Regex("""\[TARIK_NABUNG(_DONE|_CANCEL)?:\s*([^|]+)\|\s*([^|]+)(?:\|\s*([^\]]+))?\]""") }
                    val tagTransactionRegex = remember { Regex("""\[CATAT_TRANSAKSI(_DONE|_CANCEL)?:\s*([^|]+)\|\s*([^|]+)\|\s*([^|]+)\|\s*([^\]]+)\]""") }
                    val tagAttachmentRegex = remember { Regex("""\[ATTACHMENT_URI:\s*([^\]]+)\]""") }

                    if (aiMessages.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(68.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(36.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Halo! Saya TANA AI Assistant",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Tanyakan seputar keuangan, analisis struk, atau minta AI mengoperasikan tabungan & rutin otomatis untuk Anda.",
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            // Quick Prompt Suggestions
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                quickPrompts.take(4).forEach { prompt ->
                                    Surface(
                                        onClick = { onSendMessage(prompt, null, null, "") },
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.AutoAwesome,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = prompt,
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Icon(
                                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(aiMessages, key = { it.id }) { msg ->
                            val rawContent = msg.content
                            val isUser = msg.role == "user"

                            val matchRoutine = tagRoutineRegex.find(rawContent)
                            val matchStopRoutine = tagStopRoutineRegex.find(rawContent)
                            val matchGoal = tagGoalRegex.find(rawContent)
                            val matchDeleteGoal = tagDeleteGoalRegex.find(rawContent)
                            val matchUpdateGoal = tagUpdateGoalRegex.find(rawContent)
                            val matchFixTx = tagFixTxRegex.find(rawContent)
                            val matchDeposit = tagDepositRegex.find(rawContent)
                            val matchWithdraw = tagWithdrawRegex.find(rawContent)
                            val matchTrans = tagTransactionRegex.find(rawContent)
                            val matchAttachment = tagAttachmentRegex.find(rawContent)

                            var cleanContent = rawContent
                            if (matchRoutine != null) cleanContent = cleanContent.replace(matchRoutine.value, "")
                            if (matchStopRoutine != null) cleanContent = cleanContent.replace(matchStopRoutine.value, "")
                            if (matchGoal != null) cleanContent = cleanContent.replace(matchGoal.value, "")
                            if (matchDeleteGoal != null) cleanContent = cleanContent.replace(matchDeleteGoal.value, "")
                            if (matchUpdateGoal != null) cleanContent = cleanContent.replace(matchUpdateGoal.value, "")
                            if (matchFixTx != null) cleanContent = cleanContent.replace(matchFixTx.value, "")
                            if (matchDeposit != null) cleanContent = cleanContent.replace(matchDeposit.value, "")
                            if (matchWithdraw != null) cleanContent = cleanContent.replace(matchWithdraw.value, "")
                            if (matchTrans != null) cleanContent = cleanContent.replace(matchTrans.value, "")

                            var imageUriToShow = ""
                            if (matchAttachment != null) {
                                imageUriToShow = matchAttachment.groupValues[1].trim()
                                cleanContent = cleanContent.replace(matchAttachment.value, "")
                            }
                            cleanContent = cleanContent.trim()

                            Row(
                                modifier = Modifier.fillMaxWidth().animateItem(),
                                horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
                                verticalAlignment = Alignment.Top
                            ) {
                                if (!isUser) {
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 4.dp, end = 8.dp)
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.AutoAwesome,
                                            contentDescription = "AI",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Column(
                                    modifier = Modifier.fillMaxWidth(if (isUser) 0.85f else 0.88f),
                                    horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                                ) {
                                    if (!isUser) {
                                        val isCurrentStreaming = isAiLoading && msg.id == aiMessages.lastOrNull()?.id
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(bottom = 3.dp, start = 2.dp)
                                        ) {
                                            Text(
                                                text = "TANA AI",
                                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Text(
                                                text = " • $openRouterModel",
                                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                                maxLines = 1
                                            )
                                            if (isCurrentStreaming) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp)
                                                        .clip(CircleShape)
                                                        .background(BrandPrimaryBright)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Mengetik...",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 9.5.sp,
                                                        fontWeight = FontWeight.Bold
                                                    ),
                                                    color = BrandPrimaryBright
                                                )
                                            }
                                        }
                                    }

                                    val hasSpecialCards = matchRoutine != null || matchGoal != null || matchDeleteGoal != null ||
                                            matchUpdateGoal != null || matchFixTx != null || matchDeposit != null ||
                                            matchWithdraw != null || matchTrans != null || imageUriToShow.isNotEmpty()

                                    Surface(
                                        color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(
                                            topStart = if (isUser) 18.dp else 4.dp,
                                            topEnd = if (isUser) 4.dp else 18.dp,
                                            bottomStart = 18.dp,
                                            bottomEnd = 18.dp
                                        ),
                                        border = if (!isUser) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null,
                                        modifier = if (hasSpecialCards) Modifier.fillMaxWidth() else Modifier.wrapContentSize()
                                    ) {
                                        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                            if (imageUriToShow.isNotEmpty()) {
                                                 androidx.compose.material3.Surface(
                                                     modifier = Modifier
                                                         .fillMaxWidth()
                                                         .height(180.dp)
                                                         .padding(bottom = 8.dp)
                                                         .clip(RoundedCornerShape(12.dp)),
                                                     color = MaterialTheme.colorScheme.surfaceVariant
                                                 ) {
                                                     coil.compose.AsyncImage(
                                                         model = imageUriToShow,
                                                         contentDescription = "Lampiran",
                                                         contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                                         modifier = Modifier.fillMaxSize()
                                                     )
                                                 }
                                            }
                                            if (cleanContent.isNotBlank()) {
                                                RichMarkdownText(
                                                    text = cleanContent,
                                                    isUser = isUser,
                                                    modifier = Modifier.padding(bottom = 4.dp)
                                                )
                                            }

                                        // Render Action Cards
                                        // 1. Propose Autonomous Agent Routine
                                        if (!isUser && matchRoutine != null && onCreateAgentRoutine != null) {
                                            val routineStatus = matchRoutine.groupValues[1]
                                            val routineTypeStr = matchRoutine.groupValues[2].trim().uppercase()
                                            val routineTarget = matchRoutine.groupValues[3].trim()
                                            val routineAmt = matchRoutine.groupValues[4].replace(Regex("[^0-9]"), "").toDoubleOrNull() ?: 0.0
                                            val routineDays = matchRoutine.groupValues[5].trim()
                                            val routineDesc = matchRoutine.groupValues[6].trim()

                                            val typeEnum = when {
                                                routineTypeStr.contains("DEPOSIT") || routineTypeStr.contains("NABUNG") || routineTypeStr.contains("SETOR") -> AgentRoutineType.AUTO_DEPOSIT
                                                routineTypeStr.contains("MOTIVATION") || routineTypeStr.contains("MOTIVASI") -> AgentRoutineType.DAILY_MOTIVATION
                                                routineTypeStr.contains("AUDIT") -> AgentRoutineType.FINANCIAL_AUDIT
                                                else -> AgentRoutineType.CUSTOM_REMINDER
                                            }

                                            val isSaved = savedRoutineMessageIds[msg.id] == true || routineStatus == "_DONE"
                                            val isCancelled = cancelledRoutineMessageIds[msg.id] == true || routineStatus == "_CANCEL"

                                            AutonomousRoutineCard(
                                                type = typeEnum,
                                                targetName = routineTarget,
                                                amount = routineAmt,
                                                activeDaysCsv = routineDays,
                                                description = routineDesc,
                                                isApproved = isSaved,
                                                isCancelled = isCancelled,
                                                onApprove = {
                                                    val title = if (typeEnum == AgentRoutineType.AUTO_DEPOSIT) "Setoran Otomatis $routineTarget" else "Motivasi Mandiri"
                                                    onCreateAgentRoutine(typeEnum, title, routineTarget, routineAmt, routineDays, routineDesc)
                                                    savedRoutineMessageIds[msg.id] = true
                                                    onUpdateAiMessageContent?.invoke(msg.id, msg.content.replace("[PROPOSE_AGENT_ROUTINE:", "[PROPOSE_AGENT_ROUTINE_DONE:"))
                                                },
                                                onCancel = {
                                                    cancelledRoutineMessageIds[msg.id] = true
                                                    onUpdateAiMessageContent?.invoke(msg.id, msg.content.replace("[PROPOSE_AGENT_ROUTINE:", "[PROPOSE_AGENT_ROUTINE_CANCEL:"))
                                                }
                                            )
                                        }

                                        // 2. Stop Autonomous Agent Routine
                                        if (!isUser && matchStopRoutine != null && onStopAgentRoutine != null) {
                                            val stopStatus = matchStopRoutine.groupValues[1]
                                            val stopTarget = matchStopRoutine.groupValues[2].trim()

                                            val isSaved = savedStopRoutineMessageIds[msg.id] == true || stopStatus == "_DONE"
                                            val isCancelled = cancelledStopRoutineMessageIds[msg.id] == true || stopStatus == "_CANCEL"

                                            StopRoutineCard(
                                                targetQuery = stopTarget,
                                                isStopped = isSaved,
                                                isCancelled = isCancelled,
                                                onConfirmStop = {
                                                    onStopAgentRoutine(stopTarget)
                                                    savedStopRoutineMessageIds[msg.id] = true
                                                    onUpdateAiMessageContent?.invoke(msg.id, msg.content.replace("[STOP_AGENT_ROUTINE:", "[STOP_AGENT_ROUTINE_DONE:"))
                                                },
                                                onCancel = {
                                                    cancelledStopRoutineMessageIds[msg.id] = true
                                                    onUpdateAiMessageContent?.invoke(msg.id, msg.content.replace("[STOP_AGENT_ROUTINE:", "[STOP_AGENT_ROUTINE_CANCEL:"))
                                                }
                                            )
                                        }

                                        // 3. Deposit Tabungan
                                        if (!isUser && matchDeposit != null && onDepositFromAi != null) {
                                            val depositStatus = matchDeposit.groupValues[1]
                                            val depositGoalName = matchDeposit.groupValues[2].trim()
                                            val depositAmt = matchDeposit.groupValues[3].replace(Regex("[^0-9]"), "").toDoubleOrNull() ?: 50000.0
                                            val depositNote = matchDeposit.groupValues.getOrNull(4)?.trim() ?: "Setoran via AI"

                                            val isDepositSaved = savedDepositMessageIds[msg.id] == true || depositStatus == "_DONE"
                                            val isDepositCancelled = cancelledDepositMessageIds[msg.id] == true || depositStatus == "_CANCEL"

                                            ActionPlanCard(
                                                actionName = "SETORAN_TABUNGAN",
                                                actionPayload = """{"target": "$depositGoalName", "nominal": $depositAmt, "catatan": "$depositNote"}""",
                                                isSaved = isDepositSaved,
                                                isCancelled = isDepositCancelled,
                                                onExecute = {
                                                    onDepositFromAi(depositGoalName, depositAmt, depositNote)
                                                    savedDepositMessageIds[msg.id] = true
                                                    val cleanTag = if (msg.content.contains("[SETOR_TABUNGAN:")) "[SETOR_TABUNGAN:" else "[SETOR_NABUNG:"
                                                    val doneTag = if (msg.content.contains("[SETOR_TABUNGAN:")) "[SETOR_TABUNGAN_DONE:" else "[SETOR_NABUNG_DONE:"
                                                    onUpdateAiMessageContent?.invoke(msg.id, msg.content.replace(cleanTag, doneTag))
                                                },
                                                onCancel = {
                                                    cancelledDepositMessageIds[msg.id] = true
                                                    val cleanTag = if (msg.content.contains("[SETOR_TABUNGAN:")) "[SETOR_TABUNGAN:" else "[SETOR_NABUNG:"
                                                    val cancelTag = if (msg.content.contains("[SETOR_TABUNGAN:")) "[SETOR_TABUNGAN_CANCEL:" else "[SETOR_NABUNG_CANCEL:"
                                                    onUpdateAiMessageContent?.invoke(msg.id, msg.content.replace(cleanTag, cancelTag))
                                                }
                                            )
                                        }

                                        // 4. Withdraw Tabungan
                                        if (!isUser && matchWithdraw != null && onWithdrawFromAi != null) {
                                            val withdrawStatus = matchWithdraw.groupValues[1]
                                            val withdrawGoalName = matchWithdraw.groupValues[2].trim()
                                            val withdrawAmt = matchWithdraw.groupValues[3].replace(Regex("[^0-9]"), "").toDoubleOrNull() ?: 50000.0
                                            val withdrawNote = matchWithdraw.groupValues.getOrNull(4)?.trim() ?: "Penarikan via AI"

                                            val isWithdrawSaved = savedWithdrawMessageIds[msg.id] == true || withdrawStatus == "_DONE"
                                            val isWithdrawCancelled = cancelledWithdrawMessageIds[msg.id] == true || withdrawStatus == "_CANCEL"

                                            ActionPlanCard(
                                                actionName = "TARIK_TABUNGAN",
                                                actionPayload = """{"target": "$withdrawGoalName", "nominal": $withdrawAmt, "catatan": "$withdrawNote"}""",
                                                isSaved = isWithdrawSaved,
                                                isCancelled = isWithdrawCancelled,
                                                onExecute = {
                                                    onWithdrawFromAi(withdrawGoalName, withdrawAmt, withdrawNote)
                                                    savedWithdrawMessageIds[msg.id] = true
                                                    onUpdateAiMessageContent?.invoke(msg.id, msg.content.replace("[TARIK_NABUNG:", "[TARIK_NABUNG_DONE:"))
                                                },
                                                onCancel = {
                                                    cancelledWithdrawMessageIds[msg.id] = true
                                                    onUpdateAiMessageContent?.invoke(msg.id, msg.content.replace("[TARIK_NABUNG:", "[TARIK_NABUNG_CANCEL:"))
                                                }
                                            )
                                        }

                                        // 5. Catat Transaksi
                                        if (!isUser && matchTrans != null && onAddTransactionFromAi != null) {
                                            val transStatus = matchTrans.groupValues[1]
                                            val transTypeStr = matchTrans.groupValues[2].trim().uppercase()
                                            val transAmt = matchTrans.groupValues[3].replace(Regex("[^0-9]"), "").toDoubleOrNull() ?: 25000.0
                                            val transCategory = matchTrans.groupValues[4].trim()
                                            val transNote = matchTrans.groupValues[5].trim()

                                            val transTypeEnum = when {
                                                transTypeStr.contains("INCOME") || transTypeStr.contains("PEMASUKAN") -> com.tana.data.model.TransactionType.INCOME
                                                transTypeStr.contains("SAVINGS") || transTypeStr.contains("TABUNGAN") -> com.tana.data.model.TransactionType.SAVINGS
                                                else -> com.tana.data.model.TransactionType.EXPENSE
                                            }

                                            val isTransSaved = savedTransactionMessageIds[msg.id] == true || transStatus == "_DONE"
                                            val isTransCancelled = cancelledTransactionMessageIds[msg.id] == true || transStatus == "_CANCEL"

                                            ActionPlanCard(
                                                actionName = "CATAT_TRANSAKSI",
                                                actionPayload = """{"tipe": "$transTypeStr", "nominal": $transAmt, "kategori": "$transCategory"}""",
                                                isSaved = isTransSaved,
                                                isCancelled = isTransCancelled,
                                                onExecute = {
                                                    onAddTransactionFromAi(transTypeEnum, transAmt, transCategory, transNote)
                                                    savedTransactionMessageIds[msg.id] = true
                                                    onUpdateAiMessageContent?.invoke(msg.id, msg.content.replace("[CATAT_TRANSAKSI:", "[CATAT_TRANSAKSI_DONE:"))
                                                },
                                                onCancel = {
                                                    cancelledTransactionMessageIds[msg.id] = true
                                                    onUpdateAiMessageContent?.invoke(msg.id, msg.content.replace("[CATAT_TRANSAKSI:", "[CATAT_TRANSAKSI_CANCEL:"))
                                                }
                                            )
                                        }

                                        // 6. Target Tabungan
                                        if (!isUser && matchGoal != null && onAddGoalFromAi != null) {
                                            val goalStatus = matchGoal.groupValues[1]
                                            val goalTitle = matchGoal.groupValues[2].trim()
                                            val goalTarget = matchGoal.groupValues[3].replace(Regex("[^0-9]"), "").toDoubleOrNull() ?: 1000000.0
                                            val goalPlanAmt = matchGoal.groupValues[4].replace(Regex("[^0-9]"), "").toDoubleOrNull() ?: 20000.0
                                            val goalFreq = matchGoal.groupValues[5].trim().uppercase()

                                            val isAlreadySaved = savedGoalMessageIds[msg.id] == true || goalStatus == "_DONE"
                                            val isGoalCancelled = cancelledGoalMessageIds[msg.id] == true || goalStatus == "_CANCEL"

                                            ActionPlanCard(
                                                actionName = "TARGET_NABUNG",
                                                actionPayload = """{"nama": "$goalTitle", "target": $goalTarget, "setoran": $goalPlanAmt, "freq": "$goalFreq"}""",
                                                isSaved = isAlreadySaved,
                                                isCancelled = isGoalCancelled,
                                                onExecute = {
                                                    onAddGoalFromAi(
                                                        goalTitle,
                                                        goalTarget,
                                                        0.0,
                                                        goalPlanAmt,
                                                        goalFreq,
                                                        setOf(2, 3, 4, 5, 6, 7, 1),
                                                        "20:00",
                                                        "Dibuat otomatis dari saran AI TANA"
                                                    )
                                                    savedGoalMessageIds[msg.id] = true
                                                    onUpdateAiMessageContent?.invoke(msg.id, msg.content.replace("[TARGET_NABUNG:", "[TARGET_NABUNG_DONE:"))
                                                },
                                                onCancel = {
                                                    cancelledGoalMessageIds[msg.id] = true
                                                    onUpdateAiMessageContent?.invoke(msg.id, msg.content.replace("[TARGET_NABUNG:", "[TARGET_NABUNG_CANCEL:"))
                                                }
                                            )
                                        }

                                        // 7. Hapus Target Tabungan
                                        if (!isUser && matchDeleteGoal != null && onDeleteGoalFromAi != null) {
                                            val delStatus = matchDeleteGoal.groupValues[1]
                                            val delGoalName = matchDeleteGoal.groupValues[2].trim()

                                            val isDelSaved = savedDeleteGoalMessageIds[msg.id] == true || delStatus == "_DONE"
                                            val isDelCancelled = cancelledDeleteGoalMessageIds[msg.id] == true || delStatus == "_CANCEL"

                                            ActionPlanCard(
                                                actionName = "HAPUS_TARGET_TABUNGAN",
                                                actionPayload = """{"target": "$delGoalName"}""",
                                                isSaved = isDelSaved,
                                                isCancelled = isDelCancelled,
                                                onExecute = {
                                                    onDeleteGoalFromAi(delGoalName)
                                                    savedDeleteGoalMessageIds[msg.id] = true
                                                    onUpdateAiMessageContent?.invoke(msg.id, msg.content.replace("[HAPUS_NABUNG:", "[HAPUS_NABUNG_DONE:"))
                                                },
                                                onCancel = {
                                                    cancelledDeleteGoalMessageIds[msg.id] = true
                                                    onUpdateAiMessageContent?.invoke(msg.id, msg.content.replace("[HAPUS_NABUNG:", "[HAPUS_NABUNG_CANCEL:"))
                                                }
                                            )
                                        }

                                        // 8. Update Target Tabungan
                                        if (!isUser && matchUpdateGoal != null && onUpdateGoalFromAi != null) {
                                            val upStatus = matchUpdateGoal.groupValues[1]
                                            val upGoalName = matchUpdateGoal.groupValues[2].trim()
                                            val upTarget = matchUpdateGoal.groupValues[3].replace(Regex("[^0-9]"), "").toDoubleOrNull() ?: 5000000.0
                                            val upPlan = matchUpdateGoal.groupValues[4].replace(Regex("[^0-9]"), "").toDoubleOrNull() ?: 50000.0

                                            val isUpSaved = savedUpdateGoalMessageIds[msg.id] == true || upStatus == "_DONE"
                                            val isUpCancelled = cancelledUpdateGoalMessageIds[msg.id] == true || upStatus == "_CANCEL"

                                            ActionPlanCard(
                                                actionName = "UPDATE_TARGET_TABUNGAN",
                                                actionPayload = """{"target": "$upGoalName", "targetBaru": $upTarget, "setoranBaru": $upPlan}""",
                                                isSaved = isUpSaved,
                                                isCancelled = isUpCancelled,
                                                onExecute = {
                                                    onUpdateGoalFromAi(upGoalName, upTarget, upPlan)
                                                    savedUpdateGoalMessageIds[msg.id] = true
                                                    onUpdateAiMessageContent?.invoke(msg.id, msg.content.replace("[UPDATE_GOAL:", "[UPDATE_GOAL_DONE:"))
                                                },
                                                onCancel = {
                                                    cancelledUpdateGoalMessageIds[msg.id] = true
                                                    onUpdateAiMessageContent?.invoke(msg.id, msg.content.replace("[UPDATE_GOAL:", "[UPDATE_GOAL_CANCEL:"))
                                                }
                                            )
                                        }

                                        // 9. Ubah / Fix Transaksi Tabungan
                                        if (!isUser && matchFixTx != null && onFixSavingsTransactionFromAi != null) {
                                            val fixStatus = matchFixTx.groupValues[1]
                                            val fixGoalName = matchFixTx.groupValues[2].trim()
                                            val fixAmt = matchFixTx.groupValues[3].replace(Regex("[^0-9]"), "").toDoubleOrNull() ?: 50000.0
                                            val fixNote = matchFixTx.groupValues.getOrNull(4)?.trim() ?: "Perbaikan transaksi via AI"

                                            val isFixSaved = savedFixTxMessageIds[msg.id] == true || fixStatus == "_DONE"
                                            val isFixCancelled = cancelledFixTxMessageIds[msg.id] == true || fixStatus == "_CANCEL"

                                            ActionPlanCard(
                                                actionName = "PERBAIKI_TRANSAKSI_NABUNG",
                                                actionPayload = """{"target": "$fixGoalName", "nominalBaru": $fixAmt, "catatanBaru": "$fixNote"}""",
                                                isSaved = isFixSaved,
                                                isCancelled = isFixCancelled,
                                                onExecute = {
                                                    onFixSavingsTransactionFromAi(fixGoalName, fixAmt, fixNote)
                                                    savedFixTxMessageIds[msg.id] = true
                                                    onUpdateAiMessageContent?.invoke(msg.id, msg.content.replace("[UBAH_TRANSAKSI_NABUNG:", "[UBAH_TRANSAKSI_NABUNG_DONE:"))
                                                },
                                                onCancel = {
                                                    cancelledFixTxMessageIds[msg.id] = true
                                                    onUpdateAiMessageContent?.invoke(msg.id, msg.content.replace("[UBAH_TRANSAKSI_NABUNG:", "[UBAH_TRANSAKSI_NABUNG_CANCEL:"))
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                            val isWaitingForFirstToken = isAiLoading && (aiMessages.isEmpty() || aiMessages.lastOrNull()?.role != "assistant" || aiMessages.lastOrNull()?.content.isNullOrBlank())
                            if (isWaitingForFirstToken) {
                                item {
                                    val infiniteTransition = rememberInfiniteTransition(label = "dots_bounce")
                                    val bubbleBobbing by infiniteTransition.animateFloat(
                                        initialValue = 0f,
                                        targetValue = -6f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(650, easing = FastOutSlowInEasing),
                                            repeatMode = RepeatMode.Reverse
                                        ),
                                        label = "bubble_bobbing"
                                    )
                                    val bounce1 by infiniteTransition.animateFloat(
                                        initialValue = 0f,
                                        targetValue = -5f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(380, easing = FastOutSlowInEasing),
                                            repeatMode = RepeatMode.Reverse
                                        ),
                                        label = "bounce1"
                                    )
                                    val bounce2 by infiniteTransition.animateFloat(
                                        initialValue = 0f,
                                        targetValue = -5f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(380, delayMillis = 130, easing = FastOutSlowInEasing),
                                            repeatMode = RepeatMode.Reverse
                                        ),
                                        label = "bounce2"
                                    )
                                    val bounce3 by infiniteTransition.animateFloat(
                                        initialValue = 0f,
                                        targetValue = -5f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(380, delayMillis = 260, easing = FastOutSlowInEasing),
                                            repeatMode = RepeatMode.Reverse
                                        ),
                                        label = "bounce3"
                                    )

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .offset(y = bubbleBobbing.dp)
                                            .animateItem(),
                                        horizontalArrangement = Arrangement.Start,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    text = "Sedang berpikir",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                )
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(modifier = Modifier.offset(y = bounce1.dp).size(5.dp).clip(CircleShape).background(BrandPrimaryBright))
                                                    Box(modifier = Modifier.offset(y = bounce2.dp).size(5.dp).clip(CircleShape).background(AccentSavings))
                                                    Box(modifier = Modifier.offset(y = bounce3.dp).size(5.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                                                }

                                                if (onStopAiGeneration != null) {
                                                    Surface(
                                                        onClick = { onStopAiGeneration.invoke() },
                                                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f),
                                                        shape = CircleShape,
                                                        modifier = Modifier.testTag("stop_ai_loading_chip")
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Filled.Stop,
                                                            contentDescription = "Hentikan AI",
                                                            tint = MaterialTheme.colorScheme.error,
                                                            modifier = Modifier.padding(3.dp).size(11.dp)
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
            }

                // Attachment Preview Bar
                androidx.compose.animation.AnimatedVisibility(visible = attachedMediaName != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                if (attachedImageUri != null) {
                                    coil.compose.AsyncImage(
                                        model = attachedImageUri,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
                                            .padding(2.dp),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                } else if (attachedImageBitmap != null) {
                                    attachedImageBitmap?.let { bmp ->
                                        androidx.compose.foundation.Image(
                                            bitmap = bmp.asImageBitmap(),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
                                                .padding(2.dp),
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                        )
                                    }
                                } else {
                                    val mediaIcon = when (attachedMediaType) {
                                        "video" -> Icons.Filled.Videocam
                                        "document" -> Icons.Filled.Description
                                        else -> Icons.Filled.Image
                                    }
                                    Icon(
                                        imageVector = mediaIcon,
                                        contentDescription = null,
                                        tint = AccentSavings,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = attachedMediaName ?: "Media Terlampir",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Siap dianalisa & diekstrak ke target tabungan",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            IconButton(onClick = { 
                                attachedMediaName = null
                                attachedImageBitmap = null
                                attachedImageUri = null
                                attachedMediaType = null 
                            }, modifier = Modifier.size(24.dp)) {
                                Icon(imageVector = Icons.Filled.Close, contentDescription = "Batal", modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                // Voice Recording Visualization
                androidx.compose.animation.AnimatedVisibility(visible = isRecordingVoice) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CustomAudioWaveformVisualizer(
                            soundLevel = currentSoundLevel,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = speechStatusText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Input Area
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(24.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(24.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Attachment Dropdown Trigger Button (+)
                    Box {
                        IconButton(
                            onClick = { showAttachmentSheet = true },
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    if (showAttachmentSheet) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    CircleShape
                                )
                                .testTag("attach_media_button")
                        ) {
                            Icon(
                                imageVector = if (showAttachmentSheet) Icons.Filled.Close else Icons.Filled.Add,
                                contentDescription = "Lampirkan Berkas",
                                tint = if (showAttachmentSheet) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        androidx.compose.material3.DropdownMenu(
                            expanded = showAttachmentSheet,
                            onDismissRequest = { showAttachmentSheet = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("Kamera", style = MaterialTheme.typography.labelMedium) },
                                leadingIcon = {
                                    Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                                },
                                onClick = {
                                    showAttachmentSheet = false
                                    cameraLauncher.launch(null)
                                }
                            )
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("Galeri", style = MaterialTheme.typography.labelMedium) },
                                leadingIcon = {
                                    Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                                },
                                onClick = {
                                    showAttachmentSheet = false
                                    imageGalleryLauncher.launch("image/*")
                                }
                            )
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("Dokumen", style = MaterialTheme.typography.labelMedium) },
                                leadingIcon = {
                                    Icon(Icons.Filled.Description, contentDescription = null, modifier = Modifier.size(18.dp))
                                },
                                onClick = {
                                    showAttachmentSheet = false
                                    documentPickerLauncher.launch("*/*")
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Box(modifier = Modifier.weight(1f)) {
                        if (inputText.isEmpty() && !isRecordingVoice) {
                            Text(
                                text = when {
                                    isAiLoading -> "AI sedang merespon... (Ketuk stop untuk batal)"
                                    attachedMediaName != null -> "Ketik instruksi media..."
                                    else -> "Pesan TANA AI..."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        BasicTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth().testTag("ai_input_field"),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if ((inputText.isNotBlank() || attachedMediaName != null) && !isAiLoading) {
                                        val query = inputText.trim()
                                        val mediaName = attachedMediaName
                                        val mediaType = attachedMediaType
                                        val imageUri = attachedImageUri?.toString() ?: attachedImageBitmap?.let { bmp ->
                                            try {
                                                val cacheFile = java.io.File(context.cacheDir, "camera_photo_${System.currentTimeMillis()}.jpg")
                                                java.io.FileOutputStream(cacheFile).use { out ->
                                                    bmp.compress(Bitmap.CompressFormat.JPEG, 90, out)
                                                }
                                                Uri.fromFile(cacheFile).toString()
                                            } catch (e: Exception) { "" }
                                        } ?: ""
                                        inputText = ""
                                        attachedMediaName = null
                                        attachedMediaType = null
                                        attachedImageBitmap = null
                                        attachedImageUri = null
                                        focusManager.clearFocus()
                                        onSendMessage(query, mediaName, mediaType, imageUri)
                                    }
                                }
                            )
                        )
                    }

                    val canSend = (inputText.isNotBlank() || attachedMediaName != null) && !isAiLoading

                    // Action Button: Stop (if generating) | Mic (if empty) | Send (if has input)
                    if (isAiLoading) {
                        IconButton(
                            onClick = { onStopAiGeneration?.invoke() },
                            modifier = Modifier
                                .size(36.dp)
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.15f), CircleShape)
                                .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f), CircleShape)
                                .testTag("stop_ai_generation_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Stop,
                                contentDescription = "Hentikan Generasi AI",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else if (inputText.isBlank() && attachedMediaName == null) {
                        IconButton(
                            onClick = {
                                if (hasAudioPermission) {
                                    if (isRecordingVoice) {
                                        speechRecognizer?.stopListening()
                                        isRecordingVoice = false
                                    } else {
                                        val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                            putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, "id-ID")
                                        }
                                        speechRecognizer?.startListening(intent)
                                        isRecordingVoice = true
                                        speechStatusText = "Mendengarkan..."
                                    }
                                } else {
                                    audioPermissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isRecordingVoice) Icons.Filled.Stop else Icons.Filled.Mic,
                                contentDescription = "Voice",
                                tint = if (isRecordingVoice) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        IconButton(
                            onClick = {
                                if (canSend) {
                                    val query = inputText.trim()
                                    val mediaName = attachedMediaName
                                    val mediaType = attachedMediaType
                                    val imageUri = attachedImageUri?.toString() ?: attachedImageBitmap?.let { bmp ->
                                        try {
                                            val cacheFile = java.io.File(context.cacheDir, "camera_photo_${System.currentTimeMillis()}.jpg")
                                            java.io.FileOutputStream(cacheFile).use { out ->
                                                bmp.compress(Bitmap.CompressFormat.JPEG, 90, out)
                                            }
                                            Uri.fromFile(cacheFile).toString()
                                        } catch (e: Exception) { "" }
                                    } ?: ""
                                    inputText = ""
                                    attachedMediaName = null
                                    attachedMediaType = null
                                    attachedImageBitmap = null
                                    attachedImageUri = null
                                    focusManager.clearFocus()
                                    onSendMessage(query, mediaName, mediaType, imageUri)
                                }
                            },
                            enabled = canSend,
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    if (canSend) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                    CircleShape
                                )
                                .testTag("send_ai_message_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Kirim",
                                tint = if (canSend) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            if (showAgentRoutinesSheet) {
                AgentRoutinesBottomSheet(
                    agentRoutines = agentRoutines,
                    onDismiss = { showAgentRoutinesSheet = false },
                    onToggleRoutine = { id, enabled -> onToggleAgentRoutine?.invoke(id, enabled) },
                    onDeleteRoutine = { id -> onDeleteAgentRoutine?.invoke(id) },
                    onExecuteNow = { onExecuteRoutinesNow?.invoke() }
                )
            }
        }
    }

private fun formatRupiah(amount: Double): String {
    return NumberFormat.getCurrencyInstance(Locale("id", "ID")).format(amount)
}

@Composable
private fun CustomAudioWaveformVisualizer(
    soundLevel: Float = 0f,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform")
    val h1 by infiniteTransition.animateFloat(
        initialValue = 6f,
        targetValue = 16f,
        animationSpec = infiniteRepeatable(tween(400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "h1"
    )
    val h2 by infiniteTransition.animateFloat(
        initialValue = 14f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(350, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "h2"
    )
    val h3 by infiniteTransition.animateFloat(
        initialValue = 8f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(tween(450, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "h3"
    )
    val h4 by infiniteTransition.animateFloat(
        initialValue = 18f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(300, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "h4"
    )
    val h5 by infiniteTransition.animateFloat(
        initialValue = 8f,
        targetValue = 16f,
        animationSpec = infiniteRepeatable(tween(500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "h5"
    )

    // Dynamic boost based on real mic volume input (0..1f)
    val micBoost = soundLevel * 14f

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        listOf(h1, h2, h3, h4, h5).forEach { baseHeight ->
            val finalHeight = (baseHeight + micBoost).coerceIn(4f, 32f)
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(finalHeight.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
        }
    }
}

@Composable
private fun AttachmentOptionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.surface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 11.5.sp),
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.5.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ActionPlanCard(
    actionName: String,
    actionPayload: String,
    isSaved: Boolean,
    isCancelled: Boolean,
    onExecute: () -> Unit,
    onCancel: () -> Unit
) {
    if (isCancelled && !isSaved) {
        // We will show it as cancelled
    }

    androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(6.dp))

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        androidx.compose.foundation.layout.Column(modifier = Modifier.padding(12.dp)) {
            // Header
            androidx.compose.foundation.layout.Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.FlashOn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(16.dp)
                )
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Rencana Aksi",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                    shape = androidx.compose.foundation.shape.CircleShape
                ) {
                    Text(
                        text = "1",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(12.dp))

            // Action Pill & Payload
            androidx.compose.foundation.layout.Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = actionName,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = actionPayload,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }

            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(12.dp))

            if (isSaved) {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = AccentIncome,
                        modifier = Modifier.size(16.dp)
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Sudah terkirim",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = AccentIncome
                    )
                }
            } else if (isCancelled) {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Cancel,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Dibatalkan",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                androidx.compose.foundation.layout.Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onExecute,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.onSurface,
                            contentColor = MaterialTheme.colorScheme.surface
                        ),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Jalankan",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    OutlinedButton(
                        onClick = onCancel,
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text(
                            text = "Batal",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AutonomousRoutineCard(
    type: AgentRoutineType,
    targetName: String,
    amount: Double,
    activeDaysCsv: String,
    description: String,
    isApproved: Boolean,
    isCancelled: Boolean,
    onApprove: () -> Unit,
    onCancel: () -> Unit
) {
    val daysLabel = when {
        activeDaysCsv.startsWith("SEC_") -> "Setiap ${activeDaysCsv.replace("SEC_", "")} Detik Realtime"
        activeDaysCsv.startsWith("MIN_") -> "Setiap ${activeDaysCsv.replace("MIN_", "")} Menit Realtime"
        activeDaysCsv.startsWith("HOUR_") -> "Setiap ${activeDaysCsv.replace("HOUR_", "")} Jam"
        activeDaysCsv == "2,3,4,5,6,7" -> "Setiap Hari (Kecuali Minggu)"
        activeDaysCsv == "2,3,4,5,6" -> "Senin - Jumat (Hari Kerja)"
        activeDaysCsv == "1,7" -> "Sabtu & Minggu"
        else -> "Setiap Hari"
    }

    val freqUnit = when {
        activeDaysCsv.startsWith("SEC_") -> "/ detik"
        activeDaysCsv.startsWith("MIN_") -> "/ menit"
        activeDaysCsv.startsWith("HOUR_") -> "/ jam"
        else -> "/ hari"
    }

    Spacer(modifier = Modifier.height(6.dp))

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Badge
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.SmartToy,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "PERMINTAAN IZIN AI AGENT",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "MANDIRI",
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Details Box
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = if (type == AgentRoutineType.AUTO_DEPOSIT) "Setoran Rutin Otomatis" else "Motivasi Finansial Mandiri",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    if (type == AgentRoutineType.AUTO_DEPOSIT) {
                        Text(
                            text = "• Target: $targetName\n• Nominal: ${formatRupiah(amount)} $freqUnit\n• Jadwal: $daysLabel",
                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "• Tugas: Kirim motivasi & evaluasi disiplin finansial harian secara otomatis.",
                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = description,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                fontSize = 10.5.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Jika Anda menyetujui, AI Agent TANA akan menjalankan tugas ini secara mandiri setiap hari sesuai jadwal tanpa perlu Anda minta lagi. Anda dapat menghentikannya kapan saja.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp, lineHeight = 15.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (isApproved) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = AccentIncome,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Izin Diberikan & Agen Telah Diaktifkan",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = AccentIncome
                    )
                }
            } else if (isCancelled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Cancel,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Permintaan Dibatalkan",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onApprove,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Izinkan & Aktifkan",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    OutlinedButton(
                        onClick = onCancel,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Text(
                            text = "Tolak",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StopRoutineCard(
    targetQuery: String,
    isStopped: Boolean,
    isCancelled: Boolean,
    onConfirmStop: () -> Unit,
    onCancel: () -> Unit
) {
    val displayTarget = if (targetQuery.uppercase() == "ALL") "Semua Rutinitas Otomatis AI" else targetQuery

    Spacer(modifier = Modifier.height(6.dp))

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Filled.Stop,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "KONFIRMASI PENGHENTIAN AGEN",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    ),
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Apakah Anda yakin ingin menonaktifkan rutinitas otomatis untuk '$displayTarget'?",
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.5.sp),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (isStopped) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = AccentIncome,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Rutinitas Telah Dihentikan",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = AccentIncome
                    )
                }
            } else if (isCancelled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Filled.Cancel,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Penghentian Dibatalkan",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onConfirmStop,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Text(
                            text = "Konfirmasi Hentikan",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }

                    OutlinedButton(
                        onClick = onCancel,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Text(
                            text = "Batal",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentRoutinesBottomSheet(
    agentRoutines: List<AgentRoutineEntity>,
    onDismiss: () -> Unit,
    onToggleRoutine: (Long, Boolean) -> Unit,
    onDeleteRoutine: (Long) -> Unit,
    onExecuteNow: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.SmartToy,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Tugas Mandiri AI Agent",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Button(
                    onClick = onExecuteNow,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.onSurface,
                        contentColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.FlashOn,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Jalankan Hari Ini",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Daftar rutinitas otomatis yang dikerjakan AI Agent setiap hari secara mandiri di latar belakang.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            if (agentRoutines.isEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Belum Ada Rutinitas Otomatis",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Coba ketik 'Setorkan 10k setiap hari kecuali Minggu' atau 'Beri aku motivasi tiap hari' di chat AI.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(agentRoutines, key = { it.id }) { routine ->
                        val daysLabel = when {
                            routine.intervalSeconds > 0 -> {
                                if (routine.intervalSeconds < 60) "Setiap ${routine.intervalSeconds} Detik Realtime" else "Setiap ${routine.intervalSeconds / 60} Menit Realtime"
                            }
                            routine.activeDaysCsv.startsWith("SEC_") -> "Setiap ${routine.activeDaysCsv.replace("SEC_", "")} Detik Realtime"
                            routine.activeDaysCsv.startsWith("MIN_") -> "Setiap ${routine.activeDaysCsv.replace("MIN_", "")} Menit Realtime"
                            routine.activeDaysCsv.startsWith("HOUR_") -> "Setiap ${routine.activeDaysCsv.replace("HOUR_", "")} Jam"
                            routine.activeDaysCsv == "2,3,4,5,6,7" -> "Kecuali Minggu (Senin - Sabtu)"
                            routine.activeDaysCsv == "2,3,4,5,6" -> "Senin - Jumat"
                            routine.activeDaysCsv == "1,7" -> "Sabtu & Minggu"
                            else -> "Setiap Hari"
                        }

                        val freqUnit = when {
                            routine.intervalSeconds in 1..59 -> "/ ${routine.intervalSeconds} dtk"
                            routine.intervalSeconds >= 60 -> "/ ${routine.intervalSeconds / 60} mnt"
                            routine.activeDaysCsv.startsWith("SEC_") -> "/ dtk"
                            routine.activeDaysCsv.startsWith("MIN_") -> "/ mnt"
                            else -> "/ hari"
                        }

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(0.8.dp, MaterialTheme.colorScheme.outlineVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = routine.title,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (!routine.isApproved) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = "Menunggu Izin",
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                                                    color = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(3.dp))

                                    if (routine.type == AgentRoutineType.AUTO_DEPOSIT) {
                                        Text(
                                            text = "${formatRupiah(routine.amount)} $freqUnit • $daysLabel",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    } else {
                                        Text(
                                            text = "Motivasi harian • $daysLabel",
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Text(
                                        text = "Dieksekusi ${routine.executionCount}x • Terakhir: ${routine.lastExecutedDayKey.ifBlank { "—" }}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Switch(
                                        checked = routine.isEnabled,
                                        onCheckedChange = { checked -> onToggleRoutine(routine.id, checked) },
                                        colors = SwitchDefaults.colors(
                                            checkedThumbColor = MaterialTheme.colorScheme.surface,
                                            checkedTrackColor = MaterialTheme.colorScheme.primary
                                        )
                                    )

                                    IconButton(
                                        onClick = { onDeleteRoutine(routine.id) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Delete,
                                            contentDescription = "Hapus",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// --- RICH MARKDOWN FORMATTING ENGINE & CODE EMBEDS ---

sealed class MarkdownBlock {
    data class Header(val level: Int, val text: String) : MarkdownBlock()
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock()
    data class Quote(val text: String) : MarkdownBlock()
    data class BulletItem(val prefix: String, val text: String) : MarkdownBlock()
    data class Paragraph(val text: String) : MarkdownBlock()
}

fun parseMarkdownBlocks(rawText: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val lines = rawText.lines()
    var i = 0

    while (i < lines.size) {
        val line = lines[i]

        // Code block ```
        if (line.trimStart().startsWith("```")) {
            val lang = line.trimStart().removePrefix("```").trim()
            val codeLines = mutableListOf<String>()
            i++
            while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                codeLines.add(lines[i])
                i++
            }
            blocks.add(MarkdownBlock.CodeBlock(language = lang, code = codeLines.joinToString("\n")))
            i++
            continue
        }

        val trimmed = line.trim()
        if (trimmed.isEmpty()) {
            i++
            continue
        }

        // Headers #, ##, ###
        if (trimmed.startsWith("#")) {
            val level = trimmed.takeWhile { it == '#' }.length.coerceIn(1, 3)
            val text = trimmed.dropWhile { it == '#' || it == ' ' }
            blocks.add(MarkdownBlock.Header(level = level, text = text))
            i++
            continue
        }

        // Quotes >
        if (trimmed.startsWith(">")) {
            val text = trimmed.removePrefix(">").trim()
            blocks.add(MarkdownBlock.Quote(text = text))
            i++
            continue
        }

        // Bullet or numbered list item
        val bulletMatch = Regex("""^([\-\*•]|\d+\.)\s+(.+)""").find(trimmed)
        if (bulletMatch != null) {
            val prefix = bulletMatch.groupValues[1]
            val text = bulletMatch.groupValues[2]
            blocks.add(MarkdownBlock.BulletItem(prefix = if (prefix in listOf("-", "*", "•")) "" else prefix, text = text))
            i++
            continue
        }

        // Standard Paragraph
        blocks.add(MarkdownBlock.Paragraph(text = trimmed))
        i++
    }

    return if (blocks.isEmpty()) listOf(MarkdownBlock.Paragraph(text = rawText)) else blocks
}

fun buildInlineMarkdown(text: String, isUser: Boolean): AnnotatedString {
    return buildAnnotatedString {
        // Match **bold**, *italic*, `inline code`
        val pattern = Regex("""(\*\*.*?\*\*|\*.*?\*|`.*?`)""")
        var currentIndex = 0
        val matches = pattern.findAll(text)

        val codeBg = if (isUser) Color.Black.copy(alpha = 0.25f) else Color(0xFF1E293B)
        val codeFg = if (isUser) Color.White else Color(0xFFC6A15B)

        for (match in matches) {
            if (match.range.first > currentIndex) {
                append(text.substring(currentIndex, match.range.first))
            }

            val value = match.value
            when {
                value.startsWith("**") && value.endsWith("**") && value.length >= 4 -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(value.substring(2, value.length - 2))
                    }
                }
                value.startsWith("*") && value.endsWith("*") && value.length >= 2 -> {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(value.substring(1, value.length - 1))
                    }
                }
                value.startsWith("`") && value.endsWith("`") && value.length >= 2 -> {
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            background = codeBg,
                            color = codeFg
                        )
                    ) {
                        append(" ${value.substring(1, value.length - 1)} ")
                    }
                }
                else -> {
                    append(value)
                }
            }
            currentIndex = match.range.last + 1
        }

        if (currentIndex < text.length) {
            append(text.substring(currentIndex))
        }
    }
}

@Composable
fun RichMarkdownText(
    text: String,
    isUser: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val blocks = remember(text) { parseMarkdownBlocks(text) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Header -> {
                    Text(
                        text = buildInlineMarkdown(block.text, isUser),
                        style = when (block.level) {
                            1 -> MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            2 -> MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 15.5.sp)
                            else -> MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        },
                        color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    )
                }
                is MarkdownBlock.CodeBlock -> {
                    CodeBlockCard(
                        code = block.code,
                        language = block.language,
                        onCopy = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager
                            clipboard?.setPrimaryClip(android.content.ClipData.newPlainText("Kode AI", block.code))
                        }
                    )
                }
                is MarkdownBlock.Quote -> {
                    Surface(
                        color = if (isUser) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(3.dp)
                                    .height(20.dp)
                                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = buildInlineMarkdown(block.text, isUser),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontSize = 13.sp,
                                    fontStyle = FontStyle.Italic
                                ),
                                color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                is MarkdownBlock.BulletItem -> {
                    Row(
                        modifier = Modifier.padding(start = 2.dp, top = 1.dp, bottom = 1.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = if (block.prefix.isNotBlank()) "${block.prefix} " else "• ",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = buildInlineMarkdown(block.text, isUser),
                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp, fontSize = 13.5.sp),
                            color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                is MarkdownBlock.Paragraph -> {
                    Text(
                        text = buildInlineMarkdown(block.text, isUser),
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 21.sp, fontSize = 13.5.sp),
                        color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun CodeBlockCard(
    code: String,
    language: String,
    onCopy: () -> Unit
) {
    var copied by remember { mutableStateOf(false) }

    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = language.ifBlank { "code" },
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.clickable {
                        onCopy()
                        copied = true
                    },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (copied) Icons.Filled.Check else Icons.Filled.Description,
                        contentDescription = "Copy",
                        tint = if (copied) BrandPrimaryBright else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (copied) "Tersalin" else "Salin",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (copied) BrandPrimaryBright else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = code,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}
