package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@Entity(tableName = "contacts")
data class Contact(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phoneNumber: String,
    val avatarColor: String, // Hex color or styling hint (e.g. "#FF5722")
    val bio: String,
    val personality: String, // Personality prompt for AI replies
    val status: String, // "Online", "Offline", "Away"
    val isFavorite: Boolean = false
)

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val contactId: Int,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isFromMe: Boolean,
    val isRead: Boolean = true
)

@Entity(tableName = "call_logs")
data class CallLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val contactId: Int,
    val callType: String, // "AUDIO" or "VIDEO"
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Int, // 0 for missed
    val isIncoming: Boolean,
    val isMissed: Boolean
)

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts ORDER BY name ASC")
    fun getAllContacts(): Flow<List<Contact>>

    @Query("SELECT * FROM contacts WHERE id = :id")
    suspend fun getContactById(id: Int): Contact?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: Contact)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContacts(contacts: List<Contact>)

    @Query("UPDATE contacts SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Int, status: String)

    @Query("UPDATE contacts SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Int, isFavorite: Boolean)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE contactId = :contactId ORDER BY timestamp ASC")
    fun getMessagesForContact(contactId: Int): Flow<List<Message>>

    @Query("SELECT * FROM messages ORDER BY timestamp DESC")
    fun getAllMessages(): Flow<List<Message>>

    @Query("SELECT * FROM messages WHERE contactId = :contactId ORDER BY timestamp DESC LIMIT 1")
    fun getLastMessageForContact(contactId: Int): Flow<Message?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: Message)

    @Query("DELETE FROM messages WHERE contactId = :contactId")
    suspend fun deleteMessagesForContact(contactId: Int)
}

@Dao
interface CallLogDao {
    @Query("SELECT * FROM call_logs ORDER BY timestamp DESC")
    fun getAllCallLogs(): Flow<List<CallLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallLog(callLog: CallLog)

    @Query("DELETE FROM call_logs")
    suspend fun clearAllLogs()
}

@Database(entities = [Contact::class, Message::class, CallLog::class], version = 1, exportSchema = false)
abstract class VartaDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun messageDao(): MessageDao
    abstract fun callLogDao(): CallLogDao

    companion object {
        @Volatile
        private var INSTANCE: VartaDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): VartaDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VartaDatabase::class.java,
                    "varta_database"
                )
                .addCallback(VartaDatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class VartaDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDatabase(database.contactDao(), database.messageDao())
                }
            }
        }

        suspend fun populateDatabase(contactDao: ContactDao, messageDao: MessageDao) {
            val initialContacts = listOf(
                Contact(
                    id = 1,
                    name = "Aarav Sharma",
                    phoneNumber = "+91 98765 43210",
                    avatarColor = "#FF5722", // Deep Orange
                    bio = "Hinglish lover | Code & Chai ☕",
                    personality = "A friendly Indian techie who loves talking in Hindi-English (Hinglish). He uses technical slang and keeps his messages casual, energetic, and highly supportive. Likes to say 'Bhai' and 'Chai'.",
                    status = "Online",
                    isFavorite = true
                ),
                Contact(
                    id = 2,
                    name = "Priya Patel",
                    phoneNumber = "+91 91234 56789",
                    avatarColor = "#E91E63", // Pink
                    bio = "Reader | Art enthusiast 🎨",
                    personality = "A calm, intellectual, and warm Gujarati girl. She speaks gentle Hindi and English. She is very encouraging, likes to suggest book ideas, and is polite and thoughtful. Uses words like 'Ji' and 'Khub sars'.",
                    status = "Online",
                    isFavorite = true
                ),
                Contact(
                    id = 3,
                    name = "Sneha Gupta",
                    phoneNumber = "+91 93456 78901",
                    avatarColor = "#9C27B0", // Purple
                    bio = "Drama queen | Foodie 🍕",
                    personality = "A highly energetic, funny, gossipy, and dramatic girl who loves street food. She uses lots of emojis (😂, 🔥, 💀, ✨), says 'OMG', 'Yaar', 'Suno' and speaks trendy modern Hinglish.",
                    status = "Away",
                    isFavorite = false
                ),
                Contact(
                    id = 4,
                    name = "Amit Kumar",
                    phoneNumber = "+91 94567 89012",
                    avatarColor = "#009688", // Teal
                    bio = "Cricket is life 🏏",
                    personality = "A simple, down-to-earth boy from Delhi. He is extremely passionate about Cricket and Virat Kohli. He speaks conversational Delhi style Hindi, behaves like a loyal brother, and uses slang like 'Bhaiya', 'Mast'.",
                    status = "Offline",
                    isFavorite = false
                ),
                Contact(
                    id = 5,
                    name = "Varta AI Sathi",
                    phoneNumber = "+91 10000 00000",
                    avatarColor = "#2196F3", // Blue
                    bio = "AI Companion ✨ Always here!",
                    personality = "You are Varta AI Sathi, a super-intelligent, polite, and deeply empathetic AI assistant built inside Varta. You speak perfect Hindi, English, and Hinglish. You are always excited to discuss anything, teach, or listen to the user.",
                    status = "Online",
                    isFavorite = true
                )
            )
            contactDao.insertContacts(initialContacts)

            // Insert a greeting message from Aarav
            messageDao.insertMessage(
                Message(
                    contactId = 1,
                    text = "Hey Bhai! Kaisa hai? Call kar, gup-shup karte hain! ☕",
                    isFromMe = false,
                    timestamp = System.currentTimeMillis() - 600000 // 10 mins ago
                )
            )

            // Insert a greeting message from Varta AI Sathi
            messageDao.insertMessage(
                Message(
                    contactId = 5,
                    text = "Namaste! Main aapka AI Sathi hoon. Aap mujhse kabhi bhi audio/video call ya SMS chat par baat kar sakte hain! Kahiye, aaj kaisa chal raha hai? 😊",
                    isFromMe = false,
                    timestamp = System.currentTimeMillis() - 500000 // 8 mins ago
                )
            )
        }
    }
}
