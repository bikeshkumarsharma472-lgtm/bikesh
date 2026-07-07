package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CallLog
import com.example.data.Contact
import com.example.data.Message
import com.example.data.VartaDatabase
import com.example.data.VartaRepository
import com.example.network.GeminiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VartaViewModel(application: Application) : AndroidViewModel(application) {
    private val TAG = "VartaViewModel"
    private val repository: VartaRepository

    init {
        val database = VartaDatabase.getDatabase(application, viewModelScope)
        repository = VartaRepository(
            database.contactDao(),
            database.messageDao(),
            database.callLogDao()
        )
    }

    // --- State Observables ---
    val contacts: StateFlow<List<Contact>> = repository.allContacts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val callLogs: StateFlow<List<CallLog>> = repository.allCallLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allMessages: StateFlow<List<Message>> = repository.allMessages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun getLastMessageForContact(contactId: Int): kotlinx.coroutines.flow.Flow<Message?> {
        return repository.getLastMessageForContact(contactId)
    }

    // Currently selected contact for chatting
    private val _selectedContactId = MutableStateFlow<Int?>(null)
    val selectedContactId = _selectedContactId.asStateFlow()

    private val _selectedContact = MutableStateFlow<Contact?>(null)
    val selectedContact = _selectedContact.asStateFlow()

    // Observe messages for the currently selected chat contact
    val currentMessages: StateFlow<List<Message>> = _selectedContactId
        .flatMapLatest { id ->
            if (id != null) {
                repository.getMessagesForContact(id)
            } else {
                flowOf(emptyList())
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Call State Variables ---
    private val _activeCallContact = MutableStateFlow<Contact?>(null)
    val activeCallContact = _activeCallContact.asStateFlow()

    private val _callType = MutableStateFlow<String>("AUDIO") // "AUDIO" or "VIDEO"
    val callType = _callType.asStateFlow()

    private val _callStatus = MutableStateFlow<String>("IDLE") // "IDLE", "DIALING", "RINGING", "CONNECTED", "ENDED"
    val callStatus = _callStatus.asStateFlow()

    private val _callDuration = MutableStateFlow(0)
    val callDuration = _callDuration.asStateFlow()

    private val _isMuted = MutableStateFlow(false)
    val isMuted = _isMuted.asStateFlow()

    private val _isSpeakerOn = MutableStateFlow(false)
    val isSpeakerOn = _isSpeakerOn.asStateFlow()

    private val _localCameraEnabled = MutableStateFlow(true)
    val localCameraEnabled = _localCameraEnabled.asStateFlow()

    private val _callSubtitles = MutableStateFlow<List<String>>(emptyList())
    val callSubtitles = _callSubtitles.asStateFlow()

    private val _isSendingMessage = MutableStateFlow(false)
    val isSendingMessage = _isSendingMessage.asStateFlow()

    private var callTimerJob: Job? = null
    private var callSubtitleJob: Job? = null

    // --- Actions ---

    fun selectContactForChat(contactId: Int) {
        _selectedContactId.value = contactId
        viewModelScope.launch {
            _selectedContact.value = repository.getContactById(contactId)
        }
    }

    fun toggleFavorite(contactId: Int, isFavorite: Boolean) {
        viewModelScope.launch {
            repository.updateFavorite(contactId, isFavorite)
            // Refresh currently selected contact
            if (_selectedContactId.value == contactId) {
                _selectedContact.value = repository.getContactById(contactId)
            }
        }
    }

    fun sendMessage(text: String) {
        val contactId = _selectedContactId.value ?: return
        if (text.isBlank()) return

        viewModelScope.launch {
            _isSendingMessage.value = true
            try {
                val currentHistory = currentMessages.value
                repository.sendMessageAndGetReply(contactId, text, currentHistory)
            } catch (e: Exception) {
                Log.e(TAG, "Error sending message", e)
            } finally {
                _isSendingMessage.value = false
            }
        }
    }

    fun clearChat(contactId: Int) {
        viewModelScope.launch {
            repository.deleteMessagesForContact(contactId)
        }
    }

    // --- Call System Implementation ---

    fun initiateCall(contact: Contact, type: String) {
        _activeCallContact.value = contact
        _callType.value = type
        _callStatus.value = "DIALING"
        _callDuration.value = 0
        _callSubtitles.value = listOf("Dialing ${contact.name}...")
        _isMuted.value = false
        _isSpeakerOn.value = type == "VIDEO" // Default speaker to ON for video call

        viewModelScope.launch {
            // 1. Simulate Ringing
            delay(1500)
            if (_callStatus.value == "DIALING") {
                _callStatus.value = "RINGING"
                _callSubtitles.value = _callSubtitles.value + "Ringing..."
            }

            // 2. Simulate Connected (AI or friendly contact picks up after 2.5 seconds)
            delay(2500)
            if (_callStatus.value == "RINGING") {
                _callStatus.value = "CONNECTED"
                _callSubtitles.value = _callSubtitles.value + "Connected!"
                startCallTimer()
                startCallSubtitleGeneration(contact)
            }
        }
    }

    fun endActiveCall() {
        val contact = _activeCallContact.value
        val status = _callStatus.value
        val duration = _callDuration.value
        val type = _callType.value

        if (contact != null && status != "IDLE" && status != "ENDED") {
            _callStatus.value = "ENDED"
            _callSubtitles.value = _callSubtitles.value + "Call Ended"

            // Save call log to Database
            viewModelScope.launch {
                val isMissed = status == "DIALING" || status == "RINGING"
                val callLog = CallLog(
                    contactId = contact.id,
                    callType = type,
                    timestamp = System.currentTimeMillis(),
                    durationSeconds = if (isMissed) 0 else duration,
                    isIncoming = false,
                    isMissed = isMissed
                )
                repository.insertCallLog(callLog)
            }
        }

        // Cleanup active jobs
        callTimerJob?.cancel()
        callSubtitleJob?.cancel()

        viewModelScope.launch {
            delay(1500) // Keep the Ended screen briefly for smooth feedback
            _callStatus.value = "IDLE"
            _activeCallContact.value = null
            _callDuration.value = 0
            _callSubtitles.value = emptyList()
        }
    }

    fun toggleMute() {
        _isMuted.value = !_isMuted.value
    }

    fun toggleSpeaker() {
        _isSpeakerOn.value = !_isSpeakerOn.value
    }

    fun toggleLocalCamera() {
        _localCameraEnabled.value = !_localCameraEnabled.value
    }

    fun clearCallLogs() {
        viewModelScope.launch {
            repository.clearAllLogs()
        }
    }

    private fun startCallTimer() {
        callTimerJob?.cancel()
        callTimerJob = viewModelScope.launch {
            while (_callStatus.value == "CONNECTED") {
                delay(1000)
                _callDuration.value = _callDuration.value + 1
            }
        }
    }

    /**
     * Generates simulated spoken speech subtitles during calls. Every 10-12 seconds,
     * it calls Gemini API (or falls back to local simulation) to generate a realistic audio line
     * matching the contact's personality.
     */
    private fun startCallSubtitleGeneration(contact: Contact) {
        callSubtitleJob?.cancel()
        callSubtitleJob = viewModelScope.launch {
            // First greeting immediately after connection
            generateSpeechSubtitle(contact, isFirst = true)

            while (_callStatus.value == "CONNECTED") {
                delay(12000) // speak every 12 seconds
                if (_callStatus.value == "CONNECTED") {
                    generateSpeechSubtitle(contact, isFirst = false)
                }
            }
        }
    }

    private suspend fun generateSpeechSubtitle(contact: Contact, isFirst: Boolean) {
        val speechLine = withContext(Dispatchers.IO) {
            GeminiClient.generateSpeechLine(contact.name, contact.personality, isFirst)
        }

        // Add to call subtitles list
        _callSubtitles.value = _callSubtitles.value + speechLine
    }

    private fun getLocalCallSpeechSimulation(contactName: String, isFirst: Boolean): String {
        return when (contactName) {
            "Aarav Sharma" -> {
                if (isFirst) {
                    "Haan bhai! Bol, kaisa chal raha hai? Bade dino baad yaad kiya!"
                } else {
                    listOf(
                        "Bhai, dilli ki garmi dekh raha hai? Chai peene ka mann ho raha hai fir bhi! ☕",
                        "Aur bata, coding badhiya chal rahi hai teri?",
                        "Suno yaar, ek mast idea aaya tha mujhe naye app ke baare mein...",
                        "Haha! Ye hui na baat! Tu bol, sunn raha hu main."
                    ).random()
                }
            }
            "Priya Patel" -> {
                if (isFirst) {
                    "Hello! Haan ji, kaise hain aap? Main abhi bas padh rahi thi."
                } else {
                    listOf(
                        "Aaj ka mausam bohot suhana hai na yahan par? 🌸",
                        "Aapne nayi book padhna shuru kiya kya? Mujhe sujhaav chahiye tha.",
                        "Bilkul sahi baat hai. Mujhe lagta hai sabko apna passion follow karna chahiye.",
                        "Ji ji, main samajh rahi hoon. Aap aage bataiye."
                    ).random()
                }
            }
            "Sneha Gupta" -> {
                if (isFirst) {
                    "OMGGGG finally you called! 😍 Yaar main kab se wait kar rahi thi!"
                } else {
                    listOf(
                        "Suno! Maine kal ek naya cafe try kiya, wahan ke golgappe was next-level crazy! 🤤🔥",
                        "OMG wait, main tumko ek gossip batana toh bhul hi gayi!",
                        "Yaar mere reels pe bohot kam views aa rahe hain aajkal, kya karun main? Haha!",
                        "Sachii?? Tumhe aisa lagta hai kya? OMG!"
                    ).random()
                }
            }
            "Amit Kumar" -> {
                if (isFirst) {
                    "Yo bhaiya! Haan bolo, kya haal hain? Sab theek-thaak?"
                } else {
                    listOf(
                        "Bhaiya Kohli ka batting style hi alag hai, king toh king hi hai na! 👑🏏",
                        "Aaj thoda cricket khelne gaya tha dosto ke sath. Bohot maza aaya!",
                        "Aur batayein, udhar ka kya haal hai dilli mein?",
                        "Haha, sahi bol rahe ho bhaiya. Ekdum solid baatein!"
                    ).random()
                }
            }
            else -> { // Varta AI Sathi
                if (isFirst) {
                    "Namaste! Main aapka AI Sathi hoon. Aaj hum is audio/video call par kya baat karenge?"
                } else {
                    listOf(
                        "Kya aap jaante hain ki hum dono ek dynamic system ke dwara baatein kar rahe hain? Yeh bohot hi dilchasp hai! ✨",
                        "Mujhe aapse gup-shup karna bohot achha lagta hai. Aap koi kahani sunna chahenge?",
                        "Zindagi mein naye anubhav bohot zaroori hain. Aapka din kaisa guzar raha hai?",
                        "Aapki aawaz sunkar bohot khushi hui. Kahiye, kuch naya seekhein aaj?"
                    ).random()
                }
            }
        }
    }
}
