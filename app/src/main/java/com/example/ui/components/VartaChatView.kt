package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Contact
import com.example.data.Message
import com.example.ui.VartaViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VartaChatView(
    viewModel: VartaViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val contact by viewModel.selectedContact.collectAsState()
    val messages by viewModel.currentMessages.collectAsState()
    val isSending by viewModel.isSendingMessage.collectAsState()

    var textState by remember { mutableStateOf("") }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    // Automatically scroll to the bottom when new messages arrive or when entering
    LaunchedEffect(messages.size, isSending) {
        if (messages.isNotEmpty()) {
            scope.launch {
                listState.animateScrollToItem(messages.size - 1)
            }
        }
    }

    if (contact == null) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("No Contact Selected", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    val activeContact = contact!!

    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            // Can show contact details if needed
                        }
                    ) {
                        // Avatar circle
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .background(
                                    color = Color(android.graphics.Color.parseColor(activeContact.avatarColor)),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = activeContact.name.firstOrNull()?.uppercase() ?: "?",
                                color = Color.White,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black
                                )
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = activeContact.name,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    letterSpacing = (-0.5).sp
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Start
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            color = if (activeContact.status == "Online" || isSending) Color.Green else Color.Gray,
                                            shape = CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isSending) "Typing..." else activeContact.status,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                actions = {
                    // Favorite Toggle
                    IconButton(onClick = { viewModel.toggleFavorite(activeContact.id, !activeContact.isFavorite) }) {
                        Icon(
                            imageVector = if (activeContact.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite Toggle",
                            tint = if (activeContact.isFavorite) Color.Red else MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Direct Audio Call Button
                    IconButton(onClick = { viewModel.initiateCall(activeContact, "AUDIO") }) {
                        Icon(Icons.Default.Call, contentDescription = "Audio Call")
                    }

                    // Direct Video Call Button
                    IconButton(onClick = { viewModel.initiateCall(activeContact, "VIDEO") }) {
                        Icon(Icons.Default.Videocam, contentDescription = "Video Call")
                    }

                    // Clear conversation sweep button
                    IconButton(onClick = { viewModel.clearChat(activeContact.id) }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Clear Chat", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Bio Notice bar
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = activeContact.bio,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Chat timeline area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                if (messages.isEmpty() && !isSending) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "No messages yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Say Hello to start the discussion! 👋",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        items(messages) { message ->
                            ChatMessageBubble(message = message)
                        }

                        // Animated thinking dots when AI is processing response
                        if (isSending) {
                            item {
                                TypingIndicatorBubble(contactColor = activeContact.avatarColor)
                            }
                        }
                    }
                }
            }

            // Message Input bar
            Surface(
                tonalElevation = 6.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextField(
                        value = textState,
                        onValueChange = { textState = it },
                        placeholder = { Text("Write message here...") },
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(24.dp)),
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Send
                        ),
                        keyboardActions = KeyboardActions(
                            onSend = {
                                if (textState.isNotBlank()) {
                                    viewModel.sendMessage(textState)
                                    textState = ""
                                    keyboardController?.hide()
                                }
                            }
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            if (textState.isNotBlank()) {
                                viewModel.sendMessage(textState)
                                textState = ""
                                keyboardController?.hide()
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatMessageBubble(message: Message) {
    val formatter = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val timeString = formatter.format(Date(message.timestamp))

    val bubbleShape = if (message.isFromMe) {
        RoundedCornerShape(24.dp, 24.dp, 4.dp, 24.dp)
    } else {
        RoundedCornerShape(24.dp, 24.dp, 24.dp, 4.dp)
    }

    val alignment = if (message.isFromMe) Alignment.CenterEnd else Alignment.CenterStart
    val bgBrush = if (message.isFromMe) {
        Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primary,
                MaterialTheme.colorScheme.primary.copy(alpha = 0.9f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = alignment
    ) {
        Column(
            horizontalAlignment = if (message.isFromMe) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 290.dp)
        ) {
            Box(
                modifier = Modifier
                    .background(bgBrush, bubbleShape)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = if (message.isFromMe) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = timeString,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.ExtraBold
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

@Composable
fun TypingIndicatorBubble(contactColor: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    
    // Dot 1 bounce
    val dot1Y by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )

    // Dot 2 bounce (with offset)
    val dot2Y by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, delayMillis = 150, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )

    // Dot 3 bounce (with offset)
    val dot3Y by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, delayMillis = 300, easing = LinearOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Column(modifier = Modifier.widthIn(max = 100.dp)) {
            Box(
                modifier = Modifier
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        RoundedCornerShape(16.dp, 16.dp, 16.dp, 2.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .offset(y = dot1Y.dp)
                            .size(6.dp)
                            .background(Color(android.graphics.Color.parseColor(contactColor)), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .offset(y = dot2Y.dp)
                            .size(6.dp)
                            .background(Color(android.graphics.Color.parseColor(contactColor)), CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .offset(y = dot3Y.dp)
                            .size(6.dp)
                            .background(Color(android.graphics.Color.parseColor(contactColor)), CircleShape)
                    )
                }
            }
        }
    }
}
