package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ExerciseEntity
import com.example.data.WorkoutEntity
import com.example.viewmodel.LifeOsViewModel
import com.example.BuildConfig
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.ui.platform.LocalContext
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.Manifest
import android.view.Surface
import android.view.TextureView
import android.graphics.SurfaceTexture
import android.os.Bundle
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut

@Composable
fun FitnessScreen(
    viewModel: LifeOsViewModel,
    modifier: Modifier = Modifier
) {
    val workouts by viewModel.workouts.collectAsState()
    val selectedWorkoutId by viewModel.selectedWorkoutId.collectAsState()
    val exercises by viewModel.activeExercises.collectAsState()

    var showAddWorkoutDialog by remember { mutableStateOf(false) }
    var showAddExerciseDialog by remember { mutableStateOf(false) }

    val uriHandler = LocalUriHandler.current

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("fitness_screen"),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Header Section ---
        item {
            Column {
                Text(
                    text = "Fitness Command",
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Progressive Overload & Routine Builder",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // --- On-Device PPG Biosensor ---
        item {
            OnDevicePPGBiosensorComponent(viewModel = viewModel)
        }

        // --- Active Session Tracker ---
        item {
            LiveActivityTrackerComponent(viewModel = viewModel)
        }

        // --- Routine Library Header ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Routines Library",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )

                Button(
                    onClick = { showAddWorkoutDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier.testTag("add_workout_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        // --- Routine Cards List ---
        if (workouts.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No workout templates created. Start by adding one!", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(workouts, key = { it.id }) { workout ->
                val isSelected = selectedWorkoutId == workout.id
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (isSelected) viewModel.selectWorkout(null) else viewModel.selectWorkout(workout.id)
                        }
                        .testTag("workout_routine_${workout.id}"),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = workout.name,
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Estimated: ${workout.durationMinutes} mins • Burn: ~${workout.caloriesBurned} kcal",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { viewModel.deleteWorkout(workout.id) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete routine",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Icon(
                                    imageVector = if (isSelected) Icons.Default.PlayArrow else Icons.Default.PlayArrow, // Rotate or replace helper
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // --- Animated Exercise sublist for active selection ---
                        AnimatedVisibility(
                            visible = isSelected,
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Exercises Logged",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )

                                    TextButton(
                                        onClick = { showAddExerciseDialog = true },
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Add Exercise", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                if (exercises.isEmpty()) {
                                    Text(
                                        "No exercises added to this layout yet.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        exercises.forEach { exercise ->
                                            ExerciseRowItem(
                                                exercise = exercise,
                                                onToggle = { viewModel.toggleExerciseCompletion(exercise) },
                                                onApplyProgression = { viewModel.applyWeightProgression(exercise) },
                                                onDelete = { viewModel.deleteExercise(exercise.id) },
                                                onWatchTutorial = {
                                                    exercise.videoTutorialUrl?.let { url ->
                                                        uriHandler.openUri(url)
                                                    }
                                                }
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

        // --- Spotify Workout companion ---
        item {
            SpotifyCompanionView()
        }
    }

    // --- Dialogs ---
    if (showAddWorkoutDialog) {
        var name by remember { mutableStateOf("") }
        var duration by remember { mutableStateOf("45") }
        var calories by remember { mutableStateOf("300") }

        AlertDialog(
            onDismissRequest = { showAddWorkoutDialog = false },
            title = { Text("Build Custom Routine", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Routine Name (e.g. Push Day)") },
                        modifier = Modifier.fillMaxWidth().testTag("workout_name_input"),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = duration,
                        onValueChange = { duration = it },
                        label = { Text("Estimated Duration (minutes)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = calories,
                        onValueChange = { calories = it },
                        label = { Text("Estimated Calorie Burn (kcal)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            viewModel.addWorkout(
                                name = name,
                                duration = duration.toIntOrNull() ?: 45,
                                calories = calories.toIntOrNull() ?: 300
                            )
                            showAddWorkoutDialog = false
                        }
                    },
                    modifier = Modifier.testTag("submit_workout_button")
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddWorkoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAddExerciseDialog) {
        var exName by remember { mutableStateOf("") }
        var weight by remember { mutableStateOf("") }
        var reps by remember { mutableStateOf("10") }
        var sets by remember { mutableStateOf("3") }

        AlertDialog(
            onDismissRequest = { showAddExerciseDialog = false },
            title = { Text("Add Exercise to Routine", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = exName,
                        onValueChange = { exName = it },
                        label = { Text("Exercise Name (e.g. Bench Press)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { weight = it },
                        label = { Text("Initial Work Weight (lbs)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = reps,
                            onValueChange = { reps = it },
                            label = { Text("Reps") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = sets,
                            onValueChange = { sets = it },
                            label = { Text("Sets") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val activeId = selectedWorkoutId
                        if (activeId != null && exName.isNotBlank()) {
                            viewModel.addExerciseToWorkout(
                                workoutId = activeId,
                                name = exName,
                                weight = weight.toFloatOrNull() ?: 0f,
                                reps = reps.toIntOrNull() ?: 10,
                                sets = sets.toIntOrNull() ?: 3
                            )
                            showAddExerciseDialog = false
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddExerciseDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ExerciseRowItem(
    exercise: ExerciseEntity,
    onToggle: () -> Unit,
    onApplyProgression: () -> Unit,
    onDelete: () -> Unit,
    onWatchTutorial: () -> Unit
) {
    var isDone by remember(exercise.isCompleted) { mutableStateOf(exercise.isCompleted) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = isDone,
                    onCheckedChange = {
                        isDone = it
                        onToggle()
                    },
                    modifier = Modifier.testTag("exercise_chk_${exercise.id}")
                )

                Spacer(modifier = Modifier.width(6.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exercise.name,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "${exercise.currentSets} sets x ${exercise.currentReps} reps @ ${exercise.currentWeightLbs} lbs",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (exercise.videoTutorialUrl != null) {
                        IconButton(onClick = onWatchTutorial) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Watch Instruction Tutorial",
                                tint = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }

                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove exercise",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // --- PROGRESSIVE OVERLOAD POP-UP ALERT ---
            // If the user completes the exercise, dynamically scan previous weight and suggest upgrade!
            if (isDone) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "💪 Routine Progression Alert!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "You smashed this lift! Progression suggests upgrading next session to ${exercise.currentWeightLbs + 5f} lbs (+5 lbs).",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 14.sp
                        )
                    }

                    Button(
                        onClick = onApplyProgression,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.Black
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier
                            .height(28.dp)
                            .testTag("apply_progression_${exercise.id}")
                    ) {
                        Text("Apply +5lb", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun SpotifyCompanionView() {
    var selectedPlaylistId by remember { mutableStateOf("37i9dQZF1DX76t63uV6Yp1") } // Beast Mode default
    val uriHandler = LocalUriHandler.current

    val playlists = listOf(
        Triple("💪 Beast Mode", "37i9dQZF1DX76t63uV6Yp1", "High intensity tracks"),
        Triple("🏃 Cardio Pump", "37i9dQZF1DX7gIo6mEs793", "Upbeat rhythmic power"),
        Triple("⚡ Power Lift", "37i9dQZF1DX2sQHbtJy9vj", "Heavy drops and bass"),
        Triple("🧘 Zen Yoga", "37i9dQZF1DX9u7uIF06u7m", "Calming stretching focus")
    )

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("spotify_companion_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF1DB954), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Spotify Play",
                            tint = Color.Black,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Spotify Workout Station",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Play original tracks to level up your workout",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Playlist Selection Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                playlists.forEach { (label, id, _) ->
                    val isSelected = selectedPlaylistId == id
                    OutlinedButton(
                        onClick = { selectedPlaylistId = id },
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp, vertical = 2.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (isSelected) Color(0xFF1DB954).copy(alpha = 0.12f) else Color.Transparent,
                            contentColor = if (isSelected) Color(0xFF1DB954) else MaterialTheme.colorScheme.onSurface
                        ),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) Color(0xFF1DB954) else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        )
                    ) {
                        Text(
                            text = label,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Embedded Web Player WebView Panel
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color.Black
            ) {
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            webViewClient = WebViewClient()
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            setBackgroundColor(0)
                        }
                    },
                    update = { webView ->
                        val embedUrl = "https://open.spotify.com/embed/playlist/$selectedPlaylistId?utm_source=generator&theme=0"
                        webView.loadUrl(embedUrl)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action: deep-link launch Spotify App
            Button(
                onClick = {
                    val appUri = "spotify:playlist:$selectedPlaylistId"
                    try {
                        uriHandler.openUri(appUri)
                    } catch (e: Exception) {
                        uriHandler.openUri("https://open.spotify.com/playlist/$selectedPlaylistId")
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF1DB954),
                    contentColor = Color.Black
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Launch Full Spotify Player",
                    fontWeight = FontWeight.Black,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun LiveActivityTrackerComponent(
    viewModel: LifeOsViewModel
) {
    val recordedActivities by viewModel.recordedActivities.collectAsState()
    
    var selectedType by remember { mutableStateOf("Cardio") }
    var durationValue by remember { mutableStateOf(30) }
    var selectedIntensity by remember { mutableStateOf("Medium") }
    
    val typesList = listOf(
        Pair("Strength", "🏋️"),
        Pair("Cardio", "🏃"),
        Pair("Yoga", "🧘"),
        Pair("HIIT", "⚡"),
        Pair("Pilates", "🤸"),
        Pair("Walking", "🚶")
    )
    
    val intensitiesList = listOf("Low", "Medium", "High")
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("live_activity_tracker_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Activity Tracker Icon",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Active Session Tracker",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Record custom exercises to local state",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Exercise Type Selector Grid
            Text(
                text = "Select Exercise Type",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                typesList.take(3).forEach { (type, emoji) ->
                    val isSelected = selectedType == type
                    CustomFilterChip(
                        selected = isSelected,
                        onClick = { selectedType = type },
                        label = type,
                        emoji = emoji,
                        modifier = Modifier.weight(1f).height(40.dp).testTag("chip_type_$type")
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                typesList.drop(3).forEach { (type, emoji) ->
                    val isSelected = selectedType == type
                    CustomFilterChip(
                        selected = isSelected,
                        onClick = { selectedType = type },
                        label = type,
                        emoji = emoji,
                        modifier = Modifier.weight(1f).height(40.dp).testTag("chip_type_$type")
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Duration Selector with Quick Buttons and Live Indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Duration",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "$durationValue mins",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = { if (durationValue > 5) durationValue -= 5 },
                    modifier = Modifier.testTag("btn_duration_minus")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Decrease duration"
                    )
                }
                
                Slider(
                    value = durationValue.toFloat(),
                    onValueChange = { durationValue = it.toInt() },
                    valueRange = 5f..120f,
                    steps = 22,
                    modifier = Modifier.weight(1f).testTag("duration_slider")
                )
                
                IconButton(
                    onClick = { if (durationValue < 120) durationValue += 5 },
                    modifier = Modifier.testTag("btn_duration_plus")
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Increase duration"
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Intensity Level Selection Chips (Low, Medium, High)
            Text(
                text = "Intensity Level",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                intensitiesList.forEach { intensity ->
                    val isSelected = selectedIntensity == intensity
                    val (color, emoji) = when (intensity) {
                        "Low" -> MaterialTheme.colorScheme.secondary to "🟢"
                        "Medium" -> Color(0xFFF2994A) to "🟡"
                        else -> MaterialTheme.colorScheme.error to "🔴"
                    }
                    val bgAlpha = if (isSelected) 0.16f else 0.04f
                    val borderAlpha = if (isSelected) 0.6f else 0.2f
                    
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(42.dp)
                            .background(color.copy(alpha = bgAlpha), RoundedCornerShape(10.dp))
                            .clickable { selectedIntensity = intensity }
                            .border(
                                width = 1.dp,
                                color = color.copy(alpha = borderAlpha),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 8.dp)
                            .testTag("chip_intensity_$intensity"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(text = emoji, fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = intensity,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) color else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Spacer(modifier = Modifier.height(16.dp))

            // Dynamic Route Trail Google Map for Walkers / Runners
            AnimatedVisibility(
                visible = selectedType == "Walking" || selectedType == "Cardio",
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    GoogleMapRouteTrackerComponent(
                        viewModel = viewModel,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
            }

            // 4. Save Session Button
            Button(
                onClick = {
                    viewModel.recordActivity(selectedType, durationValue, selectedIntensity)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("btn_save_activity"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Record $selectedType Session", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }

            // 5. Saved Activities list (with totals/aggregates!)
            if (recordedActivities.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(14.dp))
                
                val totalMinutes = recordedActivities.sumOf { it.durationMinutes }
                val sessionsCount = recordedActivities.size
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Logged Sessions ($sessionsCount)",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Total Active: $totalMinutes mins",
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    recordedActivities.forEach { log ->
                        val emoji = typesList.find { it.first == log.type }?.second ?: "🏃"
                        val intensityColor = when (log.intensity) {
                            "Low" -> MaterialTheme.colorScheme.secondary
                            "Medium" -> Color(0xFFF2994A)
                            else -> MaterialTheme.colorScheme.error
                        }
                        
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("recorded_log_${log.id}"),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = emoji, fontSize = 18.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = log.type,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = "${log.durationMinutes} mins",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Box(
                                            modifier = Modifier
                                                .background(intensityColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = log.intensity,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = intensityColor
                                            )
                                        }
                                    }
                                }
                            }
                            
                            IconButton(
                                onClick = { viewModel.deleteRecordedActivity(log.id) },
                                modifier = Modifier.size(32.dp).testTag("delete_recorded_${log.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete tracked log",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    emoji: String,
    modifier: Modifier = Modifier
) {
    val containerColor = if (selected) MaterialTheme.copyColor(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)) else MaterialTheme.copyColor(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
    val contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    val borderStroke = if (selected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    
    Box(
        modifier = modifier
            .background(containerColor, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .border(borderStroke, RoundedCornerShape(10.dp))
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(emoji, fontSize = 12.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor,
                maxLines = 1
            )
        }
    }
}

private fun MaterialTheme.copyColor(color: Color): Color = color

@Composable
fun OnDevicePPGBiosensorComponent(
    viewModel: LifeOsViewModel
) {
    val ppgSensor = viewModel.ppgSensor
    val isScanning by ppgSensor.isScanning.collectAsState()
    val isFingerDetected by ppgSensor.isFingerDetected.collectAsState()
    val mockFingerOverride by ppgSensor.mockFingerOverride.collectAsState()
    val livePulseWave by ppgSensor.livePulseWave.collectAsState()
    val liveBpm by ppgSensor.liveBpm.collectAsState()
    val liveSpO2 by ppgSensor.liveSpO2.collectAsState()
    val liveRespiration by ppgSensor.liveRespiration.collectAsState()
    val isUsingSimulation by ppgSensor.isUsingSimulation.collectAsState()

    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) {
            ppgSensor.start(forceSimulation = false)
        } else {
            ppgSensor.start(forceSimulation = true)
        }
    }

    val wavePoints = remember { mutableStateListOf<Float>() }

    LaunchedEffect(livePulseWave, isScanning) {
        if (isScanning) {
            wavePoints.add(livePulseWave)
            if (wavePoints.size > 100) {
                wavePoints.removeAt(0)
            }
        } else {
            wavePoints.clear()
        }
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("on_device_ppg_biosensor_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "PPG Sensor Icon",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Phone Vital Scan (Option B)",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Real-time fingertip camera PPG scanner",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Simulation indicator chip
                if (isScanning && isUsingSimulation) {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                            .border(0.5.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Simulation Mode",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Body description
            Text(
                text = "Hold your index finger FIRMLY over the primary back camera and make sure it covers the flashlight when you trigger the scan. The biosensor analyzes capillary blood reflection cycles.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Pulse wave canvas (glowing ECG)
            if (isScanning) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isFingerDetected) "🔴 CAPTURING ARTERIAL PULSE" else "⚠️ WAITING FOR FINGER ON LENS...",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = if (isFingerDetected) Color(0xFF1DB954) else MaterialTheme.colorScheme.error
                            )
                            if (!isFingerDetected && !isUsingSimulation) {
                                Text(
                                    text = "Finger covering required for biometrics. Tap the button to bypass/simulate.",
                                    fontSize = 8.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    lineHeight = 10.sp
                                )
                            }
                        }
                        
                        if (!isUsingSimulation) {
                            Box(
                                modifier = Modifier
                                    .clickable { ppgSensor.setMockFingerOverride(!mockFingerOverride) }
                                    .background(
                                        color = if (mockFingerOverride) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (mockFingerOverride) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                        else Color.Transparent,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (mockFingerOverride) Icons.Default.CheckCircle else Icons.Default.Info,
                                        contentDescription = null,
                                        tint = if (mockFingerOverride) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(11.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (mockFingerOverride) "Simulated" else "Simulate",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (mockFingerOverride) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else if (isFingerDetected) {
                            Text(
                                  text = "Signal: Nominal (PPG)",
                                  fontSize = 10.sp,
                                  color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Left side: Camera Viewfinder Display Card
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                            colors = CardDefaults.cardColors(containerColor = Color.Black)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                if (isUsingSimulation) {
                                    // High-fidelity pulsing scanning telemetry animation
                                    val infiniteTransition = rememberInfiniteTransition(label = "sim_pulse")
                                    val scanScale by infiniteTransition.animateFloat(
                                        initialValue = 0.8f,
                                        targetValue = 1.2f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(1200, easing = FastOutSlowInEasing),
                                            repeatMode = RepeatMode.Reverse
                                        ),
                                        label = "scan_scale"
                                    )
                                    val scanAlpha by infiniteTransition.animateFloat(
                                        initialValue = 0.3f,
                                        targetValue = 0.8f,
                                        animationSpec = infiniteRepeatable(
                                            animation = tween(1200, easing = FastOutSlowInEasing),
                                            repeatMode = RepeatMode.Reverse
                                        ),
                                        label = "scan_alpha"
                                    )
                                    
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val w = size.width
                                        val h = size.height
                                        val cx = w / 2f
                                        val cy = h / 2f
                                        
                                        // Draw digital scanner background radar/rings
                                        drawCircle(
                                            color = Color(0xFFFF4D4D).copy(alpha = 0.05f),
                                            radius = cx * 0.9f
                                        )
                                        drawCircle(
                                            color = Color(0xFFFF4D4D).copy(alpha = 0.1f * scanAlpha),
                                            radius = cx * 0.6f * scanScale,
                                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
                                        )
                                        drawCircle(
                                            color = Color(0xFFFF4D4D).copy(alpha = 0.2f),
                                            radius = 8.dp.toPx(),
                                            style = androidx.compose.ui.graphics.drawscope.Fill
                                        )
                                        
                                        // Crosshair reticle coordinates
                                        drawLine(
                                            color = Color(0xFFFF4D4D).copy(alpha = 0.15f),
                                            start = androidx.compose.ui.geometry.Offset(0f, cy),
                                            end = androidx.compose.ui.geometry.Offset(w, cy),
                                            strokeWidth = 1f
                                        )
                                        drawLine(
                                            color = Color(0xFFFF4D4D).copy(alpha = 0.15f),
                                            start = androidx.compose.ui.geometry.Offset(cx, 0f),
                                            end = androidx.compose.ui.geometry.Offset(cx, h),
                                            strokeWidth = 1f
                                        )
                                    }
                                } else {
                                    // Real physical Back-Camera view rendering layer
                                    AndroidView(
                                        factory = { ctx ->
                                            TextureView(ctx).apply {
                                                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                                                    override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) {
                                                        ppgSensor.previewSurface = Surface(st)
                                                    }
                                                    override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) {}
                                                    override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                                                        ppgSensor.previewSurface = null
                                                        return true
                                                    }
                                                    override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    
                                    // Add camera red thermal medical color filter overlay to represent infrared raw sensor processing
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color(0xFFE50914).copy(alpha = 0.12f))
                                    )
                                }
                                
                                // Medical reticle overlays bounding the viewfinder
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(6.dp)
                                ) {
                                    // Blinking active red recording indicator dot
                                    Row(
                                        modifier = Modifier.align(Alignment.TopStart),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val animDotAlpha by rememberInfiniteTransition(label = "blink_dot").animateFloat(
                                            initialValue = 0.2f,
                                            targetValue = 1.0f,
                                            animationSpec = infiniteRepeatable(
                                                animation = tween(600, easing = LinearEasing),
                                                repeatMode = RepeatMode.Reverse
                                            ),
                                            label = "pulse_opacity"
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .background(Color.Red.copy(alpha = animDotAlpha), CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "VITAL CAM",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Red,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                    
                                    // Overlay text showing current state ("Finger covering reader")
                                    Text(
                                        text = if (isFingerDetected) "FINGER LOCKED" else "REPOSITION FINGER",
                                        fontSize = 7.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isFingerDetected) Color(0xFF1DB954) else Color(0xFFF2994A),
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        // Right side: Realtime ECG Photoplethysmography wave graph
                        Card(
                            modifier = Modifier
                                .weight(1.3f)
                                .fillMaxHeight(),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                            colors = CardDefaults.cardColors(containerColor = Color.Black)
                        ) {
                            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 6.dp)) {
                                Text(
                                    text = "HEMODYNAMIC WAVE (AC)",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isFingerDetected) Color(0xFF1DB954) else Color(0xFFF2994A).copy(alpha = 0.8f),
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.padding(bottom = 2.dp)
                                )
                                
                                Canvas(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .background(Color.Black)
                                ) {
                                    val width = size.width
                                    val height = size.height
                                    val centerY = height / 2f
                                    val stepX = if (wavePoints.size > 1) width / (wavePoints.size - 1) else width

                                    // Draw standard green cardiogram background grid values
                                    val gridCount = 6
                                    for (i in 1 until gridCount) {
                                        val lineX = i * (width / gridCount)
                                        drawLine(
                                            color = Color.Green.copy(alpha = 0.05f),
                                            start = androidx.compose.ui.geometry.Offset(lineX, 0f),
                                            end = androidx.compose.ui.geometry.Offset(lineX, height),
                                            strokeWidth = 1f
                                        )
                                    }
                                    val gridRows = 4
                                    for (i in 1 until gridRows) {
                                        val lineY = i * (height / gridRows)
                                        drawLine(
                                            color = Color.Green.copy(alpha = 0.05f),
                                            start = androidx.compose.ui.geometry.Offset(0f, lineY),
                                            end = androidx.compose.ui.geometry.Offset(width, lineY),
                                            strokeWidth = 1f
                                        )
                                    }

                                    // Draw pulse trace path
                                    val path = androidx.compose.ui.graphics.Path()
                                    if (wavePoints.isNotEmpty()) {
                                        for (idx in wavePoints.indices) {
                                            val x = idx * stepX
                                            val y = centerY - (wavePoints[idx] * (height / 2.8f))
                                            if (idx == 0) {
                                                path.moveTo(x, y)
                                            } else {
                                                path.lineTo(x, y)
                                            }
                                        }
                                        drawPath(
                                            path = path,
                                            color = if (isFingerDetected) Color(0xFF1DB954) else Color(0xFFF2994A),
                                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                                width = 2.dp.toPx(),
                                                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                                join = androidx.compose.ui.graphics.StrokeJoin.Round
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // Interactive trigger/onboarding panel
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(85.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f), RoundedCornerShape(10.dp))
                        .clickable {
                            if (hasCameraPermission) {
                                ppgSensor.start(forceSimulation = false)
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Begin PPG Biometrics Reading",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Key Biometrics parameters grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Parameter 1: Heart Rate
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Favorite, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Heart Rate", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (!isScanning) "--" else if (liveBpm > 0) "$liveBpm BPM" else "Scanning...",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Parameter 2: Oxygen Saturation (SpO2)
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF2F80ED), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("SpO2", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (!isScanning) "--" else if (liveSpO2 > 0) "$liveSpO2%" else "Scanning...",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Parameter 3: Breathing rate
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = Color(0xFF27AE60), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Respiration", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (!isScanning) "--" else if (liveRespiration > 0) "$liveRespiration/m" else "Scanning...",
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Controls Toolbar
            if (isScanning) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // STOP button
                    Button(
                        onClick = { ppgSensor.stop() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Stop", fontWeight = FontWeight.Bold)
                    }

                    // Log Reading Button
                    Button(
                        onClick = {
                            if (liveBpm > 0) {
                                viewModel.commitBiometricReading(liveBpm, liveSpO2, liveRespiration)
                            }
                        },
                        enabled = liveBpm > 0,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Log Scanned Vitals", fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

// --- 🌐 Walk, Jog & Run Google Map Tracking Suite ---

fun calculateTotalDistance(points: List<Pair<Double, Double>>): Double {
    if (points.size < 2) return 0.0
    var total = 0.0
    for (i in 0 until points.size - 1) {
        val p1 = points[i]
        val p2 = points[i + 1]
        
        // Haversine formula
        val r = 6371.0 // Earth's radius in km
        val dLat = Math.toRadians(p2.first - p1.first)
        val dLng = Math.toRadians(p2.second - p1.second)
        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(p1.first)) * Math.cos(Math.toRadians(p2.first)) *
                Math.sin(dLng / 2) * Math.sin(dLng / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        total += r * c
    }
    return total
}

@Composable
fun NeonRouteCanvas(
    routePoints: List<Pair<Double, Double>>,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        
        // 1. Draw cybernetic coordinate grid background
        val gridSpacing = 40.dp.toPx()
        for (x in 0.. (width / gridSpacing).toInt()) {
            drawLine(
                color = Color(0xFF6200EE).copy(alpha = 0.08f),
                start = androidx.compose.ui.geometry.Offset(x * gridSpacing, 0f),
                end = androidx.compose.ui.geometry.Offset(x * gridSpacing, height),
                strokeWidth = 1f
            )
        }
        for (y in 0.. (height / gridSpacing).toInt()) {
            drawLine(
                color = Color(0xFF6200EE).copy(alpha = 0.08f),
                start = androidx.compose.ui.geometry.Offset(0f, y * gridSpacing),
                end = androidx.compose.ui.geometry.Offset(width, y * gridSpacing),
                strokeWidth = 1f
            )
        }
        
        // 2. Draw compass targets
        drawCircle(
            color = Color(0xFF3F51B5).copy(alpha = 0.12f),
            radius = minOf(width, height) / 3.5f,
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
        )
        drawCircle(
            color = Color(0xFF3F51B5).copy(alpha = 0.05f),
            radius = minOf(width, height) / 2.2f,
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)
        )
        
        // Crosshairs
        drawLine(
            color = Color(0xFF3F51B5).copy(alpha = 0.15f),
            start = androidx.compose.ui.geometry.Offset(center.x - 40f, center.y),
            end = androidx.compose.ui.geometry.Offset(center.x + 40f, center.y),
            strokeWidth = 1.5f
        )
        drawLine(
            color = Color(0xFF3F51B5).copy(alpha = 0.15f),
            start = androidx.compose.ui.geometry.Offset(center.x, center.y - 40f),
            end = androidx.compose.ui.geometry.Offset(center.x, center.y + 40f),
            strokeWidth = 1.5f
        )
        
        if (routePoints.size > 1) {
            // Find coordinate boundaries for dynamic camera framing
            val lats = routePoints.map { it.first }
            val lngs = routePoints.map { it.second }
            val minLat = lats.minOrNull() ?: 0.0
            val maxLat = lats.maxOrNull() ?: 0.0
            val minLng = lngs.minOrNull() ?: 0.0
            val maxLng = lngs.maxOrNull() ?: 0.0
            
            val latRange = maxLat - minLat
            val lngRange = maxLng - minLng
            
            val margin = 32.dp.toPx()
            val scaleX = if (lngRange != 0.0) (width - margin * 2) / lngRange else 1.0
            val scaleY = if (latRange != 0.0) (height - margin * 2) / latRange else 1.0
            
            // Map coordinate space to pixel space
            val drawPoints = routePoints.map { pt ->
                val x = margin + ((pt.second - minLng) * scaleX).toFloat()
                val y = height - margin - ((pt.first - minLat) * scaleY).toFloat() // Flip latitude coordinates
                androidx.compose.ui.geometry.Offset(x, y)
            }
            
            // Draw path vector
            for (i in 0 until drawPoints.size - 1) {
                drawLine(
                    color = Color(0xFF00E676), // Bright glowing emerald trail
                    start = drawPoints[i],
                    end = drawPoints[i+1],
                    strokeWidth = 8f
                )
            }
            
            // Draw Start Node
            drawCircle(
                color = Color(0xFF2979FF), // Cool active blue for anchor
                radius = 8f,
                center = drawPoints.first()
            )
            drawCircle(
                color = Color(0xFF2979FF).copy(alpha = 0.3f),
                radius = 16f,
                center = drawPoints.first(),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
            )
            
            // Draw End Head Node (Current Position)
            drawCircle(
                color = Color(0xFFFF1744), // High-intensity racing red
                radius = 10f,
                center = drawPoints.last()
            )
            drawCircle(
                color = Color(0xFFFF1744).copy(alpha = 0.35f),
                radius = 20f,
                center = drawPoints.last(),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
            )
        } else if (routePoints.size == 1) {
            // Standby lock point
            drawCircle(
                color = Color(0xFFFF1744),
                radius = 10f,
                center = center
            )
            drawCircle(
                color = Color(0xFFFF1744).copy(alpha = 0.2f),
                radius = 24f,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
            )
        } else {
            // Awaiting Satellite telemetry lock
            drawCircle(
                color = Color(0xFFFF9100).copy(alpha = 0.2f),
                radius = 40f,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)
            )
        }
    }
}

@Composable
fun GoogleMapRouteTrackerComponent(
    viewModel: LifeOsViewModel,
    modifier: Modifier = Modifier
) {
    val liveLocation by viewModel.liveLocationState.collectAsState()
    val routePoints by viewModel.trackedRoutePoints.collectAsState()
    
    val context = LocalContext.current
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fine = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarse = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        hasLocationPermission = fine || coarse
        if (hasLocationPermission) {
            viewModel.startLocationTracking()
        }
    }

    // Toggle: 0 for Google Map, 1 for Neon Canvas Vector route
    var activeMapTypeTab by remember { mutableIntStateOf(0) }

    val isMapsApiKeyConfigured = remember {
        val key = BuildConfig.MAPS_API_KEY
        key.isNotEmpty() && 
        !key.contains("YOUR_") && 
        !key.contains("placeholder") && 
        key != "YOUR_MAPS_API_KEY_HERE"
    }

    // Setup MapView
    val mapView = remember { MapView(context) }
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle

    DisposableEffect(lifecycle, mapView) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_CREATE -> mapView.onCreate(Bundle())
                androidx.lifecycle.Lifecycle.Event.ON_START -> mapView.onStart()
                androidx.lifecycle.Lifecycle.Event.ON_RESUME -> mapView.onResume()
                androidx.lifecycle.Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> mapView.onStop()
                androidx.lifecycle.Lifecycle.Event.ON_DESTROY -> mapView.onDestroy()
                else -> {}
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
        }
    }

    // Polyline drawing logic whenever routePoints flow updates
    LaunchedEffect(routePoints, activeMapTypeTab) {
        if (activeMapTypeTab == 0 && isMapsApiKeyConfigured && routePoints.isNotEmpty()) {
            mapView.getMapAsync { googleMap ->
                googleMap.clear()
                
                // Set custom dark mode if desired, or keep default
                googleMap.uiSettings.isZoomControlsEnabled = true
                googleMap.uiSettings.isCompassEnabled = true
                
                val latLngPoints = routePoints.map { LatLng(it.first, it.second) }
                
                // Start Point Marker
                val start = latLngPoints.first()
                googleMap.addMarker(
                    MarkerOptions()
                        .position(start)
                        .title("Start Node")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
                )

                // Current Head Marker
                val current = latLngPoints.last()
                googleMap.addMarker(
                    MarkerOptions()
                        .position(current)
                        .title("Active Position")
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                )

                // Polylines Trail drawing
                googleMap.addPolyline(
                    PolylineOptions()
                        .addAll(latLngPoints)
                        .color(0xFF6200EE.toInt()) // Deep premium purple route line
                        .width(12f)
                        .geodesic(true)
                )

                // Camera follow
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(current, 16.5f))
            }
        }
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("google_map_tracking_card")
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header with status indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Place,
                        contentDescription = "Map Location PIN",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "GPS Route Trail Mapper",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Active telemetry pulse chip
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            color = if (liveLocation.hasLocation) Color(0xFF1B5E20).copy(alpha = 0.12f) else Color(0xFFB71C1C).copy(alpha = 0.12f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(
                                color = if (liveLocation.hasLocation) Color(0xFF2E7D32) else Color(0xFFD32F2F),
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (liveLocation.hasLocation) "GPS LOCKED" else "GPS INACTIVE",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (liveLocation.hasLocation) Color(0xFF81C784) else Color(0xFFE57373)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (!hasLocationPermission) {
                // Request Permission UI Screen Overlay inside the card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Location Authorization Needed",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Permit access to on-device GPS mapping engines to trace walk/jog telemetry routes live.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                locationPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.Black
                            )
                        ) {
                            Text("Grant Access 🛰️", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // Style selection tabs
                TabRow(
                    selectedTabIndex = activeMapTypeTab,
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[activeMapTypeTab]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                ) {
                    Tab(
                        selected = activeMapTypeTab == 0,
                        onClick = { activeMapTypeTab = 0 },
                        text = { Text("Satellite Trail Map", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = activeMapTypeTab == 1,
                        onClick = { activeMapTypeTab = 1 },
                        text = { Text("Cyber Neon Trail Canvas", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // The Map Screen area container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                ) {
                    if (activeMapTypeTab == 0) {
                        if (isMapsApiKeyConfigured) {
                            // Render standard Google Map MapView via AndroidView wrapper
                            AndroidView(
                                factory = { mapView },
                                modifier = Modifier.fillMaxSize().testTag("google_map_view")
                            )
                        } else {
                            // Friendly Setup Required overlay screen inside the card
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFF151421)), // Cyber dark background theme
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = "Info Icon",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "Maps API Key Setup Required",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color.White
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "To enable satellite tracking, add your MAPS_API_KEY inside the AI Studio Secrets panel. Switch below to trace your active route path instantly.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.LightGray,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Button(
                                        onClick = { activeMapTypeTab = 1 }, // Seamlessly toggle to Cyber Neon Canvas
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = Color.Black
                                        ),
                                        modifier = Modifier.height(34.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
                                    ) {
                                        Text("Use Offline Neon Canvas 🎨", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    } else {
                        // Render custom vectors on retro-themed athletic style canvas
                        NeonRouteCanvas(
                            routePoints = routePoints,
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF0D0B18))
                                .testTag("cyber_neon_canvas")
                        )
                    }
                    
                    // Standby coordinate marker details overlay
                    if (routePoints.isNotEmpty()) {
                        val currentLat = routePoints.last().first
                        val currentLng = routePoints.last().second
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp)
                                .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = String.format(java.util.Locale.US, "LAT: %.5f° | LNG: %.5f°", currentLat, currentLng),
                                color = Color.White,
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Control Switch buttons: Start / Stop tracking, Reset Trail
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (!liveLocation.hasLocation) {
                        Button(
                            onClick = { viewModel.startLocationTracking() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("btn_start_location_tracking")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Start GPS Tracker", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = { viewModel.stopLocationTracking() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .testTag("btn_stop_location_tracking")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Stop GPS Tracker", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = { viewModel.clearTrackedRoute() },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .testTag("btn_reset_trail")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Reset Trail Points", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Live Telemetry metrics grid (Distance, Speed, Recorded points)
                val totalDistanceKm = calculateTotalDistance(routePoints)
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("TOTAL DISTANCE", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(String.format(java.util.Locale.US, "%.3f km", totalDistanceKm), fontSize = 14.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("CURRENT SPEED", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(String.format(java.util.Locale.US, "%.1f km/h", liveLocation.speed), fontSize = 14.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.secondary)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text("TRAIL SENSORS", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("${routePoints.size} nodes", fontSize = 14.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.tertiary)
                    }
                }
            }
        }
    }
}





