package com.example.network

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

@JsonClass(generateAdapter = true)
data class Part(
    val text: String? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    val parts: List<Part>
)

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    val contents: List<Content>,
    val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    val content: Content
)

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null
)

interface GeminiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val service: GeminiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiService::class.java)
    }

    /**
     * Generates a casual spoken subtitle line during a call.
     */
    suspend fun generateSpeechLine(contactName: String, personalityPrompt: String, isFirst: Boolean): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return generateLocalSimulationReply(contactName, if (isFirst) "hello" else "tell me a story")
        }

        val prompt = if (isFirst) {
            "Generate a 1-sentence lively call answering opening line (Hinglish/Hindi/English) as $contactName based on personality: $personalityPrompt. Keep it conversational."
        } else {
            "Generate a 1-sentence casual conversational filler line (Hinglish/Hindi/English) as $contactName during an active call based on personality: $personalityPrompt. Ask how they are doing or talk about Cricket, Chai, Books or gossip."
        }
        val systemPrompt = "You are speaking on a real-time voice call. Speak naturally, do not use bullet points or sound robotic."
        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
        )
        return try {
            val response = service.generateContent(apiKey, request)
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim() ?: ""
        } catch (e: Exception) {
            Log.e(TAG, "Error generating speech line", e)
            ""
        }
    }

    /**
     * Generates a conversational message reply from a contact based on their personality and previous history.
     */
    suspend fun getReply(
        contactName: String,
        personalityPrompt: String,
        userMessage: String,
        chatHistory: List<com.example.data.Message>
    ): String {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "Gemini API Key is not configured. Falling back to local offline simulation.")
            return generateLocalSimulationReply(contactName, userMessage)
        }

        // Format history for the API (last 10 messages for context)
        val apiContents = mutableListOf<Content>()
        
        // Add chat history to feed conversation context
        chatHistory.takeLast(10).forEach { msg ->
            apiContents.add(Content(parts = listOf(Part(text = msg.text))))
        }
        
        // Add the current user message
        apiContents.add(Content(parts = listOf(Part(text = userMessage))))

        val systemPrompt = "You are $contactName. Here is your persona detail: $personalityPrompt. " +
                "You are texting with the user inside an Android chat application named 'Varta'. " +
                "Keep your reply brief (maximum 2 sentences), warm, highly conversational, and natural. " +
                "Do NOT sound like an AI assistant or prefix your messages with your name or any label. " +
                "Speak exactly as per your defined native language, dialect, and slang (Hinglish/Hindi/English/Delhi-style/emoji-use) to be 100% convincing. " +
                "Respond directly to their last message."

        val request = GenerateContentRequest(
            contents = apiContents,
            systemInstruction = Content(parts = listOf(Part(text = systemPrompt)))
        )

        return try {
            val response = service.generateContent(apiKey, request)
            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (!text.isNullOrBlank()) {
                text.trim()
            } else {
                generateLocalSimulationReply(contactName, userMessage)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calling Gemini API, falling back to local simulation", e)
            generateLocalSimulationReply(contactName, userMessage)
        }
    }

    /**
     * Fallback local offline responses when API is missing or error occurs.
     */
    fun generateLocalSimulationReply(contactName: String, msg: String): String {
        val messageLower = msg.lowercase()
        return when (contactName) {
            "Aarav Sharma" -> {
                when {
                    messageLower.contains("hello") || messageLower.contains("hey") || messageLower.contains("hi") -> {
                        listOf(
                            "Hey bhai! Aur bata, kaisa chal raha hai sab? Aaj bohot dino baad yaad kiya! ☕",
                            "Hi brother! Sab mast? Main abhi bas thoda coding kar raha tha, batao kya haal hain?",
                            "Arre hello bhai! Sab theek-thaak? Chai-wai pi ki nahi? 😂"
                        ).random()
                    }
                    messageLower.contains("call") || messageLower.contains("phone") -> {
                        "Haan bilkul bhai! Mujhe audio ya video call laga, abhi free hoon bas baatein karte hain!"
                    }
                    messageLower.contains("kaisa") || messageLower.contains("how are") -> {
                        "Main toh ekdum first class bhai! Tum batao, tumhara kya chal raha hai aajkal?"
                    }
                    messageLower.contains("kya kar") || messageLower.contains("doing") -> {
                        "Kuchh nahi yaar, bas thoda android app debug kar raha tha. Code fat gaya tha mera haha! Kahiye, kya chal raha hai?"
                    }
                    else -> {
                        listOf(
                            "Arre waah, sahi baat hai bhai! Aur batao, sab ghar pe badhiya?",
                            "Sach boloon toh, main bhi yahi soch raha tha! Chalo chodo, call lagao abhi masti karenge! 😄",
                            "Bhai, ye toh bilkul sahi bola tumne. Chalo, sham ko milte hain ya call pe aate hain?"
                        ).random()
                    }
                }
            }
            "Priya Patel" -> {
                when {
                    messageLower.contains("hello") || messageLower.contains("hey") || messageLower.contains("hi") -> {
                        listOf(
                            "Hello! Kaise hain aap? Kaafi achha laga aapka message dekh kar. 😊",
                            "Namaste! Sab thik hai na? Bataiye aaj kaisa chal raha hai aapka din?"
                        ).random()
                    }
                    messageLower.contains("call") || messageLower.contains("phone") -> {
                        "Haan ji, aap mujhe call kar sakte hain. Thodi der sukoon se baat karenge."
                    }
                    messageLower.contains("kaisa") || messageLower.contains("how are") -> {
                        "Main achhi hoon, dhanyawaad. Bas abhi ek naya art canvas complete kiya hai. Aap batayein?"
                    }
                    messageLower.contains("book") || messageLower.contains("padh") || messageLower.contains("read") -> {
                        "Haan! Mujhe kitabein padhna bohot pasand hai. Abhi Premchand ki kahaniyan padh rahi hoon. Aapko padhna pasand hai?"
                    }
                    else -> {
                        listOf(
                            "Aapki baatein humesha bohot samajhdari se bhari hoti hain. Dhanyawaad iske liye.",
                            "Ji bilkul, main samajh sakti hoon. Zindagi mein thoda sukoon bohot zaroori hai.",
                            "Chalo, bohot badhiya! Aap kal kya kar rahe hain? Mil kar baatein karenge?"
                        ).random()
                    }
                }
            }
            "Sneha Gupta" -> {
                when {
                    messageLower.contains("hello") || messageLower.contains("hey") || messageLower.contains("hi") -> {
                        "OMGGGG hellloooo!! ✨ Kitne dino baad yaad kiya yaar! Miss you so much! Kya chal raha hai? 😍"
                    }
                    messageLower.contains("call") || messageLower.contains("phone") -> {
                        "Yaar abhi call karo na pleaseeee! Mujhe bohot saari gossip sunani hai tumko! Fast fast call karo! 📞🔥"
                    }
                    messageLower.contains("kaisa") || messageLower.contains("how are") -> {
                        "Main ekdum bindass yaar! Abhi golgappe kha ke aayi hoon, mood ekdum jhakas ho gaya! Tum batao? 😂🍿"
                    }
                    else -> {
                        listOf(
                            "Oh my god, sach mein?? 😂 Mujhe pata hi tha! Aur sunao, aur kya chal raha hai?",
                            "Ye toh bohot crazy hai yaaaar! 💀 Tum hamesha aisi baatein batate ho ki main hassi nahi rok paati!",
                            "Awww so sweet! Suno, mujhe thode pizza khane ka man ho raha hai, order kar do na! Hehe joke tha! 😉"
                        ).random()
                    }
                }
            }
            "Amit Kumar" -> {
                when {
                    messageLower.contains("hello") || messageLower.contains("hey") || messageLower.contains("hi") -> {
                        "Yo bhaiya! Kya haal hain? Sab badhiya? Aaj dilli mein bohot garmi hai yaar! 😂"
                    }
                    messageLower.contains("cricket") || messageLower.contains("match") || messageLower.contains("kohli") -> {
                        "Bhai Kohli ka batting dekha kal? Ekdum kamaal tha yaar! Century maar di fir se king ne! 👑🏏"
                    }
                    messageLower.contains("call") || messageLower.contains("phone") -> {
                        "Haan bhaiya, lagao call! Ground pe hu par thoda baatein toh kar hi sakte hain."
                    }
                    messageLower.contains("kaisa") || messageLower.contains("how are") -> {
                        "Main ekdum fit and fine hu, bilkul masti mein. Tum batao, kya haal-chaal hain?"
                    }
                    else -> {
                        listOf(
                            "Bhaiya tum toh bade champion nikle! Bilkul mast bola tumne.",
                            "Haan yaar, sahi hai. Suno, shaam ko match dekhne chalein kya?",
                            "Chal badhiya hai bhai, koi dikkat nahi. Sab badhiya hi hoga!"
                        ).random()
                    }
                }
            }
            else -> { // Varta AI Sathi
                when {
                    messageLower.contains("hello") || messageLower.contains("hey") || messageLower.contains("hi") -> {
                        "Hello ji! Main aapka Varta AI Sathi hoon. Main aapke har sawal ka jawab dene aur baatein karne ke liye humesha online rehta hoon. 😊 Kahiye, aaj main aapki kya madad karoon?"
                    }
                    messageLower.contains("kaisa") || messageLower.contains("how are") -> {
                        "Main bilkul swasth aur khush hoon! Ek AI hone ke naate mujhe aap jaise mitron se baatein karna bohot hi pyara lagta hai. Aap bataiye, aap kaise hain?"
                    }
                    messageLower.contains("help") || messageLower.contains("help") || messageLower.contains("madad") -> {
                        "Main aapke liye code likh sakta hoon, hindi mein kahaniyan suna sakta hoon, ya bas ek sachhe dost ki tarah baatein kar sakta hoon. Kahiye, kya pasand karenge aap?"
                    }
                    else -> {
                        "Aapka ye vichar bohot hi achha hai! Mujhe aapse baatein karke bohot kuch seekhne ko milta hai. Kuch aur poochhna chahte hain ya thoda aur baatein karein?"
                    }
                }
            }
        }
    }
}
