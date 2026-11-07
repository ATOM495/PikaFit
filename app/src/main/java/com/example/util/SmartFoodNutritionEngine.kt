package com.example.util

import com.example.data.model.ExerciseLogged
import com.example.data.model.HunterBiometrics
import kotlin.math.roundToInt

data class FoodPer100gProfile(
    val keywords: List<String>,
    val displayName: String,
    val description: String,
    val caloriesPer100g: Int,
    val carbsPer100g: Double,
    val fatsPer100g: Double,
    val proteinPer100g: Double,
    val fiberPer100g: Double,
    val micronutrients: String,
    val pairingTip: String,
    val gramsPerUnit: Int = 100,
    val unitNameSingular: String = "serving",
    val unitNamePlural: String = "servings"
)

data class CalculatedFoodNutrition(
    val foodName: String,
    val grams: Int,
    val quantityCount: Int = 1,
    val isQuantityBased: Boolean = false,
    val servingLabel: String = "${grams}g",
    val description: String,
    val calories: Int,
    val carbsG: Double,
    val fatsG: Double,
    val proteinG: Double,
    val fiberG: Double,
    val micronutrients: String,
    val pairingTip: String
)

data class ExerciseCalorieBurnItem(
    val exerciseName: String,
    val sets: Int,
    val reps: String,
    val caloriesBurned: Int,
    val intensityTag: String
)

data class RecommendedWorkoutMeal(
    val timingTitle: String,
    val mealName: String,
    val portionDescription: String,
    val calories: Int,
    val proteinG: Double,
    val carbsG: Double,
    val fatsG: Double
)

data class WorkoutBurnAndDietSummary(
    val exerciseBreakdown: List<ExerciseCalorieBurnItem>,
    val totalWorkoutBurnKcal: Int,
    val totalSetsCompleted: Int,
    val baseDailyTargetKcal: Int,
    val burnAdjustedTargetKcal: Int,
    val caloriesLoggedToday: Int,
    val remainingCaloriesNeeded: Int,
    val goalModeTitle: String,
    val surplusOrDeficitHeadline: String,
    val surplusOrDeficitAdvice: String,
    val recommendedMeals: List<RecommendedWorkoutMeal>,
    val dailyDietProtocolBullets: List<String>
)

object SmartFoodNutritionEngine {

    private val database: List<FoodPer100gProfile> = listOf(
        FoodPer100gProfile(
            keywords = listOf("green tea", "black tea", "herbal tea", "matcha"),
            displayName = "Green / Black Tea (Unsweetened)",
            description = "Zero-calorie antioxidant beverage packed with catechins (EGCG) and L-theanine that enhances fat oxidation, metabolic rate, and calm focus.",
            caloriesPer100g = 2,
            carbsPer100g = 0.3,
            fatsPer100g = 0.0,
            proteinPer100g = 0.2,
            fiberPer100g = 0.0,
            micronutrients = "EGCG Catechins, L-Theanine, Polyphenols, Manganese, ~25mg Caffeine",
            pairingTip = "Drink 30 mins before workouts to boost fat mobilization and mental sharpness.",
            gramsPerUnit = 150,
            unitNameSingular = "cup",
            unitNamePlural = "cups"
        ),
        FoodPer100gProfile(
            keywords = listOf("tea", "chai", "masala tea", "milk tea"),
            displayName = "Tea (Brewed with Milk)",
            description = "Refreshing brewed beverage rich in L-theanine, polyphenols, and dairy amino acids that boosts alertness, focus, and antioxidant defense.",
            caloriesPer100g = 62,
            carbsPer100g = 7.8,
            fatsPer100g = 2.2,
            proteinPer100g = 2.4,
            fiberPer100g = 0.0,
            micronutrients = "L-Theanine, Catechins, Calcium (68mg), Potassium (95mg), Vitamin B2",
            pairingTip = "Pair with high-protein snacks like boiled eggs, roasted chana, or almonds; keep 45 mins away from heavy iron meals.",
            gramsPerUnit = 150,
            unitNameSingular = "cup",
            unitNamePlural = "cups"
        ),
        FoodPer100gProfile(
            keywords = listOf("black coffee", "espresso", "americano"),
            displayName = "Black Coffee",
            description = "Potent ergogenic pre-workout beverage rich in chlorogenic acid and caffeine that increases endurance, strength output, and thermogenesis.",
            caloriesPer100g = 2,
            carbsPer100g = 0.0,
            fatsPer100g = 0.1,
            proteinPer100g = 0.3,
            fiberPer100g = 0.0,
            micronutrients = "Caffeine (~40mg/100ml), Chlorogenic Acid, Potassium, Magnesium, Niacin (B3)",
            pairingTip = "Ideal 30–45 mins before resistance training alongside a banana for peak gym performance.",
            gramsPerUnit = 150,
            unitNameSingular = "cup",
            unitNamePlural = "cups"
        ),
        FoodPer100gProfile(
            keywords = listOf("coffee", "latte", "cappuccino", "filter coffee"),
            displayName = "Milk Coffee",
            description = "Energizing coffee blended with milk providing natural caffeine for central nervous system drive along with dairy protein and calcium.",
            caloriesPer100g = 68,
            carbsPer100g = 6.5,
            fatsPer100g = 2.8,
            proteinPer100g = 3.1,
            fiberPer100g = 0.0,
            micronutrients = "Caffeine, Chlorogenic Antioxidants, Calcium (90mg), Vitamin B12, Riboflavin",
            pairingTip = "Enjoy as a morning or pre-workout pick-me-up with minimal added sugar.",
            gramsPerUnit = 150,
            unitNameSingular = "cup",
            unitNamePlural = "cups"
        ),
        FoodPer100gProfile(
            keywords = listOf("idli", "idly", "idlies", "idlys", "rava idli"),
            displayName = "Steamed Idli",
            description = "Light, gut-friendly steamed fermented rice & lentil cake where fermentation increases Vitamin B availability and combines rice + urad dal amino acids.",
            caloriesPer100g = 146,
            carbsPer100g = 30.2,
            fatsPer100g = 0.4,
            proteinPer100g = 4.6,
            fiberPer100g = 2.1,
            micronutrients = "Probiotics, Folate, Iron, Vitamin B1, B2, Magnesium",
            pairingTip = "Eat 3–4 idlis with thick lentil sambar and 2 boiled eggs for a high-protein breakfast.",
            gramsPerUnit = 45,
            unitNameSingular = "idli",
            unitNamePlural = "idlis"
        ),
        FoodPer100gProfile(
            keywords = listOf("masala dosa", "paneer dosa", "egg dosa", "ghee roast", "onion dosa"),
            displayName = "Masala / Stuffed Dosa",
            description = "Crispy fermented rice and lentil crepe filled with spiced potato/protein masala for high energy and workout fuel.",
            caloriesPer100g = 205,
            carbsPer100g = 33.5,
            fatsPer100g = 5.8,
            proteinPer100g = 5.6,
            fiberPer100g = 2.4,
            micronutrients = "Iron, Folate, Potassium, Vitamin B1, Riboflavin",
            pairingTip = "Pair with sambar and roasted chana chutney or 2 boiled eggs to boost protein.",
            gramsPerUnit = 130,
            unitNameSingular = "dosa",
            unitNamePlural = "dosas"
        ),
        FoodPer100gProfile(
            keywords = listOf("dosa", "dosai", "dosas", "plain dosa", "rava dosa", "pesarattu", "set dosa", "uthappam", "uttapam"),
            displayName = "Dosa (Fermented Crepe)",
            description = "Crispy fermented rice and black gram crepe offering fast-digesting carbohydrates and gut-supporting fermented nutrients.",
            caloriesPer100g = 184,
            carbsPer100g = 31.8,
            fatsPer100g = 3.8,
            proteinPer100g = 5.2,
            fiberPer100g = 1.9,
            micronutrients = "Iron, Folate, Calcium, Thiamine (B1), Riboflavin",
            pairingTip = "Upgrade to an Egg Dosa, Paneer Dosa, or Moong Dal Pesarattu to double the protein content.",
            gramsPerUnit = 85,
            unitNameSingular = "dosa",
            unitNamePlural = "dosas"
        ),
        FoodPer100gProfile(
            keywords = listOf("vada", "medu vada", "vadai", "sambar vada", "dal vada"),
            displayName = "Medu Vada (Lentil Donut)",
            description = "Savory black gram (urad dal) fritter providing plant protein, iron, and crunchy energy.",
            caloriesPer100g = 285,
            carbsPer100g = 31.0,
            fatsPer100g = 13.5,
            proteinPer100g = 9.5,
            fiberPer100g = 3.8,
            micronutrients = "Iron, Folate, Magnesium, Potassium",
            pairingTip = "Pair 1–2 vadas with steamed idlis and sambar to balance fats with steamed carbs.",
            gramsPerUnit = 50,
            unitNameSingular = "vada",
            unitNamePlural = "vadas"
        ),
        FoodPer100gProfile(
            keywords = listOf("puri", "poori", "chole bhature", "bhatura"),
            displayName = "Whole Wheat Poori",
            description = "Puffed whole-wheat bread providing quick energy and satiating fats.",
            caloriesPer100g = 340,
            carbsPer100g = 46.0,
            fatsPer100g = 14.0,
            proteinPer100g = 7.5,
            fiberPer100g = 3.5,
            micronutrients = "Iron, Thiamine (B1), Niacin (B3)",
            pairingTip = "Pair with high-protein chickpea masala (chana) and cucumber salad.",
            gramsPerUnit = 35,
            unitNameSingular = "poori",
            unitNamePlural = "pooris"
        ),
        FoodPer100gProfile(
            keywords = listOf("chapati", "roti", "rotis", "chapatis", "phulka", "wheat bread"),
            displayName = "Whole Wheat Chapati / Roti",
            description = "Traditional whole-wheat flatbread rich in complex carbohydrates, insoluble dietary fiber, and B-vitamins for sustained daily energy.",
            caloriesPer100g = 297,
            carbsPer100g = 61.8,
            fatsPer100g = 1.7,
            proteinPer100g = 9.8,
            fiberPer100g = 9.2,
            micronutrients = "Magnesium, Iron, Phosphorus, Vitamin B1, B3, Zinc",
            pairingTip = "2–3 medium chapatis pair perfectly with dal, paneer, egg bhurji, or grilled chicken.",
            gramsPerUnit = 40,
            unitNameSingular = "roti",
            unitNamePlural = "rotis"
        ),
        FoodPer100gProfile(
            keywords = listOf("paratha", "parotta", "aloo paratha", "paneer paratha", "gobi paratha"),
            displayName = "Stuffed Whole Wheat Paratha",
            description = "Hearty stuffed flatbread providing dense calories, complex carbs, and satiating fats — well-suited for active mornings or muscle-building surpluses.",
            caloriesPer100g = 260,
            carbsPer100g = 38.0,
            fatsPer100g = 8.5,
            proteinPer100g = 7.5,
            fiberPer100g = 4.2,
            micronutrients = "Iron, Potassium, Vitamin B1, Calcium, Phosphorus",
            pairingTip = "Pair 1–2 parathas with 150g low-fat curd/Greek yogurt rather than heavy butter.",
            gramsPerUnit = 90,
            unitNameSingular = "paratha",
            unitNamePlural = "parathas"
        ),
        FoodPer100gProfile(
            keywords = listOf("chicken breast", "grilled chicken", "boiled chicken", "chicken"),
            displayName = "Chicken Breast (Cooked)",
            description = "Gold-standard lean animal protein with high L-leucine content and complete essential amino acids for rapid muscle hypertrophy and fat loss.",
            caloriesPer100g = 165,
            carbsPer100g = 0.0,
            fatsPer100g = 3.6,
            proteinPer100g = 31.0,
            fiberPer100g = 0.0,
            micronutrients = "Niacin (Vitamin B3), Vitamin B6, Selenium, Phosphorus, Choline",
            pairingTip = "Combine 150g–200g chicken breast with steamed rice or roti and vegetables for an ideal post-workout meal.",
            gramsPerUnit = 150,
            unitNameSingular = "breast piece",
            unitNamePlural = "breast pieces"
        ),
        FoodPer100gProfile(
            keywords = listOf("chicken curry", "butter chicken", "chicken masala"),
            displayName = "Chicken Curry (Home Style)",
            description = "Savory spiced poultry dish providing high-quality complete protein alongside healthy fats and anti-inflammatory turmeric and ginger compounds.",
            caloriesPer100g = 185,
            carbsPer100g = 4.8,
            fatsPer100g = 9.5,
            proteinPer100g = 20.5,
            fiberPer100g = 1.1,
            micronutrients = "Vitamin B6, B12, Curcumin, Iron, Zinc, Selenium",
            pairingTip = "Pair 150g–200g chicken curry with 2 whole wheat chapatis or 150g steamed basmati rice.",
            gramsPerUnit = 180,
            unitNameSingular = "bowl",
            unitNamePlural = "bowls"
        ),
        FoodPer100gProfile(
            keywords = listOf("egg white", "egg whites"),
            displayName = "Egg Whites",
            description = "Pure, zero-fat, zero-cholesterol albumin protein with 100% bioavailability — ideal for hitting high protein targets on a low-calorie budget.",
            caloriesPer100g = 52,
            carbsPer100g = 0.7,
            fatsPer100g = 0.2,
            proteinPer100g = 10.9,
            fiberPer100g = 0.0,
            micronutrients = "Selenium, Riboflavin (Vitamin B2), Potassium, Branched-Chain Amino Acids (BCAAs)",
            pairingTip = "Combine 4–5 egg whites with 1–2 whole eggs for maximum protein with balanced micronutrients.",
            gramsPerUnit = 33,
            unitNameSingular = "egg white",
            unitNamePlural = "egg whites"
        ),
        FoodPer100gProfile(
            keywords = listOf("egg", "eggs", "boiled egg", "omelette", "scrambled egg"),
            displayName = "Whole Egg (Boiled / Cooked)",
            description = "Nature's reference standard for protein biological value (100 BV), supplying complete amino acids, choline for neuro-muscular drive, and healthy fats.",
            caloriesPer100g = 155,
            carbsPer100g = 1.1,
            fatsPer100g = 10.6,
            proteinPer100g = 12.6,
            fiberPer100g = 0.0,
            micronutrients = "Choline (294mg), Vitamin D3, Vitamin B12, Selenium, Lutein & Zeaxanthin",
            pairingTip = "Eat 2–4 whole eggs at breakfast or post-workout with whole-grain toast, idli, or oats.",
            gramsPerUnit = 50,
            unitNameSingular = "egg",
            unitNamePlural = "eggs"
        ),
        FoodPer100gProfile(
            keywords = listOf("paneer", "cottage cheese"),
            displayName = "Paneer (Cottage Cheese)",
            description = "Slow-digesting vegetarian casein protein powerhouse rich in calcium and conjugated linoleic acid (CLA) for sustained muscle synthesis.",
            caloriesPer100g = 265,
            carbsPer100g = 3.4,
            fatsPer100g = 20.8,
            proteinPer100g = 18.3,
            fiberPer100g = 0.0,
            micronutrients = "Calcium (480mg), Phosphorus, Vitamin B12, Selenium, Riboflavin",
            pairingTip = "Grill or stir-fry 100g–150g paneer with bell peppers and spinach for a high-protein vegetarian meal.",
            gramsPerUnit = 100,
            unitNameSingular = "serving (100g)",
            unitNamePlural = "servings"
        ),
        FoodPer100gProfile(
            keywords = listOf("tofu", "soy paneer"),
            displayName = "Firm Tofu (Soy Bean Curd)",
            description = "Lean, low-calorie complete plant protein containing all 9 essential amino acids, calcium, and antioxidant isoflavones.",
            caloriesPer100g = 144,
            carbsPer100g = 2.8,
            fatsPer100g = 8.7,
            proteinPer100g = 15.7,
            fiberPer100g = 2.3,
            micronutrients = "Calcium (350mg), Iron (2.7mg), Magnesium, Zinc, Soy Isoflavones",
            pairingTip = "Scramble 150g tofu with turmeric, black pepper, and veggies or add to rice bowls.",
            gramsPerUnit = 100,
            unitNameSingular = "serving (100g)",
            unitNamePlural = "servings"
        ),
        FoodPer100gProfile(
            keywords = listOf("soya", "soya chunks", "soy chunks", "meal maker"),
            displayName = "Soya Chunks (Dry Weight)",
            description = "Ultra-concentrated vegetarian protein source delivering 52g protein per 100g dry weight with rich dietary fiber and iron.",
            caloriesPer100g = 345,
            carbsPer100g = 33.0,
            fatsPer100g = 0.5,
            proteinPer100g = 52.0,
            fiberPer100g = 13.0,
            micronutrients = "Iron (20mg), Calcium (350mg), Folate, Magnesium, Potassium",
            pairingTip = "Boil 50g dry soya chunks (~26g protein) and sauté with onions, tomatoes, and spices.",
            gramsPerUnit = 50,
            unitNameSingular = "cup (50g dry)",
            unitNamePlural = "cups"
        ),
        FoodPer100gProfile(
            keywords = listOf("brown rice", "red rice"),
            displayName = "Cooked Brown Rice",
            description = "Whole-grain complex carbohydrate with intact bran fiber and magnesium that provides steady, long-lasting energy without sharp insulin spikes.",
            caloriesPer100g = 112,
            carbsPer100g = 23.5,
            fatsPer100g = 0.9,
            proteinPer100g = 2.6,
            fiberPer100g = 1.8,
            micronutrients = "Magnesium, Manganese, Selenium, Thiamine (B1), Niacin (B3)",
            pairingTip = "Pair 150g–200g brown rice with dal, rajma, chicken, or fish for balanced lunch macros.",
            gramsPerUnit = 180,
            unitNameSingular = "bowl",
            unitNamePlural = "bowls"
        ),
        FoodPer100gProfile(
            keywords = listOf("rice", "white rice", "basmati rice", "jasmine rice", "steamed rice"),
            displayName = "Cooked White Rice",
            description = "Clean, easily digestible carbohydrate source that rapidly replenishes skeletal muscle glycogen stores before and after workouts.",
            caloriesPer100g = 130,
            carbsPer100g = 28.2,
            fatsPer100g = 0.3,
            proteinPer100g = 2.7,
            fiberPer100g = 0.4,
            micronutrients = "Manganese, Folate (B9), Thiamine (B1), Selenium",
            pairingTip = "Combine 150g–200g cooked rice with 150g lean protein (chicken, fish, eggs, or dal + curd) to slow glycemic impact.",
            gramsPerUnit = 180,
            unitNameSingular = "bowl",
            unitNamePlural = "bowls"
        ),
        FoodPer100gProfile(
            keywords = listOf("biryani", "chicken biryani", "mutton biryani", "veg biryani", "pulao"),
            displayName = "Biryani (Spiced Rice & Protein)",
            description = "Energy-dense complete meal combining aromatic basmati rice, protein, and spices — great for post-workout glycogen & calorie goals when portion-controlled.",
            caloriesPer100g = 178,
            carbsPer100g = 22.4,
            fatsPer100g = 6.2,
            proteinPer100g = 8.4,
            fiberPer100g = 1.2,
            micronutrients = "Vitamin B12, Iron, Zinc, Niacin, Curcuminoids",
            pairingTip = "Add extra boiled egg or grilled chicken + cucumber raita alongside 200g–250g biryani to boost protein ratio.",
            gramsPerUnit = 250,
            unitNameSingular = "plate",
            unitNamePlural = "plates"
        ),
        FoodPer100gProfile(
            keywords = listOf("dal", "lentil", "lentils", "moong dal", "toor dal", "masoor dal", "sambar"),
            displayName = "Cooked Dal / Sambar",
            description = "Nutrient-dense legume staple rich in lysine, soluble prebiotic fiber, folate, and plant iron for cardiovascular and muscular health.",
            caloriesPer100g = 116,
            carbsPer100g = 20.1,
            fatsPer100g = 0.4,
            proteinPer100g = 9.0,
            fiberPer100g = 7.9,
            micronutrients = "Folate (181mcg), Iron (3.3mg), Potassium, Manganese, Phosphorus",
            pairingTip = "Combine 1–2 bowls thick dal/sambar with idli, dosa, rice, or roti for complete amino acids.",
            gramsPerUnit = 150,
            unitNameSingular = "bowl",
            unitNamePlural = "bowls"
        ),
        FoodPer100gProfile(
            keywords = listOf("chana", "chickpea", "chickpeas", "chole", "kala chana", "sprouts", "sundal"),
            displayName = "Cooked Chickpeas / Chana",
            description = "Slow-digesting low-GI legume loaded with plant protein, resistant starch, and fiber that keeps hunger suppressed for hours.",
            caloriesPer100g = 164,
            carbsPer100g = 27.4,
            fatsPer100g = 2.6,
            proteinPer100g = 8.9,
            fiberPer100g = 7.6,
            micronutrients = "Iron, Folate, Manganese, Copper, Zinc, Magnesium",
            pairingTip = "Enjoy 1 bowl boiled chana chaat with onion, tomato, lemon, and cucumber as a high-protein snack.",
            gramsPerUnit = 150,
            unitNameSingular = "bowl",
            unitNamePlural = "bowls"
        ),
        FoodPer100gProfile(
            keywords = listOf("rajma", "kidney beans", "beans", "black beans"),
            displayName = "Cooked Rajma (Kidney Beans)",
            description = "Fiber-rich complex carbohydrate and plant protein powerhouse packed with potassium, iron, and anthocyanin antioxidants.",
            caloriesPer100g = 127,
            carbsPer100g = 22.8,
            fatsPer100g = 0.5,
            proteinPer100g = 8.7,
            fiberPer100g = 6.4,
            micronutrients = "Iron, Potassium (403mg), Folate, Magnesium, Molybdenum",
            pairingTip = "Classic Rajma + Rice + 100g curd delivers a complete amino acid spectrum.",
            gramsPerUnit = 150,
            unitNameSingular = "bowl",
            unitNamePlural = "bowls"
        ),
        FoodPer100gProfile(
            keywords = listOf("oats", "oatmeal", "rolled oats", "porridge", "masala oats"),
            displayName = "Rolled Oats",
            description = "Premier endurance & bodybuilding grain rich in beta-glucan soluble fiber, avenanthramide antioxidants, and sustained-release complex carbs.",
            caloriesPer100g = 389,
            carbsPer100g = 66.3,
            fatsPer100g = 6.9,
            proteinPer100g = 16.9,
            fiberPer100g = 10.6,
            micronutrients = "Beta-Glucan Fiber, Manganese, Phosphorus, Magnesium, Iron, Zinc",
            pairingTip = "Cook 1 bowl (60g–80g dry) oats with milk/water and stir in 1 scoop whey protein or 2 boiled eggs.",
            gramsPerUnit = 60,
            unitNameSingular = "bowl (60g)",
            unitNamePlural = "bowls"
        ),
        FoodPer100gProfile(
            keywords = listOf("banana", "bananas", "plantain"),
            displayName = "Ripe Banana",
            description = "Fast-acting natural energy fruit supplying dual-transport carbohydrates (glucose + fructose), vitamin B6, and cramp-preventing potassium.",
            caloriesPer100g = 89,
            carbsPer100g = 22.8,
            fatsPer100g = 0.3,
            proteinPer100g = 1.1,
            fiberPer100g = 2.6,
            micronutrients = "Potassium (358mg), Vitamin B6 (0.4mg), Vitamin C, Magnesium",
            pairingTip = "Eat 1–2 medium bananas 30 mins before training or blend into a post-workout protein shake.",
            gramsPerUnit = 115,
            unitNameSingular = "banana",
            unitNamePlural = "bananas"
        ),
        FoodPer100gProfile(
            keywords = listOf("apple", "green apple"),
            displayName = "Fresh Apple",
            description = "High-satiety, low-calorie fruit rich in pectin soluble fiber and quercetin polyphenols that support gut health and cardiovascular stamina.",
            caloriesPer100g = 52,
            carbsPer100g = 13.8,
            fatsPer100g = 0.2,
            proteinPer100g = 0.3,
            fiberPer100g = 2.4,
            micronutrients = "Vitamin C, Quercetin, Pectin Fiber, Potassium",
            pairingTip = "Slice 1 apple and pair with 1 tbsp peanut butter or 150g Greek yogurt for a balanced snack.",
            gramsPerUnit = 160,
            unitNameSingular = "apple",
            unitNamePlural = "apples"
        ),
        FoodPer100gProfile(
            keywords = listOf("mango", "orange", "papaya", "watermelon", "pomegranate", "guava", "pineapple", "berries", "fruit"),
            displayName = "Fresh Seasonal Fruit",
            description = "Micronutrient-rich natural carbohydrate source loaded with Vitamin C, cellular hydration electrolytes, and recovery antioxidants.",
            caloriesPer100g = 60,
            carbsPer100g = 14.5,
            fatsPer100g = 0.3,
            proteinPer100g = 0.8,
            fiberPer100g = 2.2,
            micronutrients = "Vitamin C, Vitamin A, Potassium, Lycopene / Anthocyanins, Folate",
            pairingTip = "Include 1 bowl fresh fruit post-workout or with morning yogurt to replenish liver glycogen.",
            gramsPerUnit = 150,
            unitNameSingular = "bowl",
            unitNamePlural = "bowls"
        ),
        FoodPer100gProfile(
            keywords = listOf("milk", "cow milk", "buffalo milk", "toned milk", "skim milk"),
            displayName = "Milk (Whole / Toned)",
            description = "Complete liquid nutrition providing an 80:20 casein-to-whey protein blend, natural electrolytes, and bioavailable calcium for bone and muscle strength.",
            caloriesPer100g = 61,
            carbsPer100g = 4.8,
            fatsPer100g = 3.3,
            proteinPer100g = 3.2,
            fiberPer100g = 0.0,
            micronutrients = "Calcium (120mg), Vitamin B12, Vitamin D, Riboflavin, Phosphorus, Potassium",
            pairingTip = "1 glass of milk (250ml = ~152 kcal, 8g protein) is great with oats or post-workout whey.",
            gramsPerUnit = 250,
            unitNameSingular = "glass (250ml)",
            unitNamePlural = "glasses"
        ),
        FoodPer100gProfile(
            keywords = listOf("greek yogurt", "hung curd", "skyr"),
            displayName = "Greek Yogurt / Hung Curd",
            description = "Strained high-protein probiotic dairy delivering double the protein and half the sugar of regular curd for lean muscle maintenance.",
            caloriesPer100g = 73,
            carbsPer100g = 3.8,
            fatsPer100g = 1.9,
            proteinPer100g = 10.2,
            fiberPer100g = 0.0,
            micronutrients = "Live Probiotics, Calcium, Vitamin B12, Iodine, Phosphorus",
            pairingTip = "Top 1 cup Greek yogurt with berries, chia seeds, and a drizzle of honey.",
            gramsPerUnit = 150,
            unitNameSingular = "cup",
            unitNamePlural = "cups"
        ),
        FoodPer100gProfile(
            keywords = listOf("curd", "yogurt", "dahi", "buttermilk", "chaas", "lassi"),
            displayName = "Curd / Dahi (Natural Yogurt)",
            description = "Cooling probiotic fermented dairy that aids protein digestion, gut flora balance, and calcium absorption.",
            caloriesPer100g = 61,
            carbsPer100g = 4.7,
            fatsPer100g = 3.3,
            proteinPer100g = 3.5,
            fiberPer100g = 0.0,
            micronutrients = "Lactobacillus Probiotics, Calcium, Vitamin B12, Phosphorus, Potassium",
            pairingTip = "Include 1 bowl curd alongside lunch or dinner to improve digestion and add +5.2g protein.",
            gramsPerUnit = 150,
            unitNameSingular = "bowl",
            unitNamePlural = "bowls"
        ),
        FoodPer100gProfile(
            keywords = listOf("whey", "whey protein", "protein shake", "protein powder", "isolate"),
            displayName = "Whey Protein Powder",
            description = "Fastest-digesting high-leucine protein (~11% leucine) that spikes blood amino acid levels within 30 minutes to trigger mTOR muscle synthesis.",
            caloriesPer100g = 380,
            carbsPer100g = 7.5,
            fatsPer100g = 4.5,
            proteinPer100g = 78.0,
            fiberPer100g = 0.5,
            micronutrients = "L-Leucine (8.5g/100g), Isoleucine, Valine, Calcium, Immunoglobulins",
            pairingTip = "1 standard scoop (32g = ~122 kcal, 25g protein) mixed with water or milk immediately post-workout.",
            gramsPerUnit = 32,
            unitNameSingular = "scoop",
            unitNamePlural = "scoops"
        ),
        FoodPer100gProfile(
            keywords = listOf("fish", "salmon", "tuna", "rohu", "surmai", "tilapia", "cod", "prawns", "shrimp"),
            displayName = "Fish / Seafood (Cooked)",
            description = "High-protein, easily digestible marine nutrition packed with anti-inflammatory EPA & DHA Omega-3 fatty acids for joint and heart health.",
            caloriesPer100g = 172,
            carbsPer100g = 0.0,
            fatsPer100g = 7.4,
            proteinPer100g = 24.8,
            fiberPer100g = 0.0,
            micronutrients = "Omega-3 (EPA & DHA), Vitamin D3, Vitamin B12, Selenium, Iodine",
            pairingTip = "Grill or pan-sear 1–2 fish fillets with lemon, garlic, and steamed rice.",
            gramsPerUnit = 140,
            unitNameSingular = "fillet",
            unitNamePlural = "fillets"
        ),
        FoodPer100gProfile(
            keywords = listOf("mutton", "lamb", "goat", "beef", "steak", "red meat"),
            displayName = "Lean Red Meat (Mutton / Beef, Cooked)",
            description = "Dense anabolic protein naturally rich in intramuscular creatine, heme iron, zinc, and Vitamin B12 for strength and testosterone support.",
            caloriesPer100g = 215,
            carbsPer100g = 0.0,
            fatsPer100g = 11.2,
            proteinPer100g = 26.5,
            fiberPer100g = 0.0,
            micronutrients = "Natural Creatine (400mg), Heme Iron, Zinc, Vitamin B12, Carnosine",
            pairingTip = "Choose lean cuts and pair 150g with high-fiber vegetables and a moderate carb portion.",
            gramsPerUnit = 150,
            unitNameSingular = "serving",
            unitNamePlural = "servings"
        ),
        FoodPer100gProfile(
            keywords = listOf("peanut butter", "almond butter", "peanuts", "groundnut"),
            displayName = "Natural Peanut Butter / Peanuts",
            description = "Calorie-dense healthy monounsaturated fat and plant protein source rich in Vitamin E, niacin, and resveratrol.",
            caloriesPer100g = 588,
            carbsPer100g = 20.0,
            fatsPer100g = 50.0,
            proteinPer100g = 25.0,
            fiberPer100g = 6.0,
            micronutrients = "Vitamin E, Niacin (B3), Magnesium, Folate, Healthy MUFAs",
            pairingTip = "1–2 tablespoons (16g–32g = 95–190 kcal, 4g–8g protein) on toast, bananas, or oatmeal.",
            gramsPerUnit = 16,
            unitNameSingular = "tbsp",
            unitNamePlural = "tbsp"
        ),
        FoodPer100gProfile(
            keywords = listOf("almond", "almonds", "walnut", "walnuts", "cashew", "cashews", "pistachio", "nuts", "dry fruits"),
            displayName = "Mixed Nuts (Almonds / Walnuts / Cashews)",
            description = "Nutrient-dense brain and hormone fuel supplying plant omega-3s, Vitamin E, magnesium, and satiating crunch.",
            caloriesPer100g = 579,
            carbsPer100g = 21.6,
            fatsPer100g = 49.9,
            proteinPer100g = 21.2,
            fiberPer100g = 12.5,
            micronutrients = "Vitamin E (25mg), Magnesium (270mg), Plant Omega-3 ALA, Calcium",
            pairingTip = "A handful (30g = ~174 kcal, 6.4g protein) makes an ideal mid-morning or evening snack with tea.",
            gramsPerUnit = 30,
            unitNameSingular = "handful (30g)",
            unitNamePlural = "handfuls"
        ),
        FoodPer100gProfile(
            keywords = listOf("potato", "boiled potato", "sweet potato", "shakarkandi"),
            displayName = "Boiled Potato / Sweet Potato",
            description = "One of the highest-ranking foods on the Holt Satiety Index, providing clean complex starch, potassium, and Vitamin A/C.",
            caloriesPer100g = 87,
            carbsPer100g = 20.1,
            fatsPer100g = 0.1,
            proteinPer100g = 1.9,
            fiberPer100g = 2.5,
            micronutrients = "Potassium (425mg), Vitamin C, Beta-Carotene (Sweet Potato), Vitamin B6",
            pairingTip = "1–2 medium boiled/air-fried potatoes keep you full for hours during fat-loss or lean bulking.",
            gramsPerUnit = 130,
            unitNameSingular = "medium potato",
            unitNamePlural = "medium potatoes"
        ),
        FoodPer100gProfile(
            keywords = listOf("bread", "brown bread", "whole wheat bread", "toast", "sourdough", "sandwich"),
            displayName = "Whole Wheat / Brown Bread",
            description = "Convenient complex carbohydrate base providing quick energy, dietary fiber, and B-vitamins.",
            caloriesPer100g = 252,
            carbsPer100g = 43.1,
            fatsPer100g = 3.5,
            proteinPer100g = 10.5,
            fiberPer100g = 6.0,
            micronutrients = "Selenium, Manganese, Iron, Thiamine (B1), Folate",
            pairingTip = "Top 2 slices (~60g = 151 kcal, 6.3g protein) with eggs, paneer, chicken, or peanut butter.",
            gramsPerUnit = 30,
            unitNameSingular = "slice",
            unitNamePlural = "slices"
        ),
        FoodPer100gProfile(
            keywords = listOf("poha", "upma", "pongal", "khichdi", "daliya"),
            displayName = "Poha / Upma / Pongal (Cooked)",
            description = "Light, easily digestible traditional grain dish offering gentle carbohydrates, iron, and steady morning energy.",
            caloriesPer100g = 132,
            carbsPer100g = 24.5,
            fatsPer100g = 2.6,
            proteinPer100g = 3.4,
            fiberPer100g = 1.8,
            micronutrients = "Iron, Vitamin B1, Potassium, Curry Leaf Antioxidants",
            pairingTip = "Add 30g roasted peanuts, boiled peas/sprouts, or 2 boiled eggs on the side to boost protein.",
            gramsPerUnit = 180,
            unitNameSingular = "bowl",
            unitNamePlural = "bowls"
        ),
        FoodPer100gProfile(
            keywords = listOf("broccoli", "spinach", "palak", "vegetables", "salad", "veggies", "cucumber", "carrot", "beans", "cabbage", "cauliflower"),
            displayName = "Green & Cruciferous Vegetables",
            description = "Ultra-low-calorie micronutrient & fiber powerhouse that maximizes meal volume, gut motility, and hormonal balance.",
            caloriesPer100g = 34,
            carbsPer100g = 6.6,
            fatsPer100g = 0.4,
            proteinPer100g = 2.8,
            fiberPer100g = 2.6,
            micronutrients = "Vitamin C, Vitamin K, Folate, Sulforaphane, Iron, Potassium",
            pairingTip = "Add 1–2 bowls of vegetables to lunch and dinner for volume satiety at barely ~50–70 kcal.",
            gramsPerUnit = 150,
            unitNameSingular = "bowl",
            unitNamePlural = "bowls"
        ),
        FoodPer100gProfile(
            keywords = listOf("ghee", "butter", "olive oil", "coconut oil", "oil"),
            displayName = "Ghee / Healthy Cooking Oil",
            description = "Concentrated lipid source supplying fat-soluble vitamins (A, D, E, K) and butyric/oleic fatty acids for hormone synthesis.",
            caloriesPer100g = 884,
            carbsPer100g = 0.0,
            fatsPer100g = 99.5,
            proteinPer100g = 0.0,
            fiberPer100g = 0.0,
            micronutrients = "Vitamins A, D, E, K2, Butyric Acid (Ghee), Polyphenols (Olive Oil)",
            pairingTip = "Measure 1 teaspoon (5g = ~44 kcal, 5g fat) per meal for flavor and vitamin absorption without excess calories.",
            gramsPerUnit = 5,
            unitNameSingular = "tsp",
            unitNamePlural = "tsp"
        ),
        FoodPer100gProfile(
            keywords = listOf("samosa", "samosas", "puff", "bajji", "bonda"),
            displayName = "Samosa / Savory Pastry",
            description = "Crispy fried pastry stuffed with spiced potatoes and peas — high in carbohydrates and cooking fats.",
            caloriesPer100g = 295,
            carbsPer100g = 34.0,
            fatsPer100g = 14.5,
            proteinPer100g = 5.5,
            fiberPer100g = 2.2,
            micronutrients = "Sodium, Iron, Potassium",
            pairingTip = "Pair with unsweetened green tea and keep to 1 piece to stay on track with your daily calorie budget.",
            gramsPerUnit = 80,
            unitNameSingular = "piece",
            unitNamePlural = "pieces"
        ),
        FoodPer100gProfile(
            keywords = listOf("pizza", "burger", "shawarma", "fries", "noodles", "maggi", "pasta", "biscuit", "cake", "chocolate"),
            displayName = "Fast Food / Treat Item",
            description = "Calorie-dense comfort food combining refined carbohydrates and fats — best enjoyed mindfully within your daily caloric budget.",
            caloriesPer100g = 285,
            carbsPer100g = 34.0,
            fatsPer100g = 13.0,
            proteinPer100g = 8.5,
            fiberPer100g = 2.0,
            micronutrients = "Sodium, Iron, B-Vitamins, Calcium",
            pairingTip = "Pair with a lean protein shake or side salad so you still hit your daily protein goal without overshooting calories.",
            gramsPerUnit = 150,
            unitNameSingular = "serving",
            unitNamePlural = "servings"
        )
    )

    /**
     * Resolves a food profile for any user input string.
     */
    fun findProfile(foodInput: String): FoodPer100gProfile {
        val clean = foodInput.trim().lowercase()
        database.firstOrNull { profile ->
            profile.keywords.any { kw -> clean == kw }
        }?.let { return it }

        val allMatches = database.mapNotNull { profile ->
            val matchedKw = profile.keywords
                .filter { kw -> clean.contains(kw) || (kw.contains(clean) && clean.length >= 3) }
                .maxByOrNull { it.length }
            if (matchedKw != null) Pair(profile, matchedKw.length) else null
        }
        allMatches.maxByOrNull { it.second }?.first?.let { return it }

        val titleCased = foodInput.trim().split(Regex("\\s+")).joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }.ifBlank { "Custom Food" }

        return when {
            clean.contains("juice") || clean.contains("drink") || clean.contains("soda") || clean.contains("soup") -> FoodPer100gProfile(
                keywords = listOf(clean),
                displayName = titleCased,
                description = "Liquid beverage/soup providing hydration, quick carbohydrates, and dissolved micronutrients.",
                caloriesPer100g = 48,
                carbsPer100g = 10.5,
                fatsPer100g = 0.4,
                proteinPer100g = 0.8,
                fiberPer100g = 0.5,
                micronutrients = "Vitamin C, Potassium, Sodium, Hydration Electrolytes",
                pairingTip = "Pair with a solid protein source to slow sugar absorption.",
                gramsPerUnit = 200,
                unitNameSingular = "glass",
                unitNamePlural = "glasses"
            )
            clean.contains("fried") || clean.contains("chips") || clean.contains("pakora") -> FoodPer100gProfile(
                keywords = listOf(clean),
                displayName = titleCased,
                description = "Crispy savory snack higher in cooking fats and carbohydrates — track portion weight or piece count to stay within your daily calorie target.",
                caloriesPer100g = 310,
                carbsPer100g = 32.0,
                fatsPer100g = 17.5,
                proteinPer100g = 6.5,
                fiberPer100g = 2.8,
                micronutrients = "Iron, Potassium, Sodium, B-Vitamins",
                pairingTip = "Keep portion controlled alongside unsweetened tea or green chutney.",
                gramsPerUnit = 60,
                unitNameSingular = "piece",
                unitNamePlural = "pieces"
            )
            clean.contains("sweet") || clean.contains("halwa") || clean.contains("laddu") || clean.contains("ice cream") || clean.contains("sugar") || clean.contains("honey") -> FoodPer100gProfile(
                keywords = listOf(clean),
                displayName = titleCased,
                description = "Energy-dense sweet source rich in fast-acting simple carbohydrates — best timed post-workout when muscle insulin sensitivity is highest.",
                caloriesPer100g = 340,
                carbsPer100g = 58.0,
                fatsPer100g = 10.0,
                proteinPer100g = 4.2,
                fiberPer100g = 1.0,
                micronutrients = "Quick Glucose, Calcium, Iron",
                pairingTip = "Enjoy a controlled portion post-workout to spike glycogen recovery.",
                gramsPerUnit = 50,
                unitNameSingular = "piece",
                unitNamePlural = "pieces"
            )
            else -> FoodPer100gProfile(
                keywords = listOf(clean),
                displayName = titleCased,
                description = "Balanced whole-food dish supplying mixed macronutrients (carbohydrates, proteins, and healthy fats) to support daily energy and recovery.",
                caloriesPer100g = 155,
                carbsPer100g = 18.0,
                fatsPer100g = 5.2,
                proteinPer100g = 9.0,
                fiberPer100g = 2.4,
                micronutrients = "B-Complex Vitamins, Iron, Potassium, Magnesium, Zinc",
                pairingTip = "Combine with lean protein & vegetables to hit your daily targets.",
                gramsPerUnit = 100,
                unitNameSingular = "serving",
                unitNamePlural = "servings"
            )
        }
    }

    /**
     * Parses multi-item or quantity expressions like "3 dosa 4 idly", "4 idli, 2 vada", "200g chicken", "3 rotis"
     * and returns a list of (foodName, grams, servingDescription, quantityCount, isQuantityBased) items.
     */
    private data class ParsedSubItem(
        val foodQuery: String,
        val grams: Int,
        val quantity: Int,
        val isQuantityBased: Boolean,
        val label: String
    )

    private fun parseMultiFoodInput(rawInput: String): List<ParsedSubItem> {
        val cleaned = rawInput.trim()
        if (cleaned.isBlank()) return emptyList()

        // Match patterns like "3 dosa", "4 idly", "200g chicken", "2 boiled eggs"
        val itemPattern = Regex(
            """(\d{1,4})\s*(g|gm|gms|gram|grams|ml|pcs|pc|piece|pieces|nos|no|cups|cup|bowls|bowl|plates|plate|slices|slice|scoops|scoop)?\s+([a-zA-Z][a-zA-Z\s]{1,25}?)(?=\s+\d|\s*[,+&]|\s+and\s+|\s*$)""",
            RegexOption.IGNORE_CASE
        )
        val matches = itemPattern.findAll(cleaned).toList()
        if (matches.isEmpty()) return emptyList()

        return matches.mapNotNull { m ->
            val num = m.groupValues[1].toIntOrNull()?.coerceIn(1, 5000) ?: return@mapNotNull null
            val unitToken = m.groupValues[2].lowercase().trim()
            val rawFood = m.groupValues[3]
                .replace(Regex("\\b(and|with)\\b", RegexOption.IGNORE_CASE), "")
                .trim()
            if (rawFood.isBlank()) return@mapNotNull null

            val profile = findProfile(rawFood)
            val isExplicitGrams = unitToken in setOf("g", "gm", "gms", "gram", "grams", "ml") || num >= 35 && unitToken.isEmpty() && profile.gramsPerUnit >= 100
            if (isExplicitGrams) {
                val g = num.coerceIn(5, 5000)
                ParsedSubItem(
                    foodQuery = rawFood,
                    grams = g,
                    quantity = 1,
                    isQuantityBased = false,
                    label = "${profile.displayName} (${g}g)"
                )
            } else {
                val qty = num.coerceIn(1, 100)
                val g = (qty * profile.gramsPerUnit).coerceIn(5, 5000)
                val unitWord = if (qty == 1) profile.unitNameSingular else profile.unitNamePlural
                ParsedSubItem(
                    foodQuery = rawFood,
                    grams = g,
                    quantity = qty,
                    isQuantityBased = true,
                    label = "$qty $unitWord ${profile.displayName} (${g}g)"
                )
            }
        }
    }

    /**
     * Unified smart calculator that accepts either:
     * 1) Multi-item natural text like "3 dosa 4 idly" or "2 eggs 3 idli" directly in foodInput, OR
     * 2) A food name + Quantity (Pieces/Count) when isQuantityMode == true (e.g., "Dosa", amount = 3 -> 3 dosas = 255g), OR
     * 3) A food name + Grams when isQuantityMode == false (e.g., "Chicken Breast", amount = 150 -> 150g).
     */
    fun calculateForGramsOrQuantity(
        foodInput: String,
        amountValue: Int,
        isQuantityMode: Boolean
    ): CalculatedFoodNutrition {
        val round1 = { v: Double -> ((v * 10.0).roundToInt()) / 10.0 }

        // Check if the user typed numbers directly inside the food name box (e.g., "3 dosa 4 idly" or "4 idly")
        val inlineParsed = parseMultiFoodInput(foodInput)
        if (inlineParsed.size >= 2 || (inlineParsed.size == 1 && foodInput.trim().firstOrNull()?.isDigit() == true)) {
            var totalGrams = 0
            var totalCalories = 0
            var totalCarbs = 0.0
            var totalFats = 0.0
            var totalProtein = 0.0
            var totalFiber = 0.0
            var totalQty = 0
            val names = mutableListOf<String>()
            val descriptions = mutableListOf<String>()
            val micros = mutableSetOf<String>()
            val tips = mutableListOf<String>()

            inlineParsed.forEach { item ->
                val profile = findProfile(item.foodQuery)
                val factor = item.grams / 100.0
                totalGrams += item.grams
                totalQty += item.quantity
                totalCalories += (profile.caloriesPer100g * factor).roundToInt().coerceAtLeast(1)
                totalCarbs += profile.carbsPer100g * factor
                totalFats += profile.fatsPer100g * factor
                totalProtein += profile.proteinPer100g * factor
                totalFiber += profile.fiberPer100g * factor
                names.add(item.label)
                descriptions.add(profile.description)
                profile.micronutrients.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { micros.add(it) }
                tips.add(profile.pairingTip)
            }

            val combinedLabel = names.joinToString(" + ")
            return CalculatedFoodNutrition(
                foodName = combinedLabel,
                grams = totalGrams.coerceAtLeast(1),
                quantityCount = totalQty.coerceAtLeast(1),
                isQuantityBased = true,
                servingLabel = "$combinedLabel (Total ${totalGrams}g)",
                description = descriptions.distinct().joinToString(" "),
                calories = totalCalories.coerceAtLeast(1),
                carbsG = round1(totalCarbs),
                fatsG = round1(totalFats),
                proteinG = round1(totalProtein),
                fiberG = round1(totalFiber),
                micronutrients = micros.take(6).joinToString(", "),
                pairingTip = tips.firstOrNull() ?: "Combine with lean protein and hydration to hit your daily targets."
            )
        }

        val profile = findProfile(foodInput)
        return if (isQuantityMode) {
            val safeQty = amountValue.coerceIn(1, 100)
            val computedGrams = (safeQty * profile.gramsPerUnit).coerceIn(5, 5000)
            val factor = computedGrams / 100.0
            val unitWord = if (safeQty == 1) profile.unitNameSingular else profile.unitNamePlural
            CalculatedFoodNutrition(
                foodName = profile.displayName,
                grams = computedGrams,
                quantityCount = safeQty,
                isQuantityBased = true,
                servingLabel = "$safeQty $unitWord (${computedGrams}g)",
                description = profile.description,
                calories = (profile.caloriesPer100g * factor).roundToInt().coerceAtLeast(1),
                carbsG = round1(profile.carbsPer100g * factor),
                fatsG = round1(profile.fatsPer100g * factor),
                proteinG = round1(profile.proteinPer100g * factor),
                fiberG = round1(profile.fiberPer100g * factor),
                micronutrients = profile.micronutrients,
                pairingTip = profile.pairingTip
            )
        } else {
            val safeGrams = amountValue.coerceIn(1, 5000)
            val factor = safeGrams / 100.0
            val approxQty = (safeGrams.toDouble() / profile.gramsPerUnit.coerceAtLeast(1)).roundToInt().coerceAtLeast(1)
            CalculatedFoodNutrition(
                foodName = profile.displayName,
                grams = safeGrams,
                quantityCount = approxQty,
                isQuantityBased = false,
                servingLabel = "${safeGrams}g (~$approxQty ${if (approxQty == 1) profile.unitNameSingular else profile.unitNamePlural})",
                description = profile.description,
                calories = (profile.caloriesPer100g * factor).roundToInt().coerceAtLeast(1),
                carbsG = round1(profile.carbsPer100g * factor),
                fatsG = round1(profile.fatsPer100g * factor),
                proteinG = round1(profile.proteinPer100g * factor),
                fiberG = round1(profile.fiberPer100g * factor),
                micronutrients = profile.micronutrients,
                pairingTip = profile.pairingTip
            )
        }
    }

    /**
     * Converts (foodName, grams) into exact Calories, Carbs, Fats, Protein, and Fiber.
     */
    fun calculateForGrams(foodInput: String, grams: Int): CalculatedFoodNutrition {
        return calculateForGramsOrQuantity(
            foodInput = foodInput,
            amountValue = grams,
            isQuantityMode = false
        )
    }

    /**
     * Extracts either a gram weight (e.g. "200g chicken") or quantity count (e.g. "3 dosa 4 idly", "4 idli") from a query.
     */
    fun extractGramsFromQuery(query: String): Pair<String, Int> {
        val multiParsed = parseMultiFoodInput(query)
        if (multiParsed.isNotEmpty()) {
            val totalGrams = multiParsed.sumOf { it.grams }.coerceIn(5, 5000)
            return Pair(query.trim(), totalGrams)
        }
        val regex = Regex("(\\d{1,4})\\s*(g|gm|gms|gram|grams|ml)\\b", RegexOption.IGNORE_CASE)
        val match = regex.find(query)
        return if (match != null) {
            val grams = match.groupValues[1].toIntOrNull()?.coerceIn(5, 3000) ?: 100
            val cleanedFood = query.replace(match.value, "").trim()
            Pair(cleanedFood.ifBlank { query }, grams)
        } else {
            Pair(query.trim(), 100)
        }
    }

    /**
     * Formats the exact chatbot response requested by the user:
     * <Food> - .....
     * Nutritional Breakdown :-
     * Carbs - ...
     * Fats - ...
     * Protein - ...
     * etc + Diet Plan Ideas
     */
    fun formatChatbotResponseForQuery(
        query: String,
        bio: HunterBiometrics,
        caloriesTarget: Int,
        caloriesLogged: Int,
        proteinLogged: Double,
        waterLogged: Double,
        waterTarget: Double
    ): CalculatedFoodNutrition {
        val multiParsed = parseMultiFoodInput(query)
        if (multiParsed.isNotEmpty()) {
            return calculateForGramsOrQuantity(query, 1, isQuantityMode = true)
        }
        val (foodPart, grams) = extractGramsFromQuery(query)
        return calculateForGrams(foodPart, grams)
    }

    fun buildFormattedChatReplyText(
        query: String,
        calc: CalculatedFoodNutrition,
        bio: HunterBiometrics,
        caloriesTarget: Int,
        caloriesLogged: Int,
        proteinLogged: Double
    ): String {
        val qLower = query.lowercase()
        val isDietPlanRequest = qLower.contains("diet plan") ||
            qLower.contains("meal plan") ||
            qLower.contains("full day") ||
            qLower.contains("routine") ||
            qLower.contains("chart")

        val calRemaining = (caloriesTarget - caloriesLogged).coerceAtLeast(0)
        val proRemaining = (bio.recommendedProteinG - proteinLogged.toInt()).coerceAtLeast(0)

        return if (isDietPlanRequest) {
            """
            ${bio.goalDisplayLabel} Diet Plan - Structured daily nutrition blueprint tailored for Trainer ${bio.hunterName} (${bio.weightKg} kg • Target: $caloriesTarget kcal & ${bio.recommendedProteinG}g Protein).

            Nutritional Breakdown (Daily Target Split) :-
            • Calories - $caloriesTarget kcal ($calRemaining kcal remaining today)
            • Carbs - ${(caloriesTarget * 0.45 / 4).toInt()}g (45% energy for workout glycogen)
            • Fats - ${(caloriesTarget * 0.25 / 9).toInt()}g (25% healthy lipids for hormones)
            • Protein - ${bio.recommendedProteinG}g ($proRemaining g remaining today for muscle synthesis)
            • Fiber - 30g–35g
            • Hydration Target - 3.5L–4.0L Water

            Complete Daily Diet Plan Ideas (${bio.goalDisplayLabel}) :-
            • Meal 1 (Breakfast - 8:00 AM) - 4 Idlis or 2 Dosas + Sambar + 3 Whole Boiled Eggs (580 kcal | 32g Protein | 74g Carbs | 15g Fat)
            • Meal 2 (Lunch - 1:30 PM) - 1 Bowl Cooked Rice or 3 Chapatis + 150g Grilled Chicken Breast / Paneer + 1 Bowl Dal & Green Veggies + 1 Bowl Curd (720 kcal | 52g Protein | 82g Carbs | 16g Fat)
            • Meal 3 (Pre-Workout / Evening - 5:00 PM) - 1 Cup Tea/Coffee + 1 Bowl Boiled Chana / Sprouts or 2 Slices Bread with Peanut Butter (320 kcal | 14g Protein | 42g Carbs | 9g Fat)
            • Meal 4 (Post-Workout / Dinner - 8:30 PM) - 150g Chicken / Fish / Tofu / Soya Chunks + 2 Dosas or 2 Chapatis + Mixed Salad (580 kcal | 46g Protein | 54g Carbs | 14g Fat)
            """.trimIndent()
        } else {
            """
            ${calc.foodName} (${calc.servingLabel}) - ${calc.description}

            Nutritional Breakdown (${calc.servingLabel}) :-
            • Calories - ${calc.calories} kcal
            • Carbs - ${calc.carbsG}g
            • Fats - ${calc.fatsG}g
            • Protein - ${calc.proteinG}g
            • Fiber - ${calc.fiberG}g
            • Vitamins & Minerals - ${calc.micronutrients}

            Diet Plan Ideas (${bio.goalDisplayLabel} • Target: $caloriesTarget kcal) :-
            • How to Eat It - ${calc.pairingTip}
            • Breakfast Option - ${calc.foodName} paired with 3 Whole Eggs or 150g Greek Yogurt/Paneer + 1 Fruit (~450 kcal | 30g Protein)
            • Lunch / Dinner Option - 2 Chapatis or 3 Idlis + 150g Chicken/Dal/Paneer + ${calc.foodName} (~580 kcal | 42g Protein)
            • Remaining Daily Goal - You have $calRemaining kcal and ${proRemaining}g protein left to reach today's target!
            """.trimIndent()
        }
    }

    /**
     * Calculates exact calories burned for all logged exercises & sets today,
     * and generates goal-specific Post-Workout Meals, Daily Diet Plan, and Caloric Surplus / Deficit guidance.
     */
    fun calculateWorkoutBurnAndDietPlan(
        exercises: List<ExerciseLogged>,
        bio: HunterBiometrics,
        caloriesLogged: Int,
        caloriesTarget: Int,
        proteinLogged: Double
    ): WorkoutBurnAndDietSummary {
        val weightFactor = (bio.weightKg / 70.0).coerceIn(0.65, 1.85)
        val breakdown = exercises.map { ex ->
            val lower = ex.exerciseName.lowercase()
            val repsNumeric = Regex("\\d+").find(ex.repsPerSet)?.value?.toIntOrNull()?.coerceIn(1, 100) ?: 10
            val repFactor = (repsNumeric / 10.0).coerceIn(0.6, 2.2)

            val (baseKcalPerSet, intensityTag) = when {
                listOf("squat", "deadlift", "lunge", "leg press", "burpee", "sprint", "run", "hiit", "clean", "thruster").any { lower.contains(it) } ->
                    Pair(11.5, "High-Output Compound")
                listOf("bench", "pushup", "push-up", "pullup", "pull-up", "chin", "row", "dip", "overhead", "shoulder press", "lat").any { lower.contains(it) } ->
                    Pair(8.5, "Upper-Body Hypertrophy")
                else ->
                    Pair(6.2, "Targeted Isolation")
            }

            val overloadMultiplier = if (ex.progressiveOverloadHit) 1.18 else 1.0
            val burned = (ex.setsCompleted * baseKcalPerSet * weightFactor * repFactor * overloadMultiplier)
                .roundToInt()
                .coerceAtLeast(ex.setsCompleted * 5)

            ExerciseCalorieBurnItem(
                exerciseName = ex.exerciseName,
                sets = ex.setsCompleted,
                reps = ex.repsPerSet,
                caloriesBurned = burned,
                intensityTag = intensityTag
            )
        }

        val totalBurn = breakdown.sumOf { it.caloriesBurned }
        val totalSets = breakdown.sumOf { it.sets }
        val isWeightLoss = bio.fitnessGoal.uppercase() == "WEIGHT_LOSS"
        val isWeightGain = bio.fitnessGoal.uppercase() == "WEIGHT_GAIN"

        // Adjust daily intake based on workout expenditure and fitness goal
        val burnAdjustedTarget = when {
            isWeightGain -> caloriesTarget + totalBurn // Replace 100% of burned workout calories + maintain +400 kcal surplus
            isWeightLoss -> caloriesTarget + (totalBurn * 0.35).roundToInt() // Keep 65% of workout burn as extra fat-loss deficit
            else -> caloriesTarget + (totalBurn * 0.75).roundToInt()
        }

        val remainingNeeded = burnAdjustedTarget - caloriesLogged
        val proteinTarget = bio.recommendedProteinG
        val proteinRemaining = (proteinTarget - proteinLogged.toInt()).coerceAtLeast(0)

        val (goalModeTitle, surplusHeadline, surplusAdvice) = when {
            isWeightGain -> Triple(
                "WEIGHT GAIN • ANABOLIC SURPLUS PROTOCOL",
                if (remainingNeeded > 0) {
                    "Need +$remainingNeeded kcal more today (Includes +400 kcal Surplus + $totalBurn kcal Workout Burn)"
                } else {
                    "Daily Anabolic Surplus Achieved! (+${-remainingNeeded} kcal above target)"
                },
                "You burned $totalBurn kcal across $totalSets sets today. For muscle growth & weight gain, you must eat back all $totalBurn burned calories PLUS maintain a +400 kcal anabolic surplus (Total Goal Today: $burnAdjustedTarget kcal & ${proteinTarget}g protein; ${proteinRemaining}g protein left)."
            )
            isWeightLoss -> Triple(
                "WEIGHT LOSS • CALORIC DEFICIT / REDUCTION PROTOCOL",
                if (remainingNeeded >= 0) {
                    "Eat up to $remainingNeeded kcal more today (Stay in -500 kcal Fat-Loss Deficit)"
                } else {
                    "Over Fat-Loss Budget by ${-remainingNeeded} kcal — Reduce Evening Carbs/Fats"
                },
                "You burned $totalBurn kcal across $totalSets sets today! To accelerate fat loss while sparing lean muscle, DO NOT eat back all burned calories — keep a net reduction/deficit of ~500 kcal below maintenance (Max Intake Today: $burnAdjustedTarget kcal with ${proteinTarget}g high-satiety protein; ${proteinRemaining}g protein left)."
            )
            else -> Triple(
                "BODY RECOMPOSITION • GLYCOGEN BALANCE PROTOCOL",
                if (remainingNeeded > 0) {
                    "Intake +$remainingNeeded kcal more today to match workout output"
                } else {
                    "Daily Recomposition Target Reached ($caloriesLogged / $burnAdjustedTarget kcal)"
                },
                "You burned $totalBurn kcal across $totalSets sets. Replenish 75% of workout expenditure ($burnAdjustedTarget kcal total today) while keeping protein high at ${proteinTarget}g (${proteinRemaining}g left)."
            )
        }

        val recommendedMeals = if (isWeightGain) {
            listOf(
                RecommendedWorkoutMeal(
                    timingTitle = "IMMEDIATE POST-WORKOUT (WITHIN 45 MINS)",
                    mealName = "1 Scoop Whey + 2 Bananas + 4 Egg Whites",
                    portionDescription = "Rapidly spikes muscle protein synthesis & replaces $totalBurn kcal burned glycogen.",
                    calories = 460,
                    proteinG = 40.0,
                    carbsG = 62.0,
                    fatsG = 5.0
                ),
                RecommendedWorkoutMeal(
                    timingTitle = "ANABOLIC SURPLUS RECOVERY MEAL",
                    mealName = "3 Dosas or 250g Rice + 150g Chicken Breast / Paneer + Dal",
                    portionDescription = "High-calorie, high-protein compound meal to lock in your +400 kcal muscle bulk surplus.",
                    calories = 740,
                    proteinG = 52.0,
                    carbsG = 86.0,
                    fatsG = 18.0
                ),
                RecommendedWorkoutMeal(
                    timingTitle = "EVENING HYPERTROPHY SNACK",
                    mealName = "4 Steamed Idlis + Sambar + 3 Whole Boiled Eggs",
                    portionDescription = "Clean fermented carbs + complete egg amino acids for overnight muscle repair.",
                    calories = 590,
                    proteinG = 33.0,
                    carbsG = 68.0,
                    fatsG = 17.0
                )
            )
        } else if (isWeightLoss) {
            listOf(
                RecommendedWorkoutMeal(
                    timingTitle = "POST-WORKOUT LEAN RECOVERY (HIGH PROTEIN, LOW CALORIE)",
                    mealName = "5 Boiled Egg Whites + 1 Whole Egg + 1 Apple",
                    portionDescription = "Triggers muscle repair with minimal calories so your $totalBurn kcal workout burn melts body fat.",
                    calories = 265,
                    proteinG = 28.5,
                    carbsG = 22.0,
                    fatsG = 6.0
                ),
                RecommendedWorkoutMeal(
                    timingTitle = "FAT-LOSS MAIN MEAL (CALORIE REDUCTION)",
                    mealName = "150g Grilled Chicken / Tofu + 2 Rotis or 3 Idlis + Green Salad",
                    portionDescription = "High fiber & lean protein keeps you full while maintaining a strict 500 kcal fat-loss deficit.",
                    calories = 440,
                    proteinG = 44.0,
                    carbsG = 42.0,
                    fatsG = 9.0
                ),
                RecommendedWorkoutMeal(
                    timingTitle = "LIGHT DINNER (LOW-CARB CUTTING PROTOCOL)",
                    mealName = "100g Paneer / Fish + 1 Bowl Dal Soup & Steamed Veggies",
                    portionDescription = "Zero refined carbs at night to maximize overnight fat oxidation and growth hormone.",
                    calories = 360,
                    proteinG = 32.0,
                    carbsG = 24.0,
                    fatsG = 14.0
                )
            )
        } else {
            listOf(
                RecommendedWorkoutMeal(
                    timingTitle = "POST-WORKOUT RECOMP REFUEL",
                    mealName = "1 Scoop Whey or 4 Eggs + 3 Steamed Idlis",
                    portionDescription = "Balanced protein & clean carbs to recover from $totalSets sets without excess fat storage.",
                    calories = 420,
                    proteinG = 34.0,
                    carbsG = 46.0,
                    fatsG = 8.0
                ),
                RecommendedWorkoutMeal(
                    timingTitle = "BALANCED MAIN MEAL",
                    mealName = "2 Chapatis or 2 Dosas + 150g Chicken / Paneer + 1 Bowl Curd",
                    portionDescription = "Sustains lean muscle mass and steady metabolic output.",
                    calories = 560,
                    proteinG = 45.0,
                    carbsG = 56.0,
                    fatsG = 14.0
                )
            )
        }

        val bullets = if (isWeightGain) {
            listOf(
                "Calorie Surplus Rule: Eat $burnAdjustedTarget kcal today ($caloriesTarget base + $totalBurn workout burn) so your body builds new muscle tissue instead of burning muscle for fuel.",
                "Protein Target: Hit ${proteinTarget}g protein (${proteinRemaining}g remaining) split across 4 meals (~30–40g per meal).",
                "Carb Timing: Eat 60% of your carbs (Rice, Idli, Dosa, Oats, Bananas) around your workout window."
            )
        } else if (isWeightLoss) {
            listOf(
                "Calorie Reduction Rule: Cap total intake at $burnAdjustedTarget kcal today — let your $totalBurn kcal workout burn widen your fat-loss deficit!",
                "Muscle Protection: Keep protein at ${proteinTarget}g (${proteinRemaining}g remaining) using Egg Whites, Chicken Breast, Tofu, Soya, or Fish.",
                "Smart Swaps: Replace fried snacks and sugary drinks with Steamed Idli, Dal, Green Tea, and 200g Green Vegetables per meal."
            )
        } else {
            listOf(
                "Recomp Balance: Target $burnAdjustedTarget kcal and ${proteinTarget}g protein today.",
                "Prioritize whole foods (Eggs, Chicken, Paneer, Idli, Roti, Dal, Curd) and 3.5L+ hydration."
            )
        }

        return WorkoutBurnAndDietSummary(
            exerciseBreakdown = breakdown,
            totalWorkoutBurnKcal = totalBurn,
            totalSetsCompleted = totalSets,
            baseDailyTargetKcal = caloriesTarget,
            burnAdjustedTargetKcal = burnAdjustedTarget,
            caloriesLoggedToday = caloriesLogged,
            remainingCaloriesNeeded = remainingNeeded,
            goalModeTitle = goalModeTitle,
            surplusOrDeficitHeadline = surplusHeadline,
            surplusOrDeficitAdvice = surplusAdvice,
            recommendedMeals = recommendedMeals,
            dailyDietProtocolBullets = bullets
        )
    }
}
