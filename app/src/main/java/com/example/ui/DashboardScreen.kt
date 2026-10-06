package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import java.util.Locale
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MealEntity
import com.example.data.TaskEntity
import com.example.data.LifeOsSynergyState
import com.example.data.CalibrationState
import com.example.state.LocalLifeOsViewModel
import com.example.viewmodel.LifeOsViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.Manifest
import com.example.ui.theme.*

data class WaterBubble(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val radius: Float,
    val alpha: Float
)

@Composable
fun DashboardScreen(
    viewModel: LifeOsViewModel,
    modifier: Modifier = Modifier,
    onNavigateToFitness: () -> Unit,
    onNavigateToMeals: () -> Unit
) {
    val appThemeStr by viewModel.appThemeStr.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val workouts by viewModel.workouts.collectAsState()
    val meals by viewModel.meals.collectAsState()
    val waterLoggedMl by viewModel.waterLoggedMl.collectAsState()

    val userName by viewModel.userName.collectAsState()
    val userEmail by viewModel.userEmail.collectAsState()
    val liveLocation by viewModel.liveLocationState.collectAsState()
    val liveAccelerometer by viewModel.accelerometerState.collectAsState()
    val deviceDiagnostics by viewModel.deviceDiagnostics.collectAsState()

    val calibrationState by viewModel.calibrationState.collectAsState()
    val calibrationProgress by viewModel.calibrationProgress.collectAsState()
    val calibrationMessage by viewModel.calibrationMessage.collectAsState()
    val isAccelCalibrated by viewModel.isAccelCalibrated.collectAsState()

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
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        hasLocationPermission = fineGranted || coarseGranted
        if (hasLocationPermission) {
            viewModel.startLocationTracking()
        }
    }

    // Lifecycle-aware tracker to avoid background E/AppOps errors by requesting location ONLY when in foreground (resumed) state!
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, hasLocationPermission) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                if (hasLocationPermission) {
                    viewModel.startLocationTracking()
                }
            } else if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE) {
                viewModel.stopLocationTracking()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            viewModel.stopLocationTracking()
        }
    }

    // Automatically update diagnostics dynamically
    LaunchedEffect(Unit) {
        viewModel.refreshDeviceDiagnostics()
    }

    var showAddTaskDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var isTelemetryExpanded by remember { mutableStateOf(false) }

    // Calc total progress
    val totalActionItems = tasks.size + meals.size
    val completedActionItems = tasks.count { it.isCompleted } + meals.count { it.isCompleted }
    val progressFraction = if (totalActionItems > 0) completedActionItems.toFloat() / totalActionItems else 0f
    val animatedProgress by animateFloatAsState(targetValue = progressFraction, label = "DashboardProgress")

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // --- Warm Personalized Profile Header ---
        item {
            var isProfileEditingExpanded by remember { mutableStateOf(false) }
            val userHeightCm by viewModel.userHeightCm.collectAsState()
            val userWeightKg by viewModel.userWeightKg.collectAsState()
            val userAge by viewModel.userAge.collectAsState()
            val userGender by viewModel.userGender.collectAsState()
            val userActivityLevel by viewModel.userActivityLevel.collectAsState()

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                ),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                modifier = Modifier.fillMaxWidth().testTag("user_profile_header_card")
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .padding(top = 16.dp, start = 16.dp, end = 16.dp, bottom = 8.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Initials Avatar Gradient
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary,
                                                MaterialTheme.colorScheme.secondary
                                            )
                                        ),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = userName.take(1).uppercase(),
                                    fontWeight = FontWeight.Black,
                                    fontSize = 18.sp,
                                    color = Color.Black
                                )
                            }
                            
                            Spacer(modifier = Modifier.width(14.dp))
                            
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Hello, $userName!",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    // Verified Badge
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFF1DB954).copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Verified Profile",
                                                tint = Color(0xFF1DB954),
                                                modifier = Modifier.size(10.dp)
                                            )
                                            Text(
                                                text = "OAuth Profile",
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1DB954)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = userEmail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Edit Bio/Personalization Button
                            IconButton(
                                onClick = { isProfileEditingExpanded = !isProfileEditingExpanded },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                                        shape = CircleShape
                                    )
                                    .testTag("toggle_bio_edit_button")
                            ) {
                                Icon(
                                    imageVector = if (isProfileEditingExpanded) Icons.Default.Close else Icons.Default.Edit,
                                    contentDescription = "Personalization Settings",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Google account Sign Out icon button
                            IconButton(
                                onClick = { viewModel.signOutGoogle() },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                        shape = CircleShape
                                    )
                                    .testTag("google_logout_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ExitToApp,
                                    contentDescription = "Log Out from Google",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Quick Reset All Button
                            IconButton(
                                onClick = { showResetDialog = true },
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.10f),
                                        shape = CircleShape
                                    )
                                    .testTag("reset_data_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Clear All App Data",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // Biometrics mini-view on the profile card
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier
                            .padding(bottom = 16.dp, start = 16.dp, end = 16.dp)
                            .fillMaxWidth()
                    ) {
                        Text(
                            text = "👤 ${userGender.uppercase(Locale.US)}, $userAge yrs",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "📏 $userHeightCm cm",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "⚖️ $userWeightKg kg",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "⚡ $userActivityLevel",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    AnimatedVisibility(visible = isProfileEditingExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                            
                            Text(
                                text = "Biometric Personalization (Mifflin-St Jeor)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.secondary
                            )
                            
                            var heightInput by remember(userHeightCm) { mutableStateOf(userHeightCm.toString()) }
                            var weightInput by remember(userWeightKg) { mutableStateOf(userWeightKg.toString()) }
                            var ageInput by remember(userAge) { mutableStateOf(userAge.toString()) }
                            var genderVal by remember(userGender) { mutableStateOf(userGender) }
                            var activityVal by remember(userActivityLevel) { mutableStateOf(userActivityLevel) }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = heightInput,
                                    onValueChange = { heightInput = it.filter { char -> char.isDigit() || char == '.' } },
                                    label = { Text("Height (cm)", fontSize = 11.sp) },
                                    textStyle = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f).testTag("profile_height_field"),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = weightInput,
                                    onValueChange = { weightInput = it.filter { char -> char.isDigit() || char == '.' } },
                                    label = { Text("Weight (kg)", fontSize = 11.sp) },
                                    textStyle = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f).testTag("profile_weight_field"),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = ageInput,
                                    onValueChange = { ageInput = it.filter { char -> char.isDigit() } },
                                    label = { Text("Age", fontSize = 11.sp) },
                                    textStyle = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f).testTag("profile_age_field"),
                                    singleLine = true
                                )
                            }

                            // Gender Selection Row
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Gender:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    listOf("Male", "Female", "Other").forEach { g ->
                                        val selected = genderVal == g
                                        FilterChip(
                                            selected = selected,
                                            onClick = { genderVal = g },
                                            label = { Text(g, fontSize = 11.sp) },
                                            modifier = Modifier.weight(1f).testTag("gender_chip_$g")
                                        )
                                    }
                                }
                            }

                            // Activity Level Selection Row
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Weekly Activity Multiplier:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf("Sedentary", "Light", "Moderate", "Active").forEach { act ->
                                        val selected = activityVal == act
                                        FilterChip(
                                            selected = selected,
                                            onClick = { activityVal = act },
                                            label = { Text(act, fontSize = 10.sp) },
                                            modifier = Modifier.weight(1f).testTag("activity_chip_$act")
                                        )
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    val h = heightInput.toFloatOrNull() ?: 175f
                                    val w = weightInput.toFloatOrNull() ?: 70f
                                    val a = ageInput.toIntOrNull() ?: 28
                                    viewModel.updateUserProfile(h, w, a, genderVal, activityVal)
                                    isProfileEditingExpanded = false
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("save_profile_button"),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = Color.Black
                                )
                            ) {
                                Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save to Database & Calibrate", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Encrypted SQLite Room Database Active",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- Central Cross-Module State Sync Center (React Context style) ---
        item {
            val synergyState by viewModel.lifeOsSynergyState.collectAsState()
            
            val animatedTotalSynergyScore by animateFloatAsState(
                targetValue = synergyState.totalSynergyScore,
                animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                label = "total_synergy_anim"
            )
            val animatedFitnessScore by animateFloatAsState(
                targetValue = synergyState.fitnessScore,
                animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                label = "fitness_score_anim"
            )
            val animatedNutritionScore by animateFloatAsState(
                targetValue = synergyState.nutritionScore,
                animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                label = "nutrition_score_anim"
            )
            val animatedProductivityScore by animateFloatAsState(
                targetValue = synergyState.productivityScore,
                animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                label = "productivity_score_anim"
            )

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
                ),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("state_sync_center_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header with title and Sync badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Sync Central State",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Central Synergy Sync",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        // React Context API Badge (Compose equivalent)
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "STATE CONTEXT COMPAT",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Unified Synergy Score Row (Circular Progress + stats)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Score Circle
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(70.dp)
                        ) {
                            CircularProgressIndicator(
                                progress = { animatedTotalSynergyScore },
                                modifier = Modifier.fillMaxSize(),
                                strokeWidth = 6.dp,
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                            )
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = String.format(Locale.US, "%.0f%%", synergyState.totalSynergyScore * 100f),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Synergy",
                                    fontSize = 8.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        // Key Metrics showing real-time synchronized data cross-referencing
                        Column(modifier = Modifier.weight(1f)) {
                            // Dynamic Calorie Buffer (FITNESS -> NUTRITION SYNC)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.FavoriteBorder,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Calorie Budget (Synced)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text("${synergyState.calorieAdjustedBudget} kcal", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            
                            Spacer(modifier = Modifier.height(6.dp))
                            
                            // Dynamic Hydration Tracker (FITNESS -> NUTRITION SYNC)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.ShoppingCart,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Hydration Target (Synced)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Text(String.format(Locale.US, "%.2f L", synergyState.targetHydrationLiters), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(14.dp))
                    
                    // Individual module scores indicators (Fitness, Nutrition, Productivity)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Fitness progress info block
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Fitness Flow", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { animatedFitnessScore },
                                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(String.format(Locale.US, "%.0f%%", synergyState.fitnessScore * 100), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        // Nutrition progress info block
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.15f)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Nutrition Hub", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { animatedNutritionScore },
                                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                                    color = MaterialTheme.colorScheme.secondary,
                                    trackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(String.format(Locale.US, "%.0f%%", synergyState.nutritionScore * 100), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        
                        // Productivity progress info block
                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.15f)
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Productivity", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                                Spacer(modifier = Modifier.height(4.dp))
                                LinearProgressIndicator(
                                    progress = { animatedProductivityScore },
                                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                                    color = MaterialTheme.colorScheme.tertiary,
                                    trackColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(String.format(Locale.US, "%.0f%%", synergyState.productivityScore * 100), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Central Context Sync Alert Banner (Dynamic state feedback)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = synergyState.synergyAlertMessage,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 10.5.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        }

        // --- 📊 Health & Sleep Command Center ---
        item {
            val wearableData by viewModel.wearableData.collectAsState()
            val sleepModeActive by viewModel.sleepModeActive.collectAsState()
            
            // Pulse beat animation for heart rate
            val infiniteTransition = rememberInfiniteTransition(label = "pulse_heart_beat")
            val heartScale by infiniteTransition.animateFloat(
                initialValue = 1.0f,
                targetValue = 1.25f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "heart_scale"
            )

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (sleepModeActive) {
                        Color(0xFF141329) // Deep cosmic night slate
                    } else {
                        MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
                    }
                ),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(
                    width = 1.5.dp,
                    color = if (sleepModeActive) Color(0xFF635BFF).copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("health_sleep_center_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (sleepModeActive) "🌙 Sleep Mode Active" else "📊 Vitality & Sleep Command",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (sleepModeActive) Color(0xFFE2E2FF) else MaterialTheme.colorScheme.onSurface
                            )
                        }
                        
                        // Active/In Stock Status Chip
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (sleepModeActive) Color(0xFF2E2460) else Color(0xFF122C1A),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (sleepModeActive) "REST MODE" else "MONITORING",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (sleepModeActive) Color(0xFFBB99FF) else Color(0xFF6BFFA3)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (sleepModeActive) {
                        // --- Active Sleep Mode View (Cozy Starry Night Animation + Soundscape + Wake Button) ---
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Beautiful sleeping moon & stars canvas
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(100.dp)
                                    .padding(8.dp)
                            ) {
                                // Star rotation/twinkle effect
                                val skyTwinkle by infiniteTransition.animateFloat(
                                    initialValue = 0.3f,
                                    targetValue = 1.0f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(1200, easing = LinearEasing),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "sky_twinkle"
                                )
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    // Custom draw beautiful moon and glowing star circles
                                    drawCircle(
                                        color = Color(0xFFBB99FF).copy(alpha = 0.15f * skyTwinkle),
                                        radius = size.width / 2.2f
                                    )
                                }
                                Text(
                                    text = "😴",
                                    fontSize = 44.sp
                                )
                            }

                            Text(
                                text = "Do Not Disturb Is Active",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = Color(0xFFBB99FF)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Lowering screen distraction, logging restorative sleep. Let this cycle refresh your bio-vitality score.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFAAAAFF),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            // Interactive breath pacer to aid sleep
                            var breathState by remember { mutableStateOf("Breathe In") }
                            LaunchedEffect(Unit) {
                                while (true) {
                                    breathState = "Breathe In"
                                    kotlinx.coroutines.delay(4000)
                                    breathState = "Hold"
                                    kotlinx.coroutines.delay(2000)
                                    breathState = "Exhale"
                                    kotlinx.coroutines.delay(4000)
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF1E1A3D), RoundedCornerShape(12.dp))
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "🧘 Wind Down Breath:",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFBB99FF)
                                    )
                                    Text(
                                        text = breathState.uppercase(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFFFFFFF)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = { viewModel.toggleSleepMode() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFBB99FF),
                                    contentColor = Color(0xFF110C24)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("wake_up_back_button")
                                    .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            ) {
                                Text("Wake Up & Log Sleep ☀️", fontWeight = FontWeight.Black)
                            }
                        }
                    } else {
                        // --- Standard Mode View (Steps count, Calories, Sleep, Heart rate + button to sleep) ---
                        // Grid layout of 4 vital metric items using rounded child boxes
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Row 1: Steps & Calories
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Steps Card
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                                        .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
                                        .padding(12.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = "👣 Daily Steps",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = String.format(Locale.US, "%,d", wearableData.steps),
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        // 10K step daily goal progress
                                        val stepFraction = (wearableData.steps.toFloat() / 10000f).coerceIn(0f, 1f)
                                        LinearProgressIndicator(
                                            progress = { stepFraction },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(4.dp)
                                                .clip(CircleShape),
                                            color = MaterialTheme.colorScheme.primary,
                                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Button(
                                                onClick = { viewModel.addSteps(1000) },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                                    contentColor = MaterialTheme.colorScheme.primary
                                                ),
                                                contentPadding = PaddingValues(0.dp),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(24.dp)
                                                    .testTag("add_1k_steps_button")
                                            ) {
                                                Text("+1K Walk", fontSize = 8.sp, fontWeight = FontWeight.Black)
                                            }
                                        }
                                    }
                                }

                                // Calories Card
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                                        .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
                                        .padding(12.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = "🔥 Active Burn",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "${wearableData.activeCalories} kcal",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        // Daily calorie goal progress (standard: 500 active kcal)
                                        val calFraction = (wearableData.activeCalories.toFloat() / 500f).coerceIn(0f, 1f)
                                        LinearProgressIndicator(
                                            progress = { calFraction },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(4.dp)
                                                .clip(CircleShape),
                                            color = MaterialTheme.colorScheme.secondary,
                                            trackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Button(
                                                onClick = { viewModel.addSteps(2500) }, // This also logs cals!
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f),
                                                    contentColor = MaterialTheme.colorScheme.secondary
                                                ),
                                                contentPadding = PaddingValues(0.dp),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(24.dp)
                                                    .testTag("add_run_button")
                                            ) {
                                                Text("+100 kcal", fontSize = 8.sp, fontWeight = FontWeight.Black)
                                            }
                                        }
                                    }
                                }
                            }

                            // Row 2: Sleep & Heart Pulse BPM
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Sleep Tracker Card
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                                        .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
                                        .padding(12.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = "🌙 Sleep Tracker",
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = String.format(Locale.US, "%.1f hrs", wearableData.sleepHours),
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        // 8 hrs daily goal progress
                                        val sleepFraction = (wearableData.sleepHours / 8f).coerceIn(0f, 1f)
                                        LinearProgressIndicator(
                                            progress = { sleepFraction },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(4.dp)
                                                .clip(CircleShape),
                                            color = MaterialTheme.colorScheme.tertiary,
                                            trackColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Button(
                                                onClick = { viewModel.addHoursOfSleep(1.0f) },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                                                    contentColor = MaterialTheme.colorScheme.tertiary
                                                ),
                                                contentPadding = PaddingValues(0.dp),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(24.dp)
                                                    .testTag("log_1h_sleep_button")
                                            ) {
                                                Text("+1h Sleep", fontSize = 8.sp, fontWeight = FontWeight.Black)
                                            }
                                        }
                                    }
                                }

                                // BPM Card with pulsing animation!
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                                        .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), RoundedCornerShape(14.dp))
                                        .padding(12.dp)
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "❤️ Resting Pulse",
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.weight(1f)
                                            )
                                            // Pulser icon
                                            Box(
                                                modifier = Modifier
                                                    .size(12.dp)
                                                    .background(Color.Red.copy(alpha = 0.15f), CircleShape)
                                                    .padding(1.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(6.dp * heartScale)
                                                        .background(Color.Red, CircleShape)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "${wearableData.restingHeartRate} bpm",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (wearableData.restingHeartRate < 60) "Athletic Performance" else if (wearableData.restingHeartRate < 80) "Optimal Resting Rate" else "Elevated Heart Rate",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (wearableData.restingHeartRate < 60) Color(0xFF64B5F6) else if (wearableData.restingHeartRate < 80) Color(0xFF81C784) else Color(0xFFFFB74D)
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Button(
                                                onClick = { viewModel.logRestingBpm(72) },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                                                    contentColor = MaterialTheme.colorScheme.error
                                                ),
                                                contentPadding = PaddingValues(0.dp),
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(24.dp)
                                                    .testTag("add_bpm_stabilize_button")
                                            ) {
                                                Text("Stabilize BPM", fontSize = 8.sp, fontWeight = FontWeight.Black)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Button to enter sleep mode
                        Button(
                            onClick = { viewModel.toggleSleepMode() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = Color.Black
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("turn_on_sleep_mode_button")
                        ) {
                            Text("Turn On Sleep Mode 🌙", fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }

        // --- 🌊 Playful Liquid Gravity Water Splash Interactive Mini-Game ---
        item {
            val maxTarget = 3000

            // Keep list of active bubbles
            var bubbles by remember { mutableStateOf(listOf<WaterBubble>()) }

            // Random generator for bubble splashes
            val random = remember { java.util.Random() }

            // Interactive effect: tap on canvas, splashes bubbles
            val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ),
                shape = RoundedCornerShape(22.dp),
                border = BorderStroke(2.dp, Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.tertiary
                    )
                )),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("liquid_hydration_splash_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "🌊 Interactive AquaSpheres",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF00E5FF).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("ACTIVE GRAVITY", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF))
                            }
                        }

                        Text(
                            text = "$waterLoggedMl / $maxTarget ml",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Simulated Water Beaker with Sinusoidal Waves and Bubbles
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF070C14))
                            .clickable {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                if (waterLoggedMl < maxTarget) {
                                    viewModel.updateWaterLoggedMl((waterLoggedMl + 250).coerceAtMost(maxTarget))
                                }
                                val newBubbles = mutableListOf<WaterBubble>()
                                repeat(8) {
                                    newBubbles.add(
                                        WaterBubble(
                                            x = random.nextFloat() * 600f + 50f,
                                            y = 100f,
                                            vx = (random.nextFloat() - 0.5f) * 6f,
                                            vy = -random.nextFloat() * 4f - 1.5f,
                                            radius = random.nextFloat() * 10f + 4f,
                                            alpha = 1f
                                        )
                                    )
                                }
                                bubbles = bubbles + newBubbles
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        val infiniteWaveTransition = rememberInfiniteTransition(label = "WaterWaves")
                        val wavePhase by infiniteWaveTransition.animateFloat(
                            initialValue = 0f,
                            targetValue = 2f * Math.PI.toFloat(),
                            animationSpec = infiniteRepeatable(
                                animation = tween(1500, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "WavePhase"
                        )

                        // Clean up dead bubbles and animate existing ones
                        LaunchedEffect(bubbles) {
                            if (bubbles.isNotEmpty()) {
                                while (true) {
                                    kotlinx.coroutines.delay(16)
                                    bubbles = bubbles.map { b ->
                                        b.copy(
                                            x = (b.x + b.vx + (liveAccelerometer.x * -0.6f)).coerceIn(0f, 1000f),
                                            y = b.y + b.vy,
                                            vx = b.vx * 0.98f,
                                            vy = b.vy - 0.04f, // gravity rise
                                            alpha = b.alpha - 0.015f
                                        )
                                    }.filter { b -> b.alpha > 0f && b.y > -50f }
                                    if (bubbles.isEmpty()) break
                                }
                            }
                        }

                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height

                            // Draw ocean wave fill based on waterLoggedMl
                            val fillRatio = waterLoggedMl.toFloat() / maxTarget.toFloat()
                            val waterHeight = h * fillRatio

                            val path = androidx.compose.ui.graphics.Path()
                            path.moveTo(0f, h)

                            // Gravity physical tilt offset for the wave surface!
                            val tiltFactor = (liveAccelerometer.x * 2.5f).coerceIn(-40f, 40f)

                            for (x in 0..w.toInt() step 5) {
                                val normalizedX = x.toFloat() / w
                                val waveAmplitude = 10f
                                val waveFrequency = 0.012f
                                val y = (h - waterHeight) + 
                                        (Math.sin((x * waveFrequency + wavePhase).toDouble()).toFloat() * waveAmplitude) +
                                        (normalizedX - 0.5f) * tiltFactor
                                
                                path.lineTo(x.toFloat(), y)
                            }
                            path.lineTo(w, h)
                            path.close()

                            // Draw main water body
                            drawPath(
                                path = path,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF00E5FF).copy(alpha = 0.6f),
                                        Color(0xFF007A99).copy(alpha = 0.9f)
                                    )
                                )
                            )

                            // Draw a secondary darker background wave for deep liquid depth
                            val path2 = androidx.compose.ui.graphics.Path()
                            path2.moveTo(0f, h)
                            for (x in 0..w.toInt() step 5) {
                                val normalizedX = x.toFloat() / w
                                val waveAmplitude = 7f
                                val waveFrequency = 0.018f
                                val y = (h - waterHeight) + 
                                        (Math.cos((x * waveFrequency - wavePhase).toDouble()).toFloat() * waveAmplitude) +
                                        (normalizedX - 0.5f) * tiltFactor + 4.dp.toPx()
                                
                                path2.lineTo(x.toFloat(), y)
                            }
                            path2.lineTo(w, h)
                            path2.close()

                            drawPath(
                                path = path2,
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF39FF14).copy(alpha = 0.25f),
                                        Color(0xFF00A3A6).copy(alpha = 0.5f)
                                    )
                                )
                            )

                            // Render all splashing interactive bubbles
                            bubbles.forEach { b ->
                                drawCircle(
                                    brush = Brush.radialGradient(
                                        colors = listOf(Color.White.copy(alpha = b.alpha), Color(0xFF00E5FF).copy(alpha = b.alpha * 0.4f), Color.Transparent),
                                        center = androidx.compose.ui.geometry.Offset(b.x, b.y),
                                        radius = b.radius
                                    ),
                                    radius = b.radius,
                                    center = androidx.compose.ui.geometry.Offset(b.x, b.y)
                                )
                            }
                        }

                        // Help overlay text
                        Text(
                            text = if (waterLoggedMl >= maxTarget) "🎉 HYDRATION TARGET MET!" else "TAP TO SPLASH & RECHARGE WATER",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Black,
                            fontSize = 11.sp,
                            letterSpacing = 0.8.sp,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                if (waterLoggedMl < maxTarget) {
                                    viewModel.updateWaterLoggedMl((waterLoggedMl + 250).coerceAtMost(maxTarget))
                                }
                                val newBubbles = mutableListOf<WaterBubble>()
                                repeat(12) {
                                    newBubbles.add(
                                        WaterBubble(
                                            x = random.nextFloat() * 600f + 50f,
                                            y = 100f,
                                            vx = (random.nextFloat() - 0.5f) * 8f,
                                            vy = -random.nextFloat() * 5f - 2f,
                                            radius = random.nextFloat() * 12f + 5f,
                                            alpha = 1f
                                        )
                                    )
                                }
                                bubbles = bubbles + newBubbles
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("💧 SPOUT +250ML", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Black)
                        }

                        OutlinedButton(
                            onClick = {
                                viewModel.updateWaterLoggedMl(0)
                                bubbles = emptyList()
                            },
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("RESET BUBBLES", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
        }

        // --- Interactive Real-Time Diagnostics & Hardware Sensors Hub ---
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isTelemetryExpanded = !isTelemetryExpanded }
                    .testTag("diagnostics_telemetry_hub_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Header Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Device Telemetry",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Compact toggler button
                        TextButton(
                            onClick = { isTelemetryExpanded = !isTelemetryExpanded },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text(
                                text = if (isTelemetryExpanded) "COLLAPSE" else "EXPAND",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Minimal summary layout
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Summary status dots
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Build, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(12.dp))
                                Text("Battery: ${deviceDiagnostics.batteryPct}%", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(12.dp))
                                Text(if (liveAccelerometer.active) "Motion Active" else "Motion Idle", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.Place, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
                                Text(if (liveLocation.hasLocation) "GPS Locked" else "GPS Searching", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    AnimatedVisibility(
                        visible = isTelemetryExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                            Spacer(modifier = Modifier.height(16.dp))

                            // --- Interactive Sensor Calibration Center ---
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.08f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.padding(bottom = 14.dp).fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Build,
                                                    contentDescription = "Calibration Suite",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Column {
                                                Text(
                                                    text = "Sensors Optimizer Suite",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = if (isAccelCalibrated) "Status: CALIBRATED (Precise & Smooth)" else "Status: UNCALIBRATED (Standard Jitter)",
                                                    fontSize = 9.sp,
                                                    color = if (isAccelCalibrated) Color(0xFF1DB954) else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }

                                        if (calibrationState == CalibrationState.IDLE) {
                                            Button(
                                                onClick = { viewModel.triggerSensorCalibrationSuite() },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.primary,
                                                    contentColor = Color.Black
                                                ),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                                modifier = Modifier.height(30.dp).testTag("btn_calibrate_sensors")
                                            ) {
                                                Text("CALIBRATE SENSORS", fontSize = 9.sp, fontWeight = FontWeight.Black)
                                            }
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = calibrationState.name,
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = MaterialTheme.colorScheme.secondary
                                                )
                                            }
                                        }
                                    }

                                    if (calibrationState != CalibrationState.IDLE) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = calibrationMessage,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = "${(calibrationProgress * 100).toInt()}%",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Black,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        
                                        Spacer(modifier = Modifier.height(6.dp))
                                        
                                        LinearProgressIndicator(
                                            progress = { calibrationProgress },
                                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                                            color = MaterialTheme.colorScheme.primary,
                                            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.12f)
                                        )
                                    } else {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Launches full multi-sensor calibration: zeroes out kinetic gravity biases, balances PPG optical noise gates and aligns real-time GPS satellites for ultra-smooth step, telemetry and cardio-vascular tracking.",
                                            fontSize = 9.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 12.sp
                                        )
                                    }
                                }
                            }

                            // 1. Device Hardware Diagnostics
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.padding(bottom = 12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Build,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.secondary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "On-Device Hardware Profile",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        OutlinedButton(
                                            onClick = { viewModel.refreshDeviceDiagnostics() },
                                            modifier = Modifier.height(26.dp).testTag("btn_refresh_diagnostics"),
                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                                            shape = RoundedCornerShape(6.dp),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(10.dp))
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text("Query", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Model & Battery
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Device Brand / Model", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(deviceDiagnostics.model, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text("Battery Integrity", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("${deviceDiagnostics.batteryPct}% [${deviceDiagnostics.batteryStatus}]", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        // Software & Storage
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("OS (Android Flavor)", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("Android ${deviceDiagnostics.androidVersion}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text("Engine Mem / Disk", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("RAM Free: ${deviceDiagnostics.ramAvailablePercent}% / Disk: ${deviceDiagnostics.freeStorageGb} GB", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }

                            // 2. Accelerometer Kinetic Panel
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.padding(bottom = 12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.LocationOn,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.tertiary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "Kinetic 3-Axis Accelerometer",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    if (liveAccelerometer.active) Color(0xFF1DB954).copy(alpha = 0.15f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                                                    RoundedCornerShape(6.dp)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (liveAccelerometer.active) "STREAMING" else "PAUSED",
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (liveAccelerometer.active) Color(0xFF1DB954) else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    // Motion values grid
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                            Text("X AXIS", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(String.format(Locale.US, "%.3f m/s²", liveAccelerometer.x), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                            Text("Y AXIS", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(String.format(Locale.US, "%.3f m/s²", liveAccelerometer.y), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                        }
                                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                            Text("Z AXIS", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(String.format(Locale.US, "%.3f m/s²", liveAccelerometer.z), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Live graphical magnitude gauge
                                    val safeMagnitude = liveAccelerometer.magnitude.coerceIn(0f, 30f)
                                    val normalizedForce = (safeMagnitude / 30f).coerceIn(0f, 1f)
                                    Text(
                                        text = "Combined Motion Magnitude: ${String.format(Locale.US, "%.1f", liveAccelerometer.magnitude)} m/s²",
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(bottom = 3.dp)
                                    )
                                    LinearProgressIndicator(
                                        progress = { normalizedForce },
                                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                        color = if (liveAccelerometer.magnitude > 13f) Color(0xFF1DB954) else MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                                    )

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // --- Interactive Real-Time Bouncing Sensory Orb ---
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(115.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.Black)
                                            .border(
                                                width = 1.2.dp,
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                                shape = RoundedCornerShape(12.dp)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        val primary = MaterialTheme.colorScheme.primary
                                        val sec = MaterialTheme.colorScheme.secondary
                                        val tert = MaterialTheme.colorScheme.tertiary
                                        
                                        // Animate a pulsing aura
                                        val infiniteTransition = rememberInfiniteTransition(label = "AuraPulse")
                                        val pulseScale by infiniteTransition.animateFloat(
                                            initialValue = 0.85f,
                                            targetValue = 1.15f,
                                            animationSpec = infiniteRepeatable(
                                                animation = tween(1200, easing = FastOutSlowInEasing),
                                                repeatMode = RepeatMode.Reverse
                                             ),
                                             label = "AuraScale"
                                         )

                                         Canvas(modifier = Modifier.fillMaxSize()) {
                                             val w = size.width
                                             val h = size.height
                                             val defaultX = w / 2f
                                             val defaultY = h / 2f

                                             // Render digital glowing retro grid lines
                                             val gridSpacing = 24.dp.toPx()
                                             val currTime = System.currentTimeMillis()
                                             val shiftY = (currTime / 18 % gridSpacing.toInt()).toFloat()

                                             // Draw scrolling horizontal grid lines (giving simulation travel dynamic)
                                             var currentY = shiftY
                                             while (currentY < h) {
                                                 drawLine(
                                                     color = primary.copy(alpha = 0.08f),
                                                     start = androidx.compose.ui.geometry.Offset(0f, currentY),
                                                     end = androidx.compose.ui.geometry.Offset(w, currentY),
                                                     strokeWidth = 0.8.dp.toPx()
                                                 )
                                                 currentY += gridSpacing
                                             }

                                             // Translate gravity sensor values to kinetic displacement
                                             // X: -9.8 to +9.8 -> shifts right/left
                                             // Y: -9.8 to +9.8 -> shifts down/up
                                             val offsetX = (liveAccelerometer.x * -11f).coerceIn(-defaultX + 25.dp.toPx(), defaultX - 25.dp.toPx())
                                             val offsetY = (liveAccelerometer.y * 11f).coerceIn(-defaultY + 25.dp.toPx(), defaultY - 25.dp.toPx())

                                             val centerBallX = defaultX + offsetX
                                             val centerBallY = defaultY + offsetY

                                             val baseRadius = (16f + (liveAccelerometer.magnitude * 1.5f)).coerceIn(14f, 40f).dp.toPx()

                                             // 1. Draw outermost plasma halo
                                             drawCircle(
                                                 color = tert.copy(alpha = 0.12f),
                                                 radius = baseRadius * 1.8f * pulseScale,
                                                 center = androidx.compose.ui.geometry.Offset(centerBallX, centerBallY)
                                             )

                                             // 2. Draw energy aura ring
                                             drawCircle(
                                                 color = sec.copy(alpha = 0.22f),
                                                 radius = baseRadius * 1.35f,
                                                 center = androidx.compose.ui.geometry.Offset(centerBallX, centerBallY)
                                             )

                                             // 3. Glowing core sphere
                                             drawCircle(
                                                 brush = Brush.radialGradient(
                                                     colors = listOf(Color.White, primary, primary.copy(alpha = 0.05f)),
                                                     center = androidx.compose.ui.geometry.Offset(centerBallX, centerBallY),
                                                     radius = baseRadius
                                                 ),
                                                 radius = baseRadius,
                                                 center = androidx.compose.ui.geometry.Offset(centerBallX, centerBallY)
                                             )

                                             // 4. Center reference target point
                                             drawCircle(
                                                 color = sec.copy(alpha = 0.5f),
                                                 radius = 5f,
                                                 center = androidx.compose.ui.geometry.Offset(defaultX, defaultY)
                                             )
                                         }

                                         // Custom interaction label overlay
                                         Column(
                                             horizontalAlignment = Alignment.CenterHorizontally,
                                             modifier = Modifier
                                                 .align(Alignment.BottomCenter)
                                                 .padding(bottom = 6.dp)
                                         ) {
                                             Text(
                                                 text = "KINETIC GRAVITY ORB",
                                                 color = primary,
                                                 fontSize = 8.sp,
                                                 fontWeight = FontWeight.Black,
                                                 letterSpacing = 1.sp
                                             )
                                         }
                                     }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = "💡 Movement sensor is active. Try physically shaking your phone to calibrate kinetic response vectors locally!",
                                        fontSize = 8.5.sp,
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                    )
                                }
                            }

                            // 3. Geolocation Tracker Active Panel
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Place,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "On-Device GPS Geolocation",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }

                                        if (hasLocationPermission) {
                                            Box(
                                                modifier = Modifier
                                                    .background(
                                                        if (liveLocation.hasLocation) Color(0xFF1DB954).copy(alpha = 0.15f) else Color(0xFFF2994A).copy(alpha = 0.15f),
                                                        RoundedCornerShape(6.dp)
                                                    )
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = if (liveLocation.hasLocation) "LOCKED" else "SEARCHING",
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (liveLocation.hasLocation) Color(0xFF1DB954) else Color(0xFFF2994A)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    if (!hasLocationPermission) {
                                        Column(modifier = Modifier.fillMaxWidth()) {
                                            Text(
                                                text = "To fetch real geolocation coordinates, altitude, and live Speed, authorize location sensors.",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                            Button(
                                                onClick = {
                                                    locationPermissionLauncher.launch(
                                                        arrayOf(
                                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                                        )
                                                    )
                                                },
                                                modifier = Modifier.fillMaxWidth().height(32.dp).testTag("btn_request_gps"),
                                                shape = RoundedCornerShape(6.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.primary,
                                                    contentColor = Color.Black
                                                ),
                                                contentPadding = PaddingValues(vertical = 4.dp)
                                            ) {
                                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Authorize Location Sensors", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    } else {
                                        if (liveLocation.error != null) {
                                            Text(
                                                text = "Status: ${liveLocation.error}",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.error,
                                                fontWeight = FontWeight.Medium
                                            )
                                        } else if (!liveLocation.hasLocation) {
                                            Text(
                                                text = "Waiting for satellite signals to compute GPS position...",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                            )
                                        } else {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("Latitude", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Text(String.format(Locale.US, "%.6f°", liveLocation.latitude), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text("Longitude", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Text(String.format(Locale.US, "%.6f°", liveLocation.longitude), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text("Altitude", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Text(String.format(Locale.US, "%.1f meters", liveLocation.altitude), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text("Calculated Travel Speed", fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    Text(String.format(Locale.US, "%.2f km/h", liveLocation.speed), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
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
        }

        // --- Today's Cumulative Progress ---
        item {
            val progressPercent = (animatedProgress * 100).toInt()
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Daily Completion",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (totalActionItems == 0) "No priority plans logged yet" else "$completedActionItems of $totalActionItems target milestones completed",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(54.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                shape = CircleShape
                            )
                    ) {
                        Text(
                            text = "$progressPercent%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // --- To-Do / Task List Section ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today's Agenda",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )

                IconButton(
                    onClick = { showAddTaskDialog = true },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("add_task_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Agenda Item", modifier = Modifier.size(20.dp))
                }
            }
        }

        if (tasks.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Your agenda is clear today. Rest or plan goals!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(tasks, key = { it.id }) { task ->
                TaskRowItem(
                    task = task,
                    onToggle = { viewModel.toggleTaskCompletion(task) },
                    onDelete = { viewModel.deleteTask(task.id) }
                )
            }
        }

        // --- Workout Snapshot ---
        item {
            Text(
                text = "Fitness Plan",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        }

        item {
            val activeWorkoutPlan = workouts.firstOrNull()
            if (activeWorkoutPlan != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToFitness() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Active Routine: ${activeWorkoutPlan.name}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${activeWorkoutPlan.durationMinutes} min • Targets: Weight Progression rules active",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Details",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToFitness() }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No routines registered today. Add one in Fitness section.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }

        // --- Meal Tracker snapshot ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Nutrition Log",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )

                Text(
                    text = "Configure Recipe",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.clickable { onNavigateToMeals() }
                )
            }
        }

        if (meals.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No meals logged yet. Scale and plan via kitchen!",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        } else {
            items(meals, key = { it.id }) { meal ->
                MealCardRow(
                    meal = meal,
                    onToggle = { viewModel.toggleMealCompletion(meal) },
                    onDelete = { viewModel.deleteMeal(meal.id) }
                )
            }
        }

        // --- Theme Changing & System Configuration Deck ---
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("theme_changing_deck_card")
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(8.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Theme Setup",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Command Accent Preferences",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Shift system-wide visual frequency profile",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Theme options row
                    val themeOptions = listOf(
                        Triple("EMERALD", "Emerald Neon Light", EmeraldGreen),
                        Triple("OCEAN", "Vibrant Ocean Breeze", OceanSilverBlue),
                        Triple("CYBER_SUNSET", "Candy Cyber Sunset", CyberRose),
                        Triple("CRIMSON", "Cherry Crimson Bloom", CrimsonScarlet)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        themeOptions.forEach { (themeKey, themeLabel, accentColor) ->
                            val isSelected = appThemeStr == themeKey
                            
                            val animatedSize by animateDpAsState(
                                targetValue = if (isSelected) 50.dp else 40.dp,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
                                label = "ThemeSelectorSize"
                            )
                            
                            val infiniteTransition = rememberInfiniteTransition(label = "ThemeGlow")
                            val glowAlpha by infiniteTransition.animateFloat(
                                initialValue = 0.4f,
                                targetValue = 0.9f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(1000, easing = FastOutSlowInEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "GlowAlpha"
                            )

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { viewModel.updateTheme(themeKey) }
                                    .padding(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(animatedSize)
                                        .background(accentColor, CircleShape)
                                        .border(
                                            width = if (isSelected) 3.5.dp else 1.dp,
                                            color = if (isSelected) accentColor.copy(alpha = glowAlpha) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected Theme",
                                            tint = if (themeKey == "OCEAN" || themeKey == "CRIMSON") Color.White else Color.Black,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = themeLabel.split(" ").last(),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                    color = if (isSelected) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Embedded System Architect Credit block
                    Surface(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Lead Systems Architect:",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "KUNAL R RAJAPPA",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.primary,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // --- Elegant Footer ---
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Life OS Hub v2.1.0",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "DESIGNED & BUILT BY KUNAL R RAJAPPA",
                    fontWeight = FontWeight.Medium,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    letterSpacing = 1.sp
                )
            }
        }
    }

    // --- Add Agenda Task Dialog ---
    if (showAddTaskDialog) {
        var taskTitle by remember { mutableStateOf("") }
        var blockTimeOption by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddTaskDialog = false },
            title = { Text("New Agenda Task", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = taskTitle,
                        onValueChange = { taskTitle = it },
                        label = { Text("Task Title") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("dialog_new_task_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = blockTimeOption,
                        onValueChange = { blockTimeOption = it },
                        label = { Text("Time-Block (e.g. 09:00 AM, optional)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (taskTitle.isNotBlank()) {
                            viewModel.addTask(
                                taskTitle,
                                blockTimeOption.ifBlank { null }
                            )
                            showAddTaskDialog = false
                        }
                    },
                    modifier = Modifier.testTag("dialog_new_task_submit")
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddTaskDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // --- Reset All Data Confirmation Dialog ---
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Application Data?", fontWeight = FontWeight.Bold) },
            text = {
                Text("This will permanently delete all logged tasks, daily agenda history, kitchen meals, cooking recipes, and pantry stock items. The app will state will be completely cleaned.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllUserData()
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    modifier = Modifier.testTag("dialog_reset_submit")
                ) {
                    Text("Clear All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun HealthStatItem(
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(72.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(iconColor.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(18.dp))
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun TaskRowItem(
    task: TaskEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    var isChecked by remember(task.isCompleted) { mutableStateOf(task.isCompleted) }
    
    val animatedBgColor by animateColorAsState(
        targetValue = if (isChecked) MaterialTheme.colorScheme.surface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(durationMillis = 350, easing = LinearEasing),
        label = "task_bg_color_anim"
    )

    Card(
        colors = CardDefaults.cardColors(
            containerColor = animatedBgColor
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isChecked,
                onCheckedChange = {
                    isChecked = it
                    onToggle()
                }
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    textDecoration = if (isChecked) TextDecoration.LineThrough else null,
                    color = if (isChecked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                )
                if (!task.blockTime.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Time Block",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Scheduled at ${task.blockTime}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Task",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun MealCardRow(
    meal: MealEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    var isChecked by remember(meal.isCompleted) { mutableStateOf(meal.isCompleted) }

    val animatedBgColor by animateColorAsState(
        targetValue = if (isChecked) MaterialTheme.colorScheme.surface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surface,
        animationSpec = tween(durationMillis = 350, easing = LinearEasing),
        label = "meal_bg_color_anim"
    )

    Card(
        colors = CardDefaults.cardColors(
            containerColor = animatedBgColor
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isChecked,
                onCheckedChange = {
                    isChecked = it
                    onToggle()
                }
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = meal.name,
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                        textDecoration = if (isChecked) TextDecoration.LineThrough else null,
                        color = if (isChecked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = meal.mealType,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f),
                                RoundedCornerShape(4.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "🥩 P: ${meal.protein}g  🌾 C: ${meal.carbs}g  🥑 F: ${meal.fat}g  🔥 ${meal.calories} kcal",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Meal log",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
