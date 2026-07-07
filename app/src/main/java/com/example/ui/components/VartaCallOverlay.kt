package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.Contact
import com.example.ui.VartaViewModel
import kotlinx.coroutines.launch

@Composable
fun VartaCallOverlay(
    viewModel: VartaViewModel,
    modifier: Modifier = Modifier
) {
    val activeContact by viewModel.activeCallContact.collectAsState()
    val callType by viewModel.callType.collectAsState()
    val callStatus by viewModel.callStatus.collectAsState()
    val duration by viewModel.callDuration.collectAsState()
    val isMuted by viewModel.isMuted.collectAsState()
    val isSpeakerOn by viewModel.isSpeakerOn.collectAsState()
    val localCameraEnabled by viewModel.localCameraEnabled.collectAsState()
    val subtitles by viewModel.callSubtitles.collectAsState()

    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    // Launch permission request when a video call is connected
    LaunchedEffect(callStatus, callType) {
        if (callType == "VIDEO" && callStatus == "CONNECTED" && !hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (activeContact == null || callStatus == "IDLE") return

    val contact = activeContact!!

    // Pulsating animation for calling avatar
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    // Base background colors
    val darkBgColor = Color(0xFF0F172A) // Near-black deep slate blue
    val secondaryDark = Color(0xFF1E293B)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(darkBgColor, Color(0xFF020617))
                )
            )
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) {
        // --- Video Call Feed Area ---
        if (callType == "VIDEO" && callStatus == "CONNECTED") {
            // Main remote contact representation
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    ContactLargeAvatar(contact = contact, pulseScale = 1f, pulseAlpha = 0f)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "${contact.name} (Video Feed)",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Remote Camera Active",
                        color = Color.Green.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Floating PIP Local Camera Feed
            Box(
                modifier = Modifier
                    .padding(16.dp)
                    .size(110.dp, 160.dp)
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(16.dp))
                    .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(16.dp))
            ) {
                if (localCameraEnabled && hasCameraPermission) {
                    CameraPreview(modifier = Modifier.fillMaxSize())
                } else {
                    CameraPlaceholder(
                        modifier = Modifier.fillMaxSize(),
                        text = if (!hasCameraPermission && localCameraEnabled) "No Permission" else "Cam Off"
                    )
                }
            }
        } else {
            // --- Audio Call Large Centered Waveform Area ---
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.6f)
                    .align(Alignment.TopCenter),
                contentAlignment = Alignment.Center
            ) {
                // Pulsating wave rings
                if (callStatus == "CONNECTED" || callStatus == "DIALING" || callStatus == "RINGING") {
                    Box(
                        modifier = Modifier
                            .size(180.dp)
                            .scale(pulseScale)
                            .background(
                                color = Color(android.graphics.Color.parseColor(contact.avatarColor))
                                    .copy(alpha = pulseAlpha),
                                shape = CircleShape
                            )
                    )
                    Box(
                        modifier = Modifier
                            .size(240.dp)
                            .scale(pulseScale * 0.8f)
                            .background(
                                color = Color(android.graphics.Color.parseColor(contact.avatarColor))
                                    .copy(alpha = pulseAlpha * 0.6f),
                                shape = CircleShape
                            )
                    )
                }

                // Centered contact photo
                ContactLargeAvatar(contact = contact, pulseScale = 1.05f, pulseAlpha = 1f)
            }
        }

        // --- Caller Information ---
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 40.dp)
                .align(Alignment.TopCenter)
        ) {
            Text(
                text = contact.name,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (callType == "VIDEO") Icons.Default.Videocam else Icons.Default.Call,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = when (callStatus) {
                        "DIALING" -> "Dialing..."
                        "RINGING" -> "Ringing..."
                        "CONNECTED" -> "Connected"
                        "ENDED" -> "Call Ended"
                        else -> ""
                    },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (callStatus == "CONNECTED") Color.Green else Color.White.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                )
            }
            if (callStatus == "CONNECTED") {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = formatDuration(duration),
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                )
            }
        }

        // --- Live AI Speech Subtitles (Conversational Transcript) ---
        AnimatedVisibility(
            visible = callStatus == "CONNECTED" && subtitles.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .height(130.dp)
                .align(Alignment.BottomCenter)
                .offset(y = (-110).dp)
        ) {
            val listState = rememberLazyListState()
            val scope = rememberCoroutineScope()

            LaunchedEffect(subtitles.size) {
                if (subtitles.isNotEmpty()) {
                    scope.launch {
                        listState.animateScrollToItem(subtitles.size - 1)
                    }
                }
            }

            Box(
                modifier = Modifier
                    .background(
                        Color.Black.copy(alpha = 0.45f),
                        RoundedCornerShape(16.dp)
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                    .padding(12.dp)
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(subtitles) { subtitle ->
                        val isOpening = subtitle.contains("Dialing") || subtitle.contains("Ringing") || subtitle.contains("Connected") || subtitle.contains("Ended")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!isOpening) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            Color(android.graphics.Color.parseColor(contact.avatarColor)),
                                            CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(
                                text = subtitle,
                                color = if (isOpening) Color.White.copy(alpha = 0.6f) else Color.White,
                                style = if (isOpening) MaterialTheme.typography.bodySmall else MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium
                                )
                            )
                        }
                    }
                }
            }
        }

        // --- Calling Action Controls Buttons Bar ---
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .align(Alignment.BottomCenter)
        ) {
            // Mute Button
            IconButton(
                onClick = { viewModel.toggleMute() },
                modifier = Modifier
                    .size(54.dp)
                    .background(
                        color = if (isMuted) Color.White else Color.White.copy(alpha = 0.15f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = if (isMuted) "Unmute" else "Mute",
                    tint = if (isMuted) Color.Black else Color.White
                )
            }

            // Local Camera Toggle Button (Only for Video Calls)
            if (callType == "VIDEO") {
                IconButton(
                    onClick = { viewModel.toggleLocalCamera() },
                    modifier = Modifier
                        .size(54.dp)
                        .background(
                            color = if (localCameraEnabled) Color.White.copy(alpha = 0.15f) else Color.Red.copy(alpha = 0.8f),
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = if (localCameraEnabled) Icons.Default.Videocam else Icons.Default.VideocamOff,
                        contentDescription = "Toggle Local Camera",
                        tint = Color.White
                    )
                }
            }

            // Speaker Toggle Button
            IconButton(
                onClick = { viewModel.toggleSpeaker() },
                modifier = Modifier
                    .size(54.dp)
                    .background(
                        color = if (isSpeakerOn) Color.White else Color.White.copy(alpha = 0.15f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = if (isSpeakerOn) Icons.Default.VolumeUp else Icons.Default.VolumeDown,
                    contentDescription = "Speaker Toggle",
                    tint = if (isSpeakerOn) Color.Black else Color.White
                )
            }

            // End Call Button (RED)
            IconButton(
                onClick = { viewModel.endActiveCall() },
                modifier = Modifier
                    .size(62.dp)
                    .background(
                        color = MaterialTheme.colorScheme.error,
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.CallEnd,
                    contentDescription = "End Call",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

@Composable
fun ContactLargeAvatar(
    contact: Contact,
    pulseScale: Float,
    pulseAlpha: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(110.dp)
            .scale(pulseScale)
            .background(
                color = Color(android.graphics.Color.parseColor(contact.avatarColor)),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = contact.name.firstOrNull()?.uppercase() ?: "?",
            color = Color.White,
            style = MaterialTheme.typography.displayMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 44.sp
            )
        )
    }
}

private fun formatDuration(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", mins, secs)
}
