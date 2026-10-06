package com.example.ui

import androidx.compose.animation.*
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ChatMessage
import com.example.viewmodel.LifeOsViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AiCoachScreen(
    viewModel: LifeOsViewModel,
    modifier: Modifier = Modifier
) {
    val chatHistory by viewModel.aiGuidanceHistory.collectAsState()
    val isLoading by viewModel.aiGuidanceLoading.collectAsState()

    var textInput by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val listState = rememberLazyListState()

    // Auto scroll to bottom when new messages arrive
    LaunchedEffect(chatHistory.size, isLoading) {
        if (chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(chatHistory.size)
        }
    }

    val presets = listOf(
        "💪 Squat Progression Advice" to "How do I progress my barbell squats safely using progressive overload?",
        "🍳 Recipe Idea" to "Give me a high protein healthy dinner recipe using sweet potatoes and salmon.",
        "🐍 Learn Python Outline" to "Create a step-by-step study schedule to learn Python basics in 3 weeks.",
        "🌅 Work Productivity Hack" to "Show me an elite science-backed morning routine to maximize focus."
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("ai_coach_screen")
    ) {
        // --- Header Command Panel ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "AI Coach Advisor",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Personalized fitness, food & productivity counsel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (chatHistory.isNotEmpty()) {
                IconButton(
                    onClick = { viewModel.clearGuidanceHistory() },
                    modifier = Modifier.testTag("clear_chat_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Clear Conversation Logs",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                }
            }
        }

        Divider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))

        // --- Core Content Area ---
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (chatHistory.isEmpty()) {
                // Welcome Screen & Presets
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Command Advisor Active",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Ask any question about physical health, nutritional scaling, agenda organization, or general workspace optimization.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = "Sample Guidance Queries:",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    presets.forEach { (label, query) ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    focusManager.clearFocus()
                                    viewModel.askAiGuidance(query)
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                            }
                        }
                    }
                }
            } else {
                // Interactive Chat Logs
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(chatHistory, key = { it.id }) { message ->
                        ChatBubbleRow(message = message)
                    }

                    if (isLoading) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.widthIn(max = 280.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "Consulting Gemini AI...",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // --- Active Chat Input Console ---
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .windowInsetsPadding(WindowInsets.ime),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    placeholder = { Text("Ask anything...") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("ai_chat_input"),
                    shape = RoundedCornerShape(24.dp),
                    maxLines = 4,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (textInput.isNotBlank() && !isLoading) {
                                focusManager.clearFocus()
                                viewModel.askAiGuidance(textInput)
                                textInput = ""
                            }
                        }
                    )
                )

                Spacer(modifier = Modifier.width(12.dp))

                FloatingActionButton(
                    onClick = {
                        if (textInput.isNotBlank() && !isLoading) {
                            focusManager.clearFocus()
                            viewModel.askAiGuidance(textInput)
                            textInput = ""
                        }
                    },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.Black,
                    shape = CircleShape,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("ai_send_chat_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send Guidance Query",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ChatBubbleRow(message: ChatMessage) {
    val layoutAlignment = if (message.isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (message.isUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val bubbleTextColor = if (message.isUser) {
        Color.Black
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    val bubbleShape = if (message.isUser) {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 2.dp)
    } else {
        RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 2.dp, bottomEnd = 16.dp)
    }

    val timeString = SimpleDateFormat("h:mm a", Locale.getDefault()).format(message.timestamp)

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = layoutAlignment
    ) {
        Column(
            horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = bubbleColor),
                shape = bubbleShape
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = bubbleTextColor
                    )
                }
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = timeString,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}
