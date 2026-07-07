package com.example.data

import com.example.network.GeminiClient
import kotlinx.coroutines.flow.Flow

class VartaRepository(
    private val contactDao: ContactDao,
    private val messageDao: MessageDao,
    private val callLogDao: CallLogDao
) {
    val allContacts: Flow<List<Contact>> = contactDao.getAllContacts()
    val allCallLogs: Flow<List<CallLog>> = callLogDao.getAllCallLogs()
    val allMessages: Flow<List<Message>> = messageDao.getAllMessages()

    suspend fun getContactById(id: Int): Contact? = contactDao.getContactById(id)

    fun getMessagesForContact(contactId: Int): Flow<List<Message>> =
        messageDao.getMessagesForContact(contactId)

    fun getLastMessageForContact(contactId: Int): Flow<Message?> =
        messageDao.getLastMessageForContact(contactId)

    suspend fun insertContact(contact: Contact) = contactDao.insertContact(contact)

    suspend fun updateFavorite(id: Int, isFavorite: Boolean) = contactDao.updateFavorite(id, isFavorite)

    suspend fun insertMessage(message: Message) = messageDao.insertMessage(message)

    suspend fun insertCallLog(callLog: CallLog) = callLogDao.insertCallLog(callLog)

    suspend fun clearAllLogs() = callLogDao.clearAllLogs()

    suspend fun deleteMessagesForContact(contactId: Int) = messageDao.deleteMessagesForContact(contactId)

    /**
     * Sends a message from the user and triggers an AI response (Gemini or offline local simulation).
     */
    suspend fun sendMessageAndGetReply(contactId: Int, userMessageText: String, history: List<Message>) {
        // 1. Save user's message
        val userMsg = Message(
            contactId = contactId,
            text = userMessageText,
            isFromMe = true,
            timestamp = System.currentTimeMillis()
        )
        insertMessage(userMsg)

        // 2. Fetch contact personality detail
        val contact = getContactById(contactId) ?: return

        // 3. Get AI response from Gemini Client
        val replyText = GeminiClient.getReply(
            contactName = contact.name,
            personalityPrompt = contact.personality,
            userMessage = userMessageText,
            chatHistory = history
        )

        // 4. Save AI companion's reply
        val aiMsg = Message(
            contactId = contactId,
            text = replyText,
            isFromMe = false,
            timestamp = System.currentTimeMillis()
        )
        insertMessage(aiMsg)
    }
}
