package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.core.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.font.FontFamily
import com.example.viewmodel.FoodDetectionResult
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.PantryEntity
import com.example.data.RecipeEntity
import com.example.viewmodel.LifeOsViewModel
import java.util.*

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview as CameraXPreview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import java.io.ByteArrayOutputStream

@Composable
fun MealScreen(
    viewModel: LifeOsViewModel,
    modifier: Modifier = Modifier
) {
    val meals by viewModel.meals.collectAsState()
    val recipes by viewModel.recipes.collectAsState()
    val pantryItems by viewModel.pantryItems.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showAddMealDialog by remember { mutableStateOf(false) }
    var showAddPantryItemDialog by remember { mutableStateOf(false) }

    // Calc total calories logged
    val completedMeals = meals.filter { it.isCompleted }
    val totalCalories = completedMeals.sumOf { it.calories }
    val totalProtein = completedMeals.sumOf { it.protein }
    val totalCarbs = completedMeals.sumOf { it.carbs }
    val totalFat = completedMeals.sumOf { it.fat }

    // Targets
    val targetCalories = 2400
    val targetProtein = 160
    val targetCarbs = 260
    val targetFat = 80

    val calorieProgress = (totalCalories.toFloat() / targetCalories).coerceIn(0f, 1f)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("recipe_meal_screen"),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // --- Header Section ---
        item {
            Column {
                Text(
                    text = "Smart Kitchen",
                    style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Black),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Nutrition Progress, Recipe Scaler & Inventory",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // --- Realtime Macro / Calories Dashboard ---
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Macro Targets Status",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$totalCalories / $targetCalories kcal",
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    LinearProgressIndicator(
                        progress = { calorieProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Mini Macros Progress bars
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        MacroMiniTracker(
                            name = "Protein",
                            current = totalProtein,
                            target = targetProtein,
                            barColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        MacroMiniTracker(
                            name = "Carbs",
                            current = totalCarbs,
                            target = targetCarbs,
                            barColor = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.weight(1f)
                        )
                        MacroMiniTracker(
                            name = "Fats",
                            current = totalFat,
                            target = targetFat,
                            barColor = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // --- AI Food Lens Scanner ---
        item {
            AiFoodCameraCard(viewModel = viewModel)
        }

        // --- Recipe Searchable Catalog ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recipe Vault",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )

                Button(
                    onClick = { showAddMealDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                        contentColor = MaterialTheme.colorScheme.secondary
                    ),
                    modifier = Modifier.testTag("log_manual_meal_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Log Meal", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search Recipes...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("recipe_search_input"),
                singleLine = true,
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear") // fallback close style
                        }
                    }
                }
            )
        }

        // Recipes List
        val filteredRecipes = recipes.filter {
            it.name.contains(searchQuery, ignoreCase = true)
        }

        if (filteredRecipes.isEmpty()) {
            item {
                Text(
                    "No recipes found matching \"$searchQuery\". Try Post-workout or Salmon!",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            items(filteredRecipes, key = { it.id }) { recipe ->
                RecipeScaleCard(
                    recipe = recipe,
                    onAddToWeeklyPlan = { scale ->
                        viewModel.addRecipeToWeeklyPlan(recipe, scale)
                    }
                )
            }
        }

        // --- Grocery Checklist & Inventory Section ---
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Grocery & Pantry",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )

                IconButton(
                    onClick = { showAddPantryItemDialog = true },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("add_grocery_item_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Inventory Item", modifier = Modifier.size(18.dp))
                }
            }
        }

        // Split lists: Grocery (Out of stock = false) vs Pantry (In stock = true)
        val groceryList = pantryItems.filter { !it.inStock }
        val ownedPantryList = pantryItems.filter { it.inStock }

        item {
            Text(
                text = "🚨 Shopping List (Auto-Generated)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.tertiary
            )
        }

        if (groceryList.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "Your grocery cart is empty. Planning recipes automatically adds missing ingredients!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(groceryList, key = { it.id }) { item ->
                PantryItemRow(
                    item = item,
                    onToggle = { viewModel.togglePantryStock(item) },
                    onDelete = { viewModel.deletePantryItem(item.id) }
                )
            }
        }

        item {
            Text(
                text = "📦 In stock / Pantry Inventory",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
        }

        if (ownedPantryList.isEmpty()) {
            item {
                Text(
                    "Pantry is dry. Move shopped items over here!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(ownedPantryList, key = { it.id }) { item ->
                PantryItemRow(
                    item = item,
                    onToggle = { viewModel.togglePantryStock(item) },
                    onDelete = { viewModel.deletePantryItem(item.id) }
                )
            }
        }
    }

    // --- Dialogs ---
    if (showAddMealDialog) {
        var mealName by remember { mutableStateOf("") }
        var caloriesVal by remember { mutableStateOf("450") }
        var typeSelected by remember { mutableStateOf("Lunch") }
        var pVal by remember { mutableStateOf("25") }
        var cVal by remember { mutableStateOf("40") }
        var fVal by remember { mutableStateOf("10") }

        AlertDialog(
            onDismissRequest = { showAddMealDialog = false },
            title = { Text("Log Food Intake Intake", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = mealName,
                        onValueChange = { mealName = it },
                        label = { Text("Food / Meal Name") },
                        modifier = Modifier.fillMaxWidth().testTag("add_meal_name_field"),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Breakfast", "Lunch", "Dinner", "Snack").forEach { type ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (typeSelected == type) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                    )
                                    .clickable { typeSelected = type }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = type,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (typeSelected == type) Color.Black else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = caloriesVal,
                        onValueChange = { caloriesVal = it },
                        label = { Text("Total Calories (kcal)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = pVal,
                            onValueChange = { pVal = it },
                            label = { Text("Protein (g)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = cVal,
                            onValueChange = { cVal = it },
                            label = { Text("Carbs (g)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = fVal,
                            onValueChange = { fVal = it },
                            label = { Text("Fat (g)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (mealName.isNotBlank()) {
                            viewModel.addMeal(
                                name = mealName,
                                type = typeSelected,
                                calories = caloriesVal.toIntOrNull() ?: 0,
                                protein = pVal.toIntOrNull() ?: 0,
                                carbs = cVal.toIntOrNull() ?: 0,
                                fat = fVal.toIntOrNull() ?: 0
                            )
                            showAddMealDialog = false
                        }
                    },
                    modifier = Modifier.testTag("add_meal_submit")
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddMealDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAddPantryItemDialog) {
        var pantryName by remember { mutableStateOf("") }
        var qty by remember { mutableStateOf("1") }
        var unitVal by remember { mutableStateOf("pcs") }
        var inStockChecked by remember { mutableStateOf(false) } // Default to shopping list item

        AlertDialog(
            onDismissRequest = { showAddPantryItemDialog = false },
            title = { Text("Add Pantry/Shopping Item", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = pantryName,
                        onValueChange = { pantryName = it },
                        label = { Text("Item Name (e.g. Eggs)") },
                        modifier = Modifier.fillMaxWidth().testTag("add_pantry_name_field"),
                        singleLine = true
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = qty,
                            onValueChange = { qty = it },
                            label = { Text("Qty") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = unitVal,
                            onValueChange = { unitVal = it },
                            label = { Text("Unit (g, pcs, ml)") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = inStockChecked,
                            onCheckedChange = { inStockChecked = it }
                        )
                        Text("Already in-stock in my kitchen pantry")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pantryName.isNotBlank()) {
                            viewModel.addPantryItem(
                                name = pantryName,
                                quantity = qty.toFloatOrNull() ?: 1.0f,
                                unit = unitVal.ifBlank { "pcs" },
                                inStock = inStockChecked
                            )
                            showAddPantryItemDialog = false
                        }
                    },
                    modifier = Modifier.testTag("add_pantry_submit")
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPantryItemDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun MacroMiniTracker(
    name: String,
    current: Int,
    target: Int,
    barColor: Color,
    modifier: Modifier = Modifier
) {
    val progress = (current.toFloat() / target).coerceIn(0f, 1f)
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(name, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("$current/${target}g", fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = barColor,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
fun RecipeScaleCard(
    recipe: RecipeEntity,
    onAddToWeeklyPlan: (Float) -> Unit
) {
    var scaleSliderValue by remember { mutableFloatStateOf(1f) }
    val roundedScale = scaleSliderValue.toInt()
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("recipe_card_${recipe.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recipe.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Base Portion: ${recipe.servingSize} serving • Calories per serving: ${(recipe.calories)} kcal",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = { isExpanded = !isExpanded }) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse Ingredients View" else "Expand Ingredients View"
                    )
                }
            }

            // Whole detailed view expands/collapses
            AnimatedVisibility(visible = isExpanded) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))

                    // Serving Sizer Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            "Servings: $roundedScale",
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.width(90.dp)
                        )

                        Slider(
                            value = scaleSliderValue,
                            onValueChange = { scaleSliderValue = it },
                            valueRange = 1f..6f,
                            steps = 4,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                RoundedCornerShape(8.dp)
                            )
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text("Scaled Ingredients Checklist:", fontWeight = FontWeight.Bold, fontSize = 12.sp)

                        recipe.ingredients.forEach { ing ->
                            val scaledAmount = ing.baseAmount * roundedScale
                            val formattedAmount = if (scaledAmount % 1 == 0f) scaledAmount.toInt().toString() else String.format(Locale.US, "%.1f", scaledAmount)
                            Row(modifier = Modifier.fillMaxWidth()) {
                                Text("•", color = MaterialTheme.colorScheme.secondary)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${ing.name}: $formattedAmount ${ing.unit}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Preparation Instructions:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(recipe.instructions, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action triggers
                    Button(
                        onClick = { onAddToWeeklyPlan(roundedScale.toFloat()) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("schedule_recipe_${recipe.id}"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.Black
                        )
                    ) {
                        Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Schedule meal & Auto-fill Groceries ($roundedScale servings)", fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun PantryItemRow(
    item: PantryEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onToggle,
                modifier = Modifier.testTag("grocery_item_toggle_${item.id}")
            ) {
                Icon(
                    imageVector = if (item.inStock) Icons.Default.Check else Icons.Default.Add,
                    contentDescription = "Toggle Stock Status",
                    tint = if (item.inStock) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Amount: ${if (item.quantity % 1 == 0f) item.quantity.toInt() else item.quantity} ${item.unit}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Item",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun AiFoodCameraCard(
    viewModel: LifeOsViewModel,
    modifier: Modifier = Modifier
) {
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
    }

    val imageCapture = remember { ImageCapture.Builder().build() }

    var isExpanded by remember { mutableStateOf(false) }
    val isAnalyzing by viewModel.isAnalyzingFood.collectAsState()
    val detectionResult by viewModel.aiFoodDetectionResult.collectAsState()
    val scannerError by viewModel.foodScannerError.collectAsState()

    val categories = remember {
        listOf(
            FoodCategory(
                categoryName = "Vegetarian",
                emoji = "🟢",
                items = listOf(
                    PresetFood("🍕 Pizza Margherita", "Classic thin crust Mozzarella & fresh basil", "Pizza Margherita"),
                    PresetFood("🍛 Paneer Butter Masala", "Indian cottage cheese in a buttery tomato gravy", "Paneer Butter Masala"),
                    PresetFood("🥞 Masala Dosa", "Crisp fermented lentil crepe filled with potato masala", "Masala Dosa with Chutney"),
                    PresetFood("🍄 Mushroom Risotto", "Risotto with starchy rice & earthy mushrooms", "Creamy Mushroom Risotto"),
                    PresetFood("🥘 Provençal Ratatouille", "Beautiful slow-baked zucchini, squash and tomato layers", "Provencal Ratatouille"),
                    PresetFood("🥗 Garden Salad", "Alkaline-restoring leafy greens & fresh garden vegetables", "Mixed Greens Garden Salad"),
                    PresetFood("🥣 Hearty Veggie Soup", "Comforting blend of garden root veggies & hot herbal broth", "Hearty Vegetable Soup")
                )
            ),
            FoodCategory(
                categoryName = "Non-Vegetarian",
                emoji = "🔴",
                items = listOf(
                    PresetFood("🍗 Chicken Tikka Masala", "Aromatic clay-oven grilled chicken in a cream-based curry", "Classic Curry with Basmati Rice"),
                    PresetFood("🍛 Butter Chicken", "Decadent cream and butter-infused glazed tandoori chicken", "Classic Curry with Basmati Rice"),
                    PresetFood("🍚 Chicken Biryani", "Fragrant long-grain basmati with layered meat and rich spices", "Chicken Dum Biryani"),
                    PresetFood("🥩 Sirloin Steak", "Grilled prime beef strip with roasted asparagus", "Grilled Sirloin Steak"),
                    PresetFood("🐟 Salmon Salad", "Grilled Salmon with fresh broccoli & leafy greens", "Grilled Salmon with Broccoli"),
                    PresetFood("🌮 Tacos Al Pastor", "Tasty seasoned pork corn tortillas with fresh pineapple", "Street Tacos Platter"),
                    PresetFood("🍣 Sushi Platter", "Raw salmon and tuna nigiri with sliced maki rolls", "Nigiri & Maki Sushi Platter"),
                    PresetFood("🍝 Lasagna Bolognese", "Substantial muscle-building baked lasagna layers", "Lasagna Bolognese"),
                    PresetFood("🍔 Double Burger", "Standard High Protein Beef Double Patty Cheeseburger", "Classic Double Bacon Cheeseburger")
                )
            ),
            FoodCategory(
                categoryName = "Snacks",
                emoji = "🍿",
                items = listOf(
                    PresetFood("🥟 Samosa (2 pcs)", "Crispy fried pastry stuffed with potato & peas", "Aloo Samosa (2 pcs)"),
                    PresetFood("🧀 Grilled Quesadilla", "Toasted tortilla loaded with melted Mexican cheeses", "Grilled Cheese Quesadilla"),
                    PresetFood("🧆 Falafel & Hummus", "Crispy spiced ground chickpea fritters with garlic hummus", "Falafel & Pita Basket"),
                    PresetFood("🥟 Steamed Dumplings", "Steamed pork or vegetable dumplings with dipping sauce", "Steamed Pork Dumplings"),
                    PresetFood("🍟 British Fish & Chips", "Crisp beer-battered flaky cod fillets with chunky chips", "British Fish and Chips")
                )
            ),
            FoodCategory(
                categoryName = "Breakfast & Sweets",
                emoji = "🥞",
                items = listOf(
                    PresetFood("🥑 Avocado Toast", "Ezekiel Toast with Smashed Avocado and Sunny-Side Up Eggs", "Avocado Smashed Toast with Fried Egg"),
                    PresetFood("🍳 Three-Egg Scramble", "Fluffy scrambled organic eggs with butter & chives", "Three-Egg Scrambled Plate"),
                    PresetFood("🍇 Berry Parfait", "Non-fat Greek Yogurt with Organic Berries Parfait", "Mixed Berry Greek Yogurt Parfait")
                )
            )
        )
    }

    var selectedCategoryIndex by remember { mutableIntStateOf(0) }
    var selectedItemIndex by remember { mutableIntStateOf(0) }
    val currentCategory = categories[selectedCategoryIndex]
    val currentItem = currentCategory.items.getOrNull(selectedItemIndex) ?: currentCategory.items.first()
    var selectedMealType by remember { mutableStateOf("Lunch") }
    var showSuccessToast by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(showSuccessToast) {
        if (showSuccessToast != null) {
            kotlinx.coroutines.delay(2500)
            showSuccessToast = null
        }
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "AI Lens logo",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "AI Kitchen Lens",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Instant Food & Nutrition Scanner",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = {
                        isExpanded = !isExpanded
                        if (!isExpanded) {
                            viewModel.clearFoodScanResult()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isExpanded) MaterialTheme.colorScheme.error.copy(alpha = 0.1f) else MaterialTheme.colorScheme.primary,
                        contentColor = if (isExpanded) MaterialTheme.colorScheme.error else Color.Black
                    ),
                    modifier = Modifier.testTag("toggle_food_scanner_btn")
                ) {
                    Text(
                        text = if (isExpanded) "Dismiss Scanner" else "Open AI Camera",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(14.dp))

                    // Viewfinder Screen - Displays Live Camera Preview or Permission Panel
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (hasCameraPermission) {
                            // Live output of camera feed
                            CameraPreviewView(
                                imageCapture = imageCapture,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            // Tap to authorize camera prompt
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Request permission",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = "Camera Preview is offline",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Tap to authorize live culinary lens scanner",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }

                        // Moving laser lines animation (Overlay on top of live camera preview)
                        val infiniteTransition = rememberInfiniteTransition(label = "scanner_laser")
                        val lineProgress by infiniteTransition.animateFloat(
                            initialValue = 0.05f,
                            targetValue = 0.95f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(2000, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "laser_y"
                        )
                        val reticleScale by infiniteTransition.animateFloat(
                            initialValue = 0.9f,
                            targetValue = 1.1f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1000, easing = FastOutSlowInEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "reticle_scale"
                        )

                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val cx = w / 2f
                            val cy = h / 2f

                            // Draw Radar Circles
                            drawCircle(
                                color = Color(0xFF1DB954).copy(alpha = 0.05f),
                                radius = cx * 0.4f
                            )
                            drawCircle(
                                color = Color(0xFF1DB954).copy(alpha = 0.1f * reticleScale),
                                radius = 25.dp.toPx() * reticleScale,
                                style = Stroke(width = 1.5.dp.toPx())
                            )
                            drawCircle(
                                color = Color(0xFF1DB954).copy(alpha = 0.4f),
                                radius = 4.dp.toPx(),
                                style = Fill
                            )

                            // Crosshairs
                            drawLine(
                                color = Color(0xFF1DB954).copy(alpha = 0.2f),
                                start = Offset(0f, cy),
                                end = Offset(w, cy),
                                strokeWidth = 1f
                            )
                            drawLine(
                                color = Color(0xFF1DB954).copy(alpha = 0.2f),
                                start = Offset(cx, 0f),
                                end = Offset(cx, h),
                                strokeWidth = 1f
                            )

                            // Moving scanline
                            val scanY = h * lineProgress
                            drawLine(
                                color = Color(0xFF1DB954).copy(alpha = 0.8f),
                                start = Offset(0f, scanY),
                                end = Offset(w, scanY),
                                strokeWidth = 2.dp.toPx()
                            )
                        }

                        // HUD Telemetry Overlays
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                        ) {
                            // Recording Indicator
                            Row(
                                modifier = Modifier.align(Alignment.TopStart),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val blinkAlpha by rememberInfiniteTransition(label = "blink_red").animateFloat(
                                    initialValue = 0.2f,
                                    targetValue = 1.0f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(600, easing = LinearEasing),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "blink_alpha"
                                )
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(
                                            if (isAnalyzing) Color.Red.copy(alpha = blinkAlpha) else Color(0xFF1DB954).copy(alpha = blinkAlpha),
                                            CircleShape
                                        )
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isAnalyzing) "COMPUTING QUANTUM ELEMENTS..." else "AI CAM ONLINE [PPGv2]",
                                    color = if (isAnalyzing) Color.Red else Color(0xFF1DB954),
                                    fontSize = 8.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            // Viewfinder telemetry data
                            Text(
                                text = "ISO 400 | EXP: AUTO\nFOCUS: MULTI-POINT",
                                color = Color(0xFF1DB954).copy(alpha = 0.6f),
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.align(Alignment.TopEnd)
                            )

                            Text(
                                text = "PRESET: ${currentItem.displayName.uppercase(Locale.US)}",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Category Selector Carousel
                    Text(
                        text = "Browse Culinary Categories:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        itemsIndexed(categories) { idx, cat ->
                            val isSelected = selectedCategoryIndex == idx
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
                                    )
                                    .clickable { 
                                        selectedCategoryIndex = idx
                                        selectedItemIndex = 0 // Reset item to first when changing category
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "${cat.emoji} ${cat.categoryName}",
                                    color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Dishes inside the active category
                    Text(
                        text = "Or Aim Camera at a preset dish in ${currentCategory.emoji} ${currentCategory.categoryName}:",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        itemsIndexed(currentCategory.items) { idx, item ->
                            val isSelected = selectedItemIndex == idx
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { selectedItemIndex = idx }
                                    .padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = item.displayName,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Snap trigger trigger
                    Button(
                        onClick = {
                            if (!hasCameraPermission) {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            } else {
                                try {
                                    // Set state so user sees loading instantly
                                    imageCapture.takePicture(
                                        ContextCompat.getMainExecutor(context),
                                        object : ImageCapture.OnImageCapturedCallback() {
                                            override fun onCaptureSuccess(image: ImageProxy) {
                                                val base64 = imageProxyToBase64(image)
                                                image.close()
                                                if (base64 != null) {
                                                    viewModel.analyzeFoodImage(base64, currentItem.queryName)
                                                } else {
                                                    viewModel.analyzeFoodImage("", currentItem.queryName)
                                                }
                                            }

                                            override fun onError(exception: ImageCaptureException) {
                                                Log.e("AiFoodCameraCard", "Capture failed: ${exception.message}", exception)
                                                viewModel.analyzeFoodImage("", currentItem.queryName)
                                            }
                                        }
                                    )
                                } catch (e: Exception) {
                                    Log.e("AiFoodCameraCard", "Failed to start capture: ${e.message}", e)
                                    viewModel.analyzeFoodImage("", currentItem.queryName)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("snap_food_detector_btn"),
                        enabled = !isAnalyzing
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "shutter",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isAnalyzing) "Scanning Food Elements with AI..." else "SNAP PHOTO & DETECT NUTRIENTS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    // Analyzing Loading view
                    if (isAnalyzing) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Analyzing molecular weight, carbon chains and mineral loads...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // Scanner error/offline log warning banner
                    scannerError?.let { err ->
                        Spacer(modifier = Modifier.height(14.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = "error", tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(err, fontSize = 9.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        }
                    }

                    // Scanned results sheet
                    detectionResult?.let { res ->
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                            modifier = Modifier.fillMaxWidth().testTag("food_scanned_details_panel")
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                // Scanned Item name header
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = res.foodName,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Estimated Portion: ${res.portionSize}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    // Match confidence badge
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                                            .padding(horizontal = 6.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "${(res.confidence * 100).toInt()}% Match",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Dynamic Macros badge sheet
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    MacroPillBadge("Calories", "${res.calories} kcal", MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                                    MacroPillBadge("Protein", "${res.protein}g", MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), MaterialTheme.colorScheme.primary, modifier = Modifier.weight(1f))
                                    MacroPillBadge("Carbs", "${res.carbs}g", MaterialTheme.colorScheme.secondary.copy(alpha = 0.08f), MaterialTheme.colorScheme.secondary, modifier = Modifier.weight(1f))
                                    MacroPillBadge("Fat", "${res.fat}g", MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f), MaterialTheme.colorScheme.tertiary, modifier = Modifier.weight(1f))
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                // Minerals
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                        .padding(10.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.secondary)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("MINERALS COMPOSITION CALCULATED:", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = res.minerals,
                                        fontSize = 10.sp,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Health coach intelligence card
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Star,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp).padding(top = 2.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = res.healthInsight,
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Log Scanned Nutrition into Intake database!
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Select meal target:", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        listOf("Breakfast", "Lunch", "Dinner", "Snack").forEach { type ->
                                            val isTypeSelected = selectedMealType == type
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(
                                                        if (isTypeSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                                                    )
                                                    .clickable { selectedMealType = type }
                                                    .padding(vertical = 8.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = type,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isTypeSelected) Color.Black else MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Button(
                                        onClick = {
                                            viewModel.logScannedMealAsIntake(res, selectedMealType)
                                            showSuccessToast = "Logged ${res.foodName} (${res.calories} kcal) to ${selectedMealType}!"
                                            viewModel.clearFoodScanResult()
                                            isExpanded = false
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = Color.Black
                                        ),
                                        modifier = Modifier.fillMaxWidth().testTag("save_scanned_meal_btn")
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("LOG TO SMART KITCHEN INTAKE", fontSize = 11.sp, fontWeight = FontWeight.Black)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // High tactility log success alert banner
            showSuccessToast?.let { msg ->
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "success", tint = Color.Black)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = msg,
                            color = Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

data class PresetFood(
    val displayName: String,
    val description: String,
    val queryName: String
)

data class FoodCategory(
    val categoryName: String,
    val emoji: String,
    val items: List<PresetFood>
)

@Composable
fun MacroPillBadge(
    label: String,
    value: String,
    bg: Color,
    txtColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = label, fontSize = 8.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, fontSize = 11.sp, fontWeight = FontWeight.Black, color = txtColor)
    }
}

@Composable
fun CameraPreviewView(
    imageCapture: ImageCapture,
    modifier: Modifier = Modifier,
    onCameraReady: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    AndroidView(
        factory = { ctx ->
            PreviewView(ctx).apply {
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }
        },
        modifier = modifier,
        update = { previewView ->
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = CameraXPreview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    // Unbind use cases before rebinding
                    cameraProvider.unbindAll()

                    // Bind use cases to camera
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                    onCameraReady()
                } catch (exc: Exception) {
                    Log.e("CameraPreviewView", "Use case binding failed", exc)
                }
            }, ContextCompat.getMainExecutor(context))
        }
    )
}

fun imageProxyToBase64(image: ImageProxy): String? {
    return try {
        val plane = image.planes[0]
        val buffer = plane.buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        if (bitmap != null) {
            // Rotate back to match camera viewport orientation if wider than tall
            val rotatedBitmap = if (bitmap.width > bitmap.height) {
                val matrix = android.graphics.Matrix().apply { postRotate(90f) }
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } else {
                bitmap
            }
            val scaledBitmap = Bitmap.createScaledBitmap(rotatedBitmap, 480, 480, true)
            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            val compressedBytes = outputStream.toByteArray()
            Base64.encodeToString(compressedBytes, Base64.NO_WRAP)
        } else {
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        }
    } catch (e: Exception) {
        Log.e("CameraCapture", "Failed to convert image proxy to base64: ${e.message}")
        null
    }
}

