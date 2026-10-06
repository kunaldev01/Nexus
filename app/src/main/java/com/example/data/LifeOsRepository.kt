package com.example.data

import kotlinx.coroutines.flow.Flow

class LifeOsRepository(private val dao: LifeOsDao) {

    // Tasks
    val allTasks: Flow<List<TaskEntity>> = dao.getAllTasks()

    suspend fun insertTask(task: TaskEntity): Long = dao.insertTask(task)

    suspend fun updateTask(task: TaskEntity) = dao.updateTask(task)

    suspend fun deleteTaskById(id: Int) = dao.deleteTaskById(id)

    // Workouts
    val allWorkouts: Flow<List<WorkoutEntity>> = dao.getAllWorkouts()

    suspend fun insertWorkout(workout: WorkoutEntity): Long = dao.insertWorkout(workout)

    suspend fun deleteWorkoutById(id: Int) = dao.deleteWorkoutById(id)

    // Exercises
    fun getExercisesForWorkout(workoutId: Int): Flow<List<ExerciseEntity>> = dao.getExercisesForWorkout(workoutId)

    suspend fun insertExercise(exercise: ExerciseEntity): Long = dao.insertExercise(exercise)

    suspend fun updateExercise(exercise: ExerciseEntity) = dao.updateExercise(exercise)

    suspend fun deleteExerciseById(id: Int) = dao.deleteExerciseById(id)

    // Meals
    val allMeals: Flow<List<MealEntity>> = dao.getAllMeals()

    suspend fun insertMeal(meal: MealEntity): Long = dao.insertMeal(meal)

    suspend fun updateMeal(meal: MealEntity) = dao.updateMeal(meal)

    suspend fun deleteMealById(id: Int) = dao.deleteMealById(id)

    // Recipes
    val allRecipes: Flow<List<RecipeEntity>> = dao.getAllRecipes()

    suspend fun insertRecipe(recipe: RecipeEntity): Long = dao.insertRecipe(recipe)

    suspend fun deleteRecipeById(id: Int) = dao.deleteRecipeById(id)

    // Pantry
    val allPantryItems: Flow<List<PantryEntity>> = dao.getAllPantryItems()

    suspend fun insertPantryItem(item: PantryEntity): Long = dao.insertPantryItem(item)

    suspend fun updatePantryItem(item: PantryEntity) = dao.updatePantryItem(item)

    suspend fun deletePantryItemById(id: Int) = dao.deletePantryItemById(id)

    // Goals
    val allGoals: Flow<List<GoalEntity>> = dao.getAllGoals()

    suspend fun insertGoal(goal: GoalEntity): Long = dao.insertGoal(goal)

    suspend fun updateGoal(goal: GoalEntity) = dao.updateGoal(goal)

    suspend fun deleteGoalById(id: Int) = dao.deleteGoalById(id)

    // User Profile
    val userProfile: Flow<UserEntity?> = dao.getUserProfile()

    suspend fun getUserProfileOnce(): UserEntity? = dao.getUserProfileOnce()

    suspend fun insertOrUpdateUserProfile(user: UserEntity) = dao.insertOrUpdateUserProfile(user)

    suspend fun clearUserProfile() = dao.clearUserProfile()

    // User Activities
    val allUserActivities: Flow<List<UserActivityEntity>> = dao.getAllUserActivities()

    suspend fun insertUserActivity(activity: UserActivityEntity): Long = dao.insertUserActivity(activity)

    suspend fun deleteUserActivityById(id: String) = dao.deleteUserActivityById(id)

    suspend fun clearUserActivities() = dao.clearUserActivities()

    // User Chat Messages
    val allChatMessages: Flow<List<UserChatEntity>> = dao.getAllChatMessages()

    suspend fun insertChatMessage(message: UserChatEntity): Long = dao.insertChatMessage(message)

    suspend fun clearChatMessages() = dao.clearChatMessages()

    // User Metric Logs
    val allUserMetricLogs: Flow<List<UserMetricLogEntity>> = dao.getAllUserMetricLogs()

    suspend fun insertUserMetricLog(log: UserMetricLogEntity): Long = dao.insertUserMetricLog(log)
}
