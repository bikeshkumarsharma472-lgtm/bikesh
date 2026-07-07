package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.ui.VartaViewModel
import com.example.ui.components.VartaCallOverlay
import com.example.ui.components.VartaChatView
import com.example.ui.components.VartaDashboard
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val viewModel: VartaViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                var currentScreen by remember { mutableStateOf("DASHBOARD") }
                val callStatus by viewModel.callStatus.collectAsState()

                Box(modifier = Modifier.fillMaxSize()) {
                    // Screen selection
                    when (currentScreen) {
                        "DASHBOARD" -> {
                            VartaDashboard(
                                viewModel = viewModel,
                                onContactSelectedForChat = { contactId ->
                                    viewModel.selectContactForChat(contactId)
                                    currentScreen = "CHAT"
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        "CHAT" -> {
                            VartaChatView(
                                viewModel = viewModel,
                                onBack = {
                                    currentScreen = "DASHBOARD"
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    // Immersive Active Call Overlay
                    AnimatedVisibility(
                        visible = callStatus != "IDLE",
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        VartaCallOverlay(
                            viewModel = viewModel,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
            }
        }
    }
}

