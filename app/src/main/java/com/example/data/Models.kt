package com.example.data

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GoalSubtask(
    val id: String,
    val text: String,
    val relativeTimeline: String,
    val isCompleted: Boolean = false,
    val linkedTaskCreated: Boolean = false
)

@JsonClass(generateAdapter = true)
data class RecipeIngredient(
    val name: String,
    val baseAmount: Float, // Amount for 1 serving
    val unit: String
)

data class WearableData(
    val steps: Int,
    val activeCalories: Int,
    val sleepHours: Float,
    val restingHeartRate: Int
)

data class ChatMessage(
    val id: String,
    val text: String,
    val isUser: Boolean,
    val timestamp: java.util.Date
)

data class RecordedActivity(
    val id: String,
    val type: String, // e.g., "Strength", "Cardio", "Yoga", "HIIT", "Pilates", "Walking"
    val durationMinutes: Int,
    val intensity: String, // "Low", "Medium", "High"
    val timestamp: Long
)

data class LocationData(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val altitude: Double = 0.0,
    val speed: Float = 0f, // in km/h
    val hasLocation: Boolean = false,
    val error: String? = null
)

data class AccelerometerData(
    val x: Float = 0f,
    val y: Float = 0f,
    val z: Float = 0f,
    val magnitude: Float = 0f,
    val active: Boolean = false
)

data class DeviceDiagnosticsData(
    val batteryPct: Int = 100,
    val batteryStatus: String = "Unknown",
    val model: String = "Android Device",
    val androidVersion: String = "13",
    val ramAvailablePercent: Int = 100,
    val ramTotalGb: Double = 8.0,
    val freeStorageGb: Double = 64.0
)

data class LifeOsSynergyState(
    val fitnessScore: Float = 0.0f,            // 0.0 to 1.0 (based on steps / 10,000 steps goal)
    val nutritionScore: Float = 0.0f,          // 0.0 to 1.0 (based on logging/completing planned meals)
    val productivityScore: Float = 0.0f,       // 0.0 to 1.0 (based on tasks + completed subtasks)
    val totalSynergyScore: Float = 0.0f,       // Combination fitness, nutrition, productivity
    val calorieAdjustedBudget: Int = 2000,     // Adjusted dynamically based on steps & activity burn
    val targetHydrationLiters: Float = 2.5f,   // Water target calibrated dynamically from activity intensity
    val synergyAlertMessage: String = "Modules synchronized under a unified state context."
)

enum class CalibrationState {
    IDLE,
    PREPARING,
    CALIBRATING_ACCEL,    // Step 1: Calibrate accelerometer gravity vector (zero-point)
    CALIBRATING_PPG,      // Step 2: Calibrate finger PPG camera reflection filter baseline thresholds
    CALIBRATING_GPS,      // Step 3: Align GPS satellites and calibrate latency bounds
    SUCCESS,
    FAILED
}



