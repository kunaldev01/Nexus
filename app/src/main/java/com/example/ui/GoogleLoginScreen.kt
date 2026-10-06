package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.LifeOsViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoogleLoginScreen(
    viewModel: LifeOsViewModel,
    onLoginSuccess: () -> Unit
) {
    var onboardingStep by remember { mutableStateOf(1) } // 1: Name & Email, 2: Biometrics Setup
    var emailInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }
    
    // Default sensible onboarding parameters
    var heightInput by remember { mutableStateOf("175") }
    var weightInput by remember { mutableStateOf("70") }
    var ageInput by remember { mutableStateOf("28") }
    var genderVal by remember { mutableStateOf("Male") }
    var activityVal by remember { mutableStateOf("Moderate") }

    var isConnecting by remember { mutableStateOf(false) }
    var calibrationMessage by remember { mutableStateOf("Authorizing Google Identity handshake...") }
    var validationError by remember { mutableStateOf<String?>(null) }

    val gradientBackground = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.surface,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(gradientBackground)
            .padding(16.dp)
            .testTag("google_login_screen"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isConnecting) {
                // High-End Interactive Calibration Loader
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = "Life OS Calibration Engine",
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = calibrationMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Calibrating Mifflin-St Jeor metabolic levels with active device telemetry, location parameters and step diagnostics.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                }
            } else {
                // Header section with Google BioServices branding
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .background(Color.White, CircleShape)
                            .border(1.dp, Color.LightGray.copy(alpha = 0.3f), CircleShape)
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "G",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.SansSerif,
                            color = Color(0xFF4285F4)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Welcome to Life OS",
                    fontWeight = FontWeight.Black,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = "Google Identity Safe Onboarding Protocol",
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Multi-step form card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        // Progress indicators / tabs
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (onboardingStep >= 1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                    )
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (onboardingStep >= 2) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                    )
                            )
                        }

                        AnimatedContent(
                            targetState = onboardingStep,
                            transitionSpec = {
                                if (targetState > initialState) {
                                    (slideInHorizontally { width -> width } + fadeIn() togetherWith
                                     slideOutHorizontally { width -> -width } + fadeOut())
                                } else {
                                    (slideInHorizontally { width -> -width } + fadeIn() togetherWith
                                     slideOutHorizontally { width -> width } + fadeOut())
                                }
                            },
                            label = "OnboardingStepTransition"
                        ) { step ->
                            when (step) {
                                1 -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                        Text(
                                            text = "STEP 1: IDENTITY DETAILS",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.primary,
                                            letterSpacing = 1.sp
                                        )

                                        Text(
                                             text = "Please customize your identity parameters to establish a local secure environment database connection.",
                                             fontSize = 12.sp,
                                             color = MaterialTheme.colorScheme.onSurfaceVariant,
                                             lineHeight = 16.sp
                                        )

                                        OutlinedTextField(
                                            value = nameInput,
                                            onValueChange = {
                                                nameInput = it
                                                validationError = null
                                            },
                                            label = { Text("Display Name") },
                                            leadingIcon = { Icon(Icons.Default.Person, null, tint = MaterialTheme.colorScheme.primary) },
                                            placeholder = { Text("e.g. Kunal") },
                                            isError = validationError != null && nameInput.isBlank(),
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth().testTag("onboarding_name_field"),
                                            shape = RoundedCornerShape(12.dp),
                                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                                        )

                                        OutlinedTextField(
                                            value = emailInput,
                                            onValueChange = {
                                                emailInput = it
                                                validationError = null
                                            },
                                            label = { Text("Google Account Email") },
                                            leadingIcon = { Icon(Icons.Default.Email, null, tint = MaterialTheme.colorScheme.primary) },
                                            placeholder = { Text("e.g. kunal@gmail.com") },
                                            isError = validationError != null && (emailInput.isBlank() || !emailInput.contains("@")),
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth().testTag("onboarding_email_field"),
                                            shape = RoundedCornerShape(12.dp),
                                            keyboardOptions = KeyboardOptions(
                                                keyboardType = KeyboardType.Email,
                                                imeAction = ImeAction.Done
                                            )
                                        )

                                        if (validationError != null) {
                                            Text(
                                                text = validationError ?: "",
                                                color = MaterialTheme.colorScheme.error,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                if (nameInput.isBlank()) {
                                                    validationError = "Please enter your Display Name"
                                                } else if (emailInput.isBlank() || !emailInput.contains("@")) {
                                                    validationError = "Please enter a valid Google Account Email"
                                                } else {
                                                    validationError = null
                                                    onboardingStep = 2
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.primary,
                                                contentColor = Color.Black
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(50.dp)
                                                .testTag("onboarding_next_button")
                                        ) {
                                            Text("Next: Calibrate Biometrics", fontWeight = FontWeight.Black)
                                        }
                                    }
                                }
                                2 -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                        Text(
                                            text = "STEP 2: BIOMETRIC CALIBRATION",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.primary,
                                            letterSpacing = 1.sp
                                        )

                                        Text(
                                            text = "Configure your body dimensions to calculate accurate hydration limits, exercise loads, and metabolic caloric multipliers.",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            lineHeight = 16.sp
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = heightInput,
                                                onValueChange = { heightInput = it.filter { char -> char.isDigit() || char == '.' } },
                                                label = { Text("Height (cm)", fontSize = 11.sp) },
                                                textStyle = MaterialTheme.typography.bodyMedium,
                                                modifier = Modifier.weight(1f).testTag("onboarding_height_field"),
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
                                            )
                                            OutlinedTextField(
                                                value = weightInput,
                                                onValueChange = { weightInput = it.filter { char -> char.isDigit() || char == '.' } },
                                                label = { Text("Weight (kg)", fontSize = 11.sp) },
                                                textStyle = MaterialTheme.typography.bodyMedium,
                                                modifier = Modifier.weight(1f).testTag("onboarding_weight_field"),
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
                                            )
                                            OutlinedTextField(
                                                value = ageInput,
                                                onValueChange = { ageInput = it.filter { char -> char.isDigit() } },
                                                label = { Text("Age", fontSize = 11.sp) },
                                                textStyle = MaterialTheme.typography.bodyMedium,
                                                modifier = Modifier.weight(1f).testTag("onboarding_age_field"),
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
                                            )
                                        }

                                        // Gender Choice
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text("Gender:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                listOf("Male", "Female", "Other").forEach { g ->
                                                    val isSelected = genderVal == g
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
                                                            .border(
                                                                width = 1.dp,
                                                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                                                shape = RoundedCornerShape(8.dp)
                                                            )
                                                            .clickable { genderVal = g }
                                                            .padding(vertical = 8.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(g, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                                                    }
                                                }
                                            }
                                        }

                                        // Activity Factor Choice
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text("Weekly Activity Level:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                listOf("Sedentary", "Light", "Moderate", "Active").forEach { act ->
                                                    val isSelected = activityVal == act
                                                    Box(
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(if (isSelected) MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f) else Color.Transparent)
                                                            .border(
                                                                width = 1.dp,
                                                                color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                                                                shape = RoundedCornerShape(8.dp)
                                                            )
                                                            .clickable { activityVal = act }
                                                            .padding(vertical = 6.dp),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(act, fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = if (isSelected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface)
                                                    }
                                                }
                                            }
                                        }

                                        if (validationError != null) {
                                            Text(
                                                text = validationError ?: "",
                                                color = MaterialTheme.colorScheme.error,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(top = 2.dp)
                                            )
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = { onboardingStep = 1 },
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.weight(1f).height(50.dp)
                                            ) {
                                                Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Back")
                                            }

                                            Button(
                                                onClick = {
                                                    val h = heightInput.toFloatOrNull()
                                                    val w = weightInput.toFloatOrNull()
                                                    val a = ageInput.toIntOrNull()

                                                    if (h == null || h <= 0f) {
                                                        validationError = "Please enter a valid height in cm"
                                                    } else if (w == null || w <= 0f) {
                                                        validationError = "Please enter a valid weight in kg"
                                                    } else if (a == null || a <= 0) {
                                                        validationError = "Please enter a valid age"
                                                    } else {
                                                        validationError = null
                                                        isConnecting = true
                                                        
                                                        // Sequence high-fidelity loading text transitions
                                                        val handler = android.os.Handler(android.os.Looper.getMainLooper())
                                                        calibrationMessage = "Authorizing Google Identity OAuth..."
                                                        
                                                        handler.postDelayed({
                                                            calibrationMessage = "Calibrating personalized Mifflin-St Jeor metabolic index..."
                                                        }, 1000)

                                                        handler.postDelayed({
                                                            calibrationMessage = "Establishing local secure SQLite database cache..."
                                                        }, 2000)

                                                        handler.postDelayed({
                                                            calibrationMessage = "Syncing physical dimensions to Life OS dashboard!"
                                                        }, 3000)

                                                        handler.postDelayed({
                                                            // Persist user details, register bio data, and complete onboarding
                                                            viewModel.updateUserProfile(h, w, a, genderVal, activityVal)
                                                            viewModel.signInWithGoogle(emailInput, nameInput)
                                                            isConnecting = false
                                                            onLoginSuccess()
                                                        }, 4200)
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.primary,
                                                    contentColor = Color.Black
                                                ),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.weight(1.5f).height(50.dp).testTag("onboarding_complete_button")
                                            ) {
                                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Complete Setup", fontWeight = FontWeight.Black)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Secure Handshake Footer
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Google Identity Safe Handshake Protocol",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
