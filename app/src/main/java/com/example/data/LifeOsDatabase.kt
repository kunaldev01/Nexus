package com.example.data

import android.content.Context
import androidx.room.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.Flow

// --- Converters for lists stored as JSON ---
class Converters {
    private val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

    @TypeConverter
    fun fromSubtaskList(value: List<GoalSubtask>?): String? {
        if (value == null) return null
        val type = Types.newParameterizedType(List::class.java, GoalSubtask::class.java)
        return moshi.adapter<List<GoalSubtask>>(type).toJson(value)
    }

    @TypeConverter
    fun toSubtaskList(value: String?): List<GoalSubtask>? {
        if (value == null) return null
        val type = Types.newParameterizedType(List::class.java, GoalSubtask::class.java)
        return moshi.adapter<List<GoalSubtask>>(type).fromJson(value)
    }

    @TypeConverter
    fun fromIngredientList(value: List<RecipeIngredient>?): String? {
        if (value == null) return null
        val type = Types.newParameterizedType(List::class.java, RecipeIngredient::class.java)
        return moshi.adapter<List<RecipeIngredient>>(type).toJson(value)
    }

    @TypeConverter
    fun toIngredientList(value: String?): List<RecipeIngredient>? {
        if (value == null) return null
        val type = Types.newParameterizedType(List::class.java, RecipeIngredient::class.java)
        return moshi.adapter<List<RecipeIngredient>>(type).fromJson(value)
    }
}

// --- Entities ---

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val isCompleted: Boolean = false,
    val linkedGoalId: Int? = null,
    val isGroceryLinked: Boolean = false,
    val blockTime: String? = null // e.g. "09:00 AM" or null
)

@Entity(tableName = "workouts")
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val durationMinutes: Int,
    val caloriesBurned: Int,
    val date: String // e.g., "2026-06-14"
)

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val workoutId: Int, // relates to WorkoutEntity.id
    val name: String,
    val currentWeightLbs: Float,
    val currentReps: Int,
    val currentSets: Int = 3,
    val lastCompletedWeight: Float? = null,
    val lastCompletedReps: Int? = null,
    val targetWeightLbs: Float,
    val targetReps: Int,
    val isCompleted: Boolean = false,
    val videoTutorialUrl: String? = null
)

@Entity(tableName = "meals")
data class MealEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val mealType: String, // Breakfast, Lunch, Dinner, Snack
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fat: Int,
    val isCompleted: Boolean = false,
    val date: String // e.g. "2026-06-14"
)

@Entity(tableName = "recipes")
data class RecipeEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val ingredients: List<RecipeIngredient>,
    val calories: Int,
    val protein: Int,
    val carbs: Int,
    val fat: Int,
    val instructions: String,
    val servingSize: Int = 1
)

@Entity(tableName = "pantry")
data class PantryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val quantity: Float = 1.0f,
    val unit: String = "pcs",
    val inStock: Boolean = true
)

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val targetDate: String,
    val subtasks: List<GoalSubtask>,
    val isCompleted: Boolean = false
)

// --- User Profile & User Data Entities ---

@Entity(tableName = "user_profile")
data class UserEntity(
    @PrimaryKey val id: Int = 1, // Single profile or current active user ID
    val name: String = "Life OS Athlete",
    val email: String = "user@example.com",
    val heightCm: Float = 175f,
    val weightKg: Float = 70f,
    val age: Int = 28,
    val gender: String = "Male",
    val activityLevel: String = "Moderate",
    val isGoogleSignedIn: Boolean = false,
    val dailyCalorieTarget: Int = 2000,
    val waterGoalMl: Int = 2500,
    val waterLoggedMl: Int = 1250,
    val sleepHoursGoal: Float = 8.0f,
    val stepsGoal: Int = 10000,
    val connectedWearableBrand: String? = null,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_activities")
data class UserActivityEntity(
    @PrimaryKey val id: String,
    val type: String, // "Strength", "Cardio", "Yoga", "Walking", etc.
    val durationMinutes: Int,
    val intensity: String,
    val timestamp: Long = System.currentTimeMillis(),
    val caloriesBurned: Int = 0
)

@Entity(tableName = "user_chat_messages")
data class UserChatEntity(
    @PrimaryKey val id: String,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_metric_logs")
data class UserMetricLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String, // e.g. "2026-09-22"
    val steps: Int,
    val activeCalories: Int,
    val sleepHours: Float,
    val restingHeartRate: Int,
    val waterMl: Int,
    val timestamp: Long = System.currentTimeMillis()
)

// --- DAO ---

@Dao
interface LifeOsDao {
    // Tasks
    @Query("SELECT * FROM tasks ORDER BY id DESC")
    fun getAllTasks(): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Int)

    // Workouts
    @Query("SELECT * FROM workouts ORDER BY date DESC, id DESC")
    fun getAllWorkouts(): Flow<List<WorkoutEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: WorkoutEntity): Long

    @Query("DELETE FROM workouts WHERE id = :id")
    suspend fun deleteWorkoutById(id: Int)

    // Exercises
    @Query("SELECT * FROM exercises WHERE workoutId = :workoutId")
    fun getExercisesForWorkout(workoutId: Int): Flow<List<ExerciseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: ExerciseEntity): Long

    @Update
    suspend fun updateExercise(exercise: ExerciseEntity)

    @Query("DELETE FROM exercises WHERE id = :id")
    suspend fun deleteExerciseById(id: Int)

    // Meals
    @Query("SELECT * FROM meals ORDER BY id DESC")
    fun getAllMeals(): Flow<List<MealEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(meal: MealEntity): Long

    @Update
    suspend fun updateMeal(meal: MealEntity)

    @Query("DELETE FROM meals WHERE id = :id")
    suspend fun deleteMealById(id: Int)

    // Recipes
    @Query("SELECT * FROM recipes ORDER BY name ASC")
    fun getAllRecipes(): Flow<List<RecipeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipe(recipe: RecipeEntity): Long

    @Query("DELETE FROM recipes WHERE id = :id")
    suspend fun deleteRecipeById(id: Int)

    // Pantry
    @Query("SELECT * FROM pantry ORDER BY name ASC")
    fun getAllPantryItems(): Flow<List<PantryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPantryItem(item: PantryEntity): Long

    @Update
    suspend fun updatePantryItem(item: PantryEntity)

    @Query("DELETE FROM pantry WHERE id = :id")
    suspend fun deletePantryItemById(id: Int)

    // Goals
    @Query("SELECT * FROM goals ORDER BY id DESC")
    fun getAllGoals(): Flow<List<GoalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: GoalEntity): Long

    @Update
    suspend fun updateGoal(goal: GoalEntity)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteGoalById(id: Int)

    // --- User Profile & User Data DAO Methods ---
    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    fun getUserProfile(): Flow<UserEntity?>

    @Query("SELECT * FROM user_profile WHERE id = 1 LIMIT 1")
    suspend fun getUserProfileOnce(): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateUserProfile(user: UserEntity)

    @Query("DELETE FROM user_profile")
    suspend fun clearUserProfile()

    // User Activities
    @Query("SELECT * FROM user_activities ORDER BY timestamp DESC")
    fun getAllUserActivities(): Flow<List<UserActivityEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserActivity(activity: UserActivityEntity): Long

    @Query("DELETE FROM user_activities WHERE id = :id")
    suspend fun deleteUserActivityById(id: String)

    @Query("DELETE FROM user_activities")
    suspend fun clearUserActivities()

    // User Chat Messages
    @Query("SELECT * FROM user_chat_messages ORDER BY timestamp ASC")
    fun getAllChatMessages(): Flow<List<UserChatEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: UserChatEntity): Long

    @Query("DELETE FROM user_chat_messages")
    suspend fun clearChatMessages()

    // User Metric Logs
    @Query("SELECT * FROM user_metric_logs ORDER BY timestamp DESC")
    fun getAllUserMetricLogs(): Flow<List<UserMetricLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserMetricLog(log: UserMetricLogEntity): Long
}

// --- AppDatabase ---

@Database(
    entities = [
        TaskEntity::class,
        WorkoutEntity::class,
        ExerciseEntity::class,
        MealEntity::class,
        RecipeEntity::class,
        PantryEntity::class,
        GoalEntity::class,
        UserEntity::class,
        UserActivityEntity::class,
        UserChatEntity::class,
        UserMetricLogEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class LifeOsDatabase : RoomDatabase() {
    abstract fun dao(): LifeOsDao

    companion object {
        @Volatile
        private var INSTANCE: LifeOsDatabase? = null

        fun getDatabase(context: Context): LifeOsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LifeOsDatabase::class.java,
                    "life_os_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
