package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GoalEntity
import com.example.data.GoalSubtask
import com.example.viewmodel.LifeOsViewModel

@Composable
fun GoalScreen(
    viewModel: LifeOsViewModel,
    modifier: Modifier = Modifier
) {
    val goals by viewModel.goals.collectAsState()
    val isDecomposing by viewModel.isDecomposing.collectAsState()
    val aiError by viewModel.aiDecompositionError.collectAsState()

    var goalInput by remember { mutableStateOf("") }
    var descInput by remember { mutableStateOf("") }
    var targetDate by remember { mutableStateOf("2 Weeks") }

    var selectedGoalForLink by remember { mutableStateOf<Pair<GoalEntity, GoalSubtask>?>(null) }
    var showTimeBlockDialog by remember { mutableStateOf(false) }
    var timeBlockVal by remember { mutableStateOf("09:00 AM") }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("goals_screen"),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // --- Header Section ---
        item {
            Column {
                Text(
                    text = "Goal Decomposition",
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "AI-Driven Chronological Project Planner",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // --- AI Decomposition Input Card ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "🚀 Ask Gemini AI to Decompose Deconstruct",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )

                    OutlinedTextField(
                        value = goalInput,
                        onValueChange = { goalInput = it },
                        label = { Text("What is your goal/project?") },
                        placeholder = { Text("e.g. Plan a surprise birthday party or Learn Python") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ai_goal_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = descInput,
                        onValueChange = { descInput = it },
                        label = { Text("Brief context/wishes (optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = targetDate,
                            onValueChange = { targetDate = it },
                            label = { Text("Planned Timeline") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )

                        Button(
                            onClick = {
                                if (goalInput.isNotBlank()) {
                                    viewModel.decomposeGoalWithAi(goalInput, descInput, targetDate)
                                    goalInput = ""
                                    descInput = ""
                                }
                            },
                            enabled = !isDecomposing,
                            modifier = Modifier
                                .weight(1.2f)
                                .height(50.dp)
                                .testTag("ai_decompose_submit_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.Black
                            )
                        ) {
                            if (isDecomposing) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black)
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Analyze", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Optional Error Display
                    if (aiError != null) {
                        Text(
                            text = aiError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 11.sp,
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }

        // --- Active Goals List Section ---
        item {
            Text(
                text = "Active Decomposed Plans",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        }

        if (goals.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "No active plans compiled. Ask Gemini above or try presets like Party or Python!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(goals, key = { it.id }) { goal ->
                GoalPlanCard(
                    goal = goal,
                    onToggleSubtask = { subtaskId ->
                        viewModel.toggleGoalSubtask(goal, subtaskId)
                    },
                    onTimeBlockSubtask = { subtask ->
                        selectedGoalForLink = Pair(goal, subtask)
                        showTimeBlockDialog = true
                    },
                    onGroceryLinkSubtask = { subtask ->
                        viewModel.linkSubtaskToGroceryList(goal, subtask.id)
                    },
                    onDeleteGoal = {
                        viewModel.deleteGoal(goal.id)
                    }
                )
            }
        }
    }

    // --- Time Block Dialog Confirmation ---
    if (showTimeBlockDialog && selectedGoalForLink != null) {
        val (currentGoal, currentSubtask) = selectedGoalForLink!!

        AlertDialog(
            onDismissRequest = {
                showTimeBlockDialog = false
                selectedGoalForLink = null
            },
            title = { Text("Add Time Block to Calendar", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "This will schedule \"${currentSubtask.text}\" to today's dashboard checklist.",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    OutlinedTextField(
                        value = timeBlockVal,
                        onValueChange = { timeBlockVal = it },
                        label = { Text("Execution Time Block") },
                        modifier = Modifier.fillMaxWidth().testTag("time_block_time_input"),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (timeBlockVal.isNotBlank()) {
                            viewModel.linkSubtaskToCalendarBlock(
                                currentGoal,
                                currentSubtask.id,
                                timeBlockVal
                            )
                            showTimeBlockDialog = false
                            selectedGoalForLink = null
                        }
                    },
                    modifier = Modifier.testTag("time_block_confirm_submit")
                ) {
                    Text("Schedule Task")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showTimeBlockDialog = false
                        selectedGoalForLink = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun GoalPlanCard(
    goal: GoalEntity,
    onToggleSubtask: (String) -> Unit,
    onTimeBlockSubtask: (GoalSubtask) -> Unit,
    onGroceryLinkSubtask: (GoalSubtask) -> Unit,
    onDeleteGoal: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("goal_card_${goal.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = goal.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Deadline Target: ${goal.targetDate} • Description: ${goal.description}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = onDeleteGoal) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete project plan",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Subtasks grouped by milestone relativeTimeline
            val grouped = goal.subtasks.groupBy { it.relativeTimeline }

            if (grouped.isEmpty()) {
                Text(
                    "No subtasks listed. Try asking Gemini for interactive decomposition!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    grouped.forEach { (timeline, list) ->
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = timeline,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                                        RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )

                            list.forEach { subtask ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = subtask.isCompleted,
                                        onCheckedChange = { onToggleSubtask(subtask.id) }
                                    )

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = subtask.text,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            textDecoration = if (subtask.isCompleted) TextDecoration.LineThrough else null,
                                            color = if (subtask.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    // Integration Link triggers if not already scheduled
                                    if (!subtask.isCompleted && !subtask.linkedTaskCreated) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            // 1. Time Block to dashboard calendar
                                            IconButton(
                                                onClick = { onTimeBlockSubtask(subtask) },
                                                colors = IconButtonDefaults.iconButtonColors(
                                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                                    contentColor = MaterialTheme.colorScheme.primary
                                                ),
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .testTag("action_timeblock_${subtask.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.PlayArrow, // Calendar placeholder style icon
                                                    contentDescription = "Block to Calendar",
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }

                                            // 2. Link to grocery if context fits shopping / purchasing
                                            val triggersInSubtask = subtask.text.contains("buy", ignoreCase = true) ||
                                                    subtask.text.contains("get", ignoreCase = true) ||
                                                    subtask.text.contains("purchase", ignoreCase = true) ||
                                                    subtask.text.contains("shop", ignoreCase = true)

                                            if (triggersInSubtask) {
                                                IconButton(
                                                    onClick = { onGroceryLinkSubtask(subtask) },
                                                    colors = IconButtonDefaults.iconButtonColors(
                                                        containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                                                        contentColor = MaterialTheme.colorScheme.tertiary
                                                    ),
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .testTag("action_grocery_${subtask.id}")
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Add, // Shopping item placeholder
                                                        contentDescription = "Link to Grocery List",
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                        }
                                    } else if (subtask.linkedTaskCreated) {
                                        Text(
                                            text = "Linked",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 4.dp)
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
