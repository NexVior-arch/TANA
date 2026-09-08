package com.tana.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.tana.security.BiometricAuthHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun FullScreenSecurityLockScreen(
    correctPin: String,
    isBiometricAllowed: Boolean,
    failedAttemptsCount: Int,
    maxFailedAttempts: Int = 5,
    onUnlockSuccess: () -> Unit,
    onFailedAttempt: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val coroutineScope = rememberCoroutineScope()

    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var cooldownSeconds by remember { mutableIntStateOf(0) }
    var isCapturingIntruder by remember { mutableStateOf(false) }
    var isUnlockedAnim by remember { mutableStateOf(false) }

    // Shake animation for incorrect PIN
    val shakeOffset = remember { Animatable(0f) }
    
    // Scale bounce for lock icon on entry / interaction
    val lockIconScale = remember { Animatable(0.6f) }
    val lockIconRotation = remember { Animatable(-15f) }

    // Floating breathing pulse for lock glow effect
    val infiniteTransition = rememberInfiniteTransition(label = "lockGlow")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Request camera permission if not granted yet
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ -> }

    LaunchedEffect(Unit) {
        // Entrance animation
        launch {
            lockIconScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        launch {
            lockIconRotation.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Cooldown countdown timer when locked
    LaunchedEffect(cooldownSeconds) {
        if (cooldownSeconds > 0) {
            delay(1000L)
            cooldownSeconds -= 1
        }
    }

    // Attempt Biometric Prompt on launch if allowed
    LaunchedEffect(isBiometricAllowed) {
        if (isBiometricAllowed && activity != null && BiometricAuthHelper.isBiometricAvailable(context) && cooldownSeconds == 0) {
            BiometricAuthHelper.promptBiometric(
                activity = activity,
                title = "TANA Security",
                subtitle = "Pindai sidik jari Anda untuk membuka aplikasi",
                negativeButtonText = "Gunakan PIN",
                onSuccess = {
                    isUnlockedAnim = true
                    coroutineScope.launch {
                        delay(250)
                        onUnlockSuccess()
                    }
                },
                onError = { _ -> },
                onFailed = {
                    onFailedAttempt()
                }
            )
        }
    }

    // Back handler: prevent exiting or going back into the app
    BackHandler(enabled = true) {
        activity?.moveTaskToBack(true)
    }

    val maxPinLength = 4 // Strict 4 digit PIN

    fun triggerShakeAnimation() {
        coroutineScope.launch {
            repeat(3) {
                shakeOffset.animateTo(22f, tween(50, easing = LinearEasing))
                shakeOffset.animateTo(-22f, tween(50, easing = LinearEasing))
            }
            shakeOffset.animateTo(0f, spring(stiffness = Spring.StiffnessHigh))
        }
    }

    fun verifyPin(pinToTest: String) {
        if (cooldownSeconds > 0) return

        if (pinToTest == correctPin) {
            errorMessage = null
            isUnlockedAnim = true
            coroutineScope.launch {
                lockIconScale.animateTo(1.25f, tween(150))
                delay(200)
                onUnlockSuccess()
            }
        } else {
            triggerShakeAnimation()
            enteredPin = ""
            val newAttempt = failedAttemptsCount + 1
            onFailedAttempt()

            if (newAttempt >= maxFailedAttempts) {
                // Silent intruder snapshot & Telegram alert triggered in background
                isCapturingIntruder = true
                
                // Immediately terminate & force close the application
                coroutineScope.launch {
                    delay(350)
                    activity?.finishAffinity()
                }
            } else {
                errorMessage = "PIN Salah! Percobaan $newAttempt dari $maxFailedAttempts"
            }
        }
    }

    fun onKeyPress(digit: String) {
        if (cooldownSeconds > 0) return
        errorMessage = null
        if (enteredPin.length < maxPinLength) {
            val newPin = enteredPin + digit
            enteredPin = newPin
            if (newPin.length == maxPinLength) {
                verifyPin(newPin)
            }
        }
    }

    fun onDeletePress() {
        if (cooldownSeconds > 0) return
        if (enteredPin.isNotEmpty()) {
            enteredPin = enteredPin.dropLast(1)
        }
    }

    fun triggerBiometricManual() {
        if (cooldownSeconds > 0 || activity == null) return
        if (BiometricAuthHelper.isBiometricAvailable(context)) {
            BiometricAuthHelper.promptBiometric(
                activity = activity,
                title = "TANA Security",
                subtitle = "Pindai sidik jari Anda untuk membuka aplikasi",
                negativeButtonText = "Gunakan PIN",
                onSuccess = {
                    isUnlockedAnim = true
                    coroutineScope.launch {
                        delay(250)
                        onUnlockSuccess()
                    }
                },
                onError = { err ->
                    errorMessage = err
                },
                onFailed = {
                    onFailedAttempt()
                }
            )
        } else {
            errorMessage = "Sensor sidik jari tidak tersedia / belum terdaftar di perangkat."
        }
    }

    // Animated full screen background transition
    val screenBgColor by animateColorAsState(
        targetValue = if (isUnlockedAnim) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                      else MaterialTheme.colorScheme.background,
        animationSpec = tween(300),
        label = "bgColor"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(screenBgColor)
            .padding(horizontal = 28.dp, vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxSize()
        ) {
            // Header Top Animated
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(top = 28.dp)
                    .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
            ) {
                // Animated Lock Badge with Spring Scale & Rotation
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .scale(lockIconScale.value * if (cooldownSeconds > 0) 1f else pulseScale)
                        .rotate(lockIconRotation.value)
                        .clip(CircleShape)
                        .background(
                            if (cooldownSeconds > 0) MaterialTheme.colorScheme.error.copy(alpha = 0.18f)
                            else if (isUnlockedAnim) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isUnlockedAnim) Icons.Filled.LockOpen else Icons.Filled.Lock,
                        contentDescription = "Terkunci",
                        tint = if (cooldownSeconds > 0) MaterialTheme.colorScheme.error
                               else if (isUnlockedAnim) MaterialTheme.colorScheme.primary
                               else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(34.dp)
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                Text(
                    text = "FINMONOCHROME",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.5.sp
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (cooldownSeconds > 0) "Aplikasi Terkunci Sementara ($cooldownSeconds d)" 
                           else if (isUnlockedAnim) "Membuka Kunci..."
                           else "Masukkan 4 Digit PIN Keamanan",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (cooldownSeconds > 0) MaterialTheme.colorScheme.error 
                            else if (isUnlockedAnim) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(28.dp))

                // PIN Dots Display with Dynamic Scale & Fill Animation
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until maxPinLength) {
                        val isFilled = i < enteredPin.length
                        
                        val dotScale by animateFloatAsState(
                            targetValue = if (isFilled) 1.25f else 1.0f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "dotScale$i"
                        )
                        
                        val dotSize by animateDpAsState(
                            targetValue = if (isFilled) 18.dp else 14.dp,
                            animationSpec = spring(stiffness = Spring.StiffnessMedium),
                            label = "dotSize$i"
                        )

                        val dotColor by animateColorAsState(
                            targetValue = when {
                                cooldownSeconds > 0 -> MaterialTheme.colorScheme.error
                                isUnlockedAnim -> MaterialTheme.colorScheme.primary
                                isFilled -> MaterialTheme.colorScheme.onBackground
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                            animationSpec = tween(150),
                            label = "dotColor$i"
                        )

                        Box(
                            modifier = Modifier
                                .size(dotSize)
                                .scale(dotScale)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                    }
                }

                // Error / Intruder message with Slide and Fade Animation
                AnimatedVisibility(
                    visible = errorMessage != null,
                    enter = fadeIn(tween(200)) + slideInVertically(initialOffsetY = { -20 }),
                    exit = fadeOut(tween(150))
                ) {
                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(18.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = errorMessage ?: "",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.error,
                                        lineHeight = 16.sp
                                    ),
                                    textAlign = TextAlign.Start
                                )
                            }
                        }
                    }
                }
            }

            // Keypad Number Grid with Animated Press Feedback
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("BIO", "0", "DEL")
                )

                for (row in rows) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (key in row) {
                            when (key) {
                                "BIO" -> {
                                    if (isBiometricAllowed) {
                                        AnimatedKeyButton(
                                            onClick = { triggerBiometricManual() },
                                            enabled = cooldownSeconds == 0
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Fingerprint,
                                                contentDescription = "Sidik Jari",
                                                tint = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.size(70.dp))
                                    }
                                }
                                "DEL" -> {
                                    AnimatedKeyButton(
                                        onClick = { onDeletePress() },
                                        enabled = cooldownSeconds == 0 && enteredPin.isNotEmpty()
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Backspace,
                                            contentDescription = "Hapus Digit",
                                            tint = if (enteredPin.isNotEmpty()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                }
                                else -> {
                                    AnimatedKeyButton(
                                        onClick = { onKeyPress(key) },
                                        enabled = cooldownSeconds == 0
                                    ) {
                                        Text(
                                            text = key,
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 24.sp
                                            ),
                                            color = MaterialTheme.colorScheme.onSurface
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

/**
 * Keypad Button with smooth micro-interaction spring animations and specular glass border
 */
@Composable
private fun AnimatedKeyButton(
    onClick: () -> Unit,
    enabled: Boolean,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "btnScale"
    )

    val bgColor by animateColorAsState(
        targetValue = if (isPressed) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                      else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
        animationSpec = tween(120),
        label = "btnBgColor"
    )

    Box(
        modifier = Modifier
            .size(72.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(bgColor)
            .border(
                BorderStroke(
                    1.dp,
                    if (isPressed) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                ),
                CircleShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
