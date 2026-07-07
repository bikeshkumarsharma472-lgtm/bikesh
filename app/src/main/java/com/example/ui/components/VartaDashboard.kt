package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CallLog
import com.example.data.Contact
import com.example.data.Message
import com.example.ui.VartaViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VartaDashboard(
    viewModel: VartaViewModel,
    onContactSelectedForChat: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val contacts by viewModel.contacts.collectAsState()
    val callLogs by viewModel.callLogs.collectAsState()
    val messages by viewModel.allMessages.collectAsState()

    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabTitles = listOf("Chats", "Contacts", "Call Logs")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Icon(
                            imageVector = Icons.Default.Forum,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Bikesh",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Black,
                                fontSize = 28.sp,
                                letterSpacing = (-0.75).sp
                            )
                        )
                    }
                },
                actions = {
                    // Quick stats / clean action
                    if (selectedTabIndex == 2 && callLogs.isNotEmpty()) {
                        TextButton(
                            onClick = { viewModel.clearCallLogs() },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear Logs", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                        }
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
            // Hero Brand Accent Panel
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Bikesh Calling & Chat Sathi 🤝",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black
                        ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Make real audio/video call simulations and chat in real-time with responsive offline & Gemini AI-powered friends!",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                }
            }

            // Custom pill-styled Navigation bar (from Design HTML style)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabTitles.forEachIndexed { index, title ->
                    val isSelected = selectedTabIndex == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable { selectedTabIndex = index }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = when (index) {
                                    0 -> Icons.Default.ChatBubble
                                    1 -> Icons.Default.People
                                    else -> Icons.Default.Call
                                },
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.sp
                                ),
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Tab content rendering
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (selectedTabIndex) {
                    0 -> ChatsTab(
                        viewModel = viewModel,
                        contacts = contacts,
                        messages = messages,
                        onContactSelectedForChat = onContactSelectedForChat
                    )
                    1 -> ContactsTab(
                        viewModel = viewModel,
                        contacts = contacts,
                        onContactSelectedForChat = onContactSelectedForChat
                    )
                    2 -> CallLogsTab(
                        viewModel = viewModel,
                        contacts = contacts,
                        callLogs = callLogs
                    )
                }
            }
        }
    }
}

// ======================== TABS IMPLEMENTATION ========================

@Composable
fun ChatsTab(
    viewModel: VartaViewModel,
    contacts: List<Contact>,
    messages: List<Message>,
    onContactSelectedForChat: (Int) -> Unit
) {
    // Filter contacts that have at least one message or are favorites to start with
    val activeContacts = remember(contacts, messages) {
        contacts.filter { contact ->
            messages.any { it.contactId == contact.id } || contact.isFavorite
        }
    }

    if (activeContacts.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No active chats. Go to Contacts to start chatting!", style = MaterialTheme.typography.bodyMedium)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(activeContacts) { contact ->
            // Collect the last message for this specific contact
            val lastMessageFlow = remember(contact.id) { viewModel.getLastMessageForContact(contact.id) }
            val lastMessage by lastMessageFlow.collectAsState(initial = null)

            ChatRowItem(
                contact = contact,
                lastMessage = lastMessage,
                onClick = { onContactSelectedForChat(contact.id) }
            )
        }
    }
}

@Composable
fun ChatRowItem(
    contact: Contact,
    lastMessage: Message?,
    onClick: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val timeString = lastMessage?.let { formatter.format(Date(it.timestamp)) } ?: ""

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Avatar with status indicator
        Box(modifier = Modifier.size(54.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = Color(android.graphics.Color.parseColor(contact.avatarColor)),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = contact.name.firstOrNull()?.uppercase() ?: "?",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black
                    )
                )
            }

            // Status Beacon
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(
                        color = if (contact.status == "Online") Color.Green else if (contact.status == "Away") Color.Yellow else Color.Gray,
                        shape = CircleShape
                    )
                    .align(Alignment.BottomEnd)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (timeString.isNotBlank()) {
                    Text(
                        text = timeString,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = lastMessage?.text ?: "No messages yet",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ContactsTab(
    viewModel: VartaViewModel,
    contacts: List<Contact>,
    onContactSelectedForChat: (Int) -> Unit
) {
    val favorites = remember(contacts) { contacts.filter { it.isFavorite } }
    val others = remember(contacts) { contacts.filter { !it.isFavorite } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp)
    ) {
        if (favorites.isNotEmpty()) {
            item {
                SectionHeader(title = "Favorites ⭐")
            }
            items(favorites) { contact ->
                ContactRowItem(
                    contact = contact,
                    onChat = { onContactSelectedForChat(contact.id) },
                    onVoiceCall = { viewModel.initiateCall(contact, "AUDIO") },
                    onVideoCall = { viewModel.initiateCall(contact, "VIDEO") }
                )
            }
        }

        if (others.isNotEmpty()) {
            item {
                SectionHeader(title = "All Contacts")
            }
            items(others) { contact ->
                ContactRowItem(
                    contact = contact,
                    onChat = { onContactSelectedForChat(contact.id) },
                    onVoiceCall = { viewModel.initiateCall(contact, "AUDIO") },
                    onVideoCall = { viewModel.initiateCall(contact, "VIDEO") }
                )
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Black,
            letterSpacing = (-0.5).sp
        ),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
fun ContactRowItem(
    contact: Contact,
    onChat: () -> Unit,
    onVoiceCall: () -> Unit,
    onVideoCall: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(24.dp), // matched to design HTML's highly rounded corners
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile image
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(
                        color = Color(android.graphics.Color.parseColor(contact.avatarColor)),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = contact.name.firstOrNull()?.uppercase() ?: "?",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black
                    )
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black
                    )
                )
                Text(
                    text = contact.phoneNumber,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                Text(
                    text = contact.bio,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            // Actions block
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = onChat) {
                    Icon(Icons.AutoMirrored.Filled.Chat, contentDescription = "Send Message", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onVoiceCall) {
                    Icon(Icons.Default.Call, contentDescription = "Audio Call", tint = MaterialTheme.colorScheme.secondary)
                }
                IconButton(onClick = onVideoCall) {
                    Icon(Icons.Default.Videocam, contentDescription = "Video Call", tint = MaterialTheme.colorScheme.tertiary)
                }
            }
        }
    }
}

@Composable
fun CallLogsTab(
    viewModel: VartaViewModel,
    contacts: List<Contact>,
    callLogs: List<CallLog>
) {
    if (callLogs.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.PhoneCallback,
                    contentDescription = null,
                    modifier = Modifier.size(52.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No Call History",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Any voice or video calls you make will show up here.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        items(callLogs) { log ->
            val contact = remember(log.contactId, contacts) {
                contacts.find { it.id == log.contactId }
            }

            if (contact != null) {
                CallLogRowItem(
                    log = log,
                    contact = contact,
                    onRedial = { viewModel.initiateCall(contact, log.callType) }
                )
            }
        }
    }
}

@Composable
fun CallLogRowItem(
    log: CallLog,
    contact: Contact,
    onRedial: () -> Unit
) {
    val formatter = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
    val dateString = formatter.format(Date(log.timestamp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onRedial)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Small avatar
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(
                    color = Color(android.graphics.Color.parseColor(contact.avatarColor)),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = contact.name.firstOrNull()?.uppercase() ?: "?",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = contact.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Icon(
                    imageVector = when {
                        log.isMissed -> Icons.Default.CallMissed
                        log.isIncoming -> Icons.Default.CallReceived
                        else -> Icons.Default.CallMade
                    },
                    contentDescription = null,
                    tint = if (log.isMissed) Color.Red else Color.Green,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${log.callType} call • $dateString",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            if (!log.isMissed) {
                Text(
                    text = "Duration: ${formatLogDuration(log.durationSeconds)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }

        IconButton(onClick = onRedial) {
            Icon(
                imageVector = if (log.callType == "VIDEO") Icons.Default.Videocam else Icons.Default.Call,
                contentDescription = "Callback",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private fun formatLogDuration(seconds: Int): String {
    if (seconds <= 0) return "0s"
    val mins = seconds / 60
    val secs = seconds % 60
    return if (mins > 0) "${mins}m ${secs}s" else "${secs}s"
}
