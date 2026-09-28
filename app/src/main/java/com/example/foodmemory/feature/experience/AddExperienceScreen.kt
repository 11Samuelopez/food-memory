package com.example.foodmemory.feature.experience

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.foodmemory.domain.model.NewDish
import com.example.foodmemory.domain.model.NewExperience
import com.example.foodmemory.domain.model.DishIngredient
import com.example.foodmemory.R
import com.example.foodmemory.navigation.CapturedDishCodec
import kotlinx.coroutines.flow.collect
import java.math.RoundingMode
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class DishDraft(
    val name: String,
    val rating: String = "",
    val description: String,
    val photoPath: String?,
    val ingredients: List<DishIngredient>
)

@Composable
fun AddExperienceScreen(
    viewModel: ExperienceViewModel,
    capturedDishJson: String?,
    onConsumeCapturedDish: () -> Unit,
    onAddDish: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    var restaurant by rememberSaveable { mutableStateOf("") }
    var city by rememberSaveable { mutableStateOf("") }
    var visitDate by rememberSaveable { mutableStateOf(isoDateFormat().format(Date())) }
    var price by rememberSaveable { mutableStateOf("") }
    var overallRating by rememberSaveable { mutableStateOf("") }
    var companions by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var manualDishName by rememberSaveable { mutableStateOf("") }
    val dishes = remember { mutableStateListOf<DishDraft>() }
    var errorMessage by rememberSaveable { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                AddExperienceEvent.Saved -> onBack()
                AddExperienceEvent.Failed -> {
                    isSaving = false
                    errorMessage = context.getString(R.string.experience_saved_error)
                }
            }
        }
    }
    LaunchedEffect(capturedDishJson) {
        capturedDishJson?.let { json ->
            runCatching { CapturedDishCodec.decode(json) }
                .onSuccess { dish ->
                    dishes.add(DishDraft(dish.name, description = dish.description, photoPath = dish.photoPath, ingredients = dish.ingredients))
                }
            onConsumeCapturedDish()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Text("‹", style = MaterialTheme.typography.headlineMedium) }
            Column {
                Text(stringResource(R.string.experience_title), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.experience_subtitle), style = MaterialTheme.typography.bodySmall)
            }
        }

        OutlinedTextField(restaurant, { restaurant = it; errorMessage = null }, label = { Text(stringResource(R.string.experience_restaurant)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(city, { city = it }, label = { Text(stringResource(R.string.experience_city)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(
            visitDate,
            { visitDate = it; errorMessage = null },
            label = { Text(stringResource(R.string.experience_date)) },
            supportingText = { Text(stringResource(R.string.experience_date_hint)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            price,
            { price = it; errorMessage = null },
            label = { Text(stringResource(R.string.experience_price)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true
        )
        OutlinedTextField(
            overallRating,
            { overallRating = it; errorMessage = null },
            label = { Text(stringResource(R.string.experience_rating)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            singleLine = true
        )

        Text(stringResource(R.string.experience_dishes), style = MaterialTheme.typography.titleMedium)
        if (dishes.isEmpty()) {
            Text(stringResource(R.string.experience_no_dishes), style = MaterialTheme.typography.bodyMedium)
        }
        dishes.forEachIndexed { index, dish ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(dish.name, style = MaterialTheme.typography.titleMedium)
                    if (dish.description.isNotBlank()) Text(dish.description, style = MaterialTheme.typography.bodyMedium)
                    if (dish.ingredients.isNotEmpty()) {
                        Text(
                            dish.ingredients.joinToString { it.name },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = dish.rating,
                        onValueChange = { dishes[index] = dish.copy(rating = it) },
                        label = { Text(stringResource(R.string.experience_rating)) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                    OutlinedButton(onClick = { dishes.removeAt(index) }, modifier = Modifier.padding(start = 8.dp)) {
                        Text(stringResource(R.string.experience_remove_dish))
                    }
                }
                }
            }
        }
        OutlinedTextField(
            value = manualDishName,
            onValueChange = { manualDishName = it; errorMessage = null },
            label = { Text(stringResource(R.string.experience_manual_dish)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedButton(
            onClick = {
                if (manualDishName.isBlank()) {
                    errorMessage = context.getString(R.string.experience_manual_dish_error)
                } else {
                    dishes.add(DishDraft(manualDishName.trim(), description = "", photoPath = null, ingredients = emptyList()))
                    manualDishName = ""
                    errorMessage = null
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(R.string.experience_add_manual_dish)) }
        OutlinedButton(onClick = onAddDish, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.experience_add_dish_photo))
        }

        OutlinedTextField(companions, { companions = it }, label = { Text(stringResource(R.string.experience_companions)) }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        OutlinedTextField(notes, { notes = it }, label = { Text(stringResource(R.string.experience_notes)) }, modifier = Modifier.fillMaxWidth(), minLines = 3)

        errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(
            onClick = {
                val parsedDate = parseIsoDate(visitDate.trim())
                val parsedPrice = if (price.isBlank()) null else runCatching {
                    price.toDecimalOrNull()
                        ?.takeIf { it >= java.math.BigDecimal.ZERO }
                        ?.movePointRight(2)
                        ?.setScale(0, RoundingMode.HALF_UP)
                        ?.longValueExact()
                }.getOrNull()
                val parsedOverall = if (overallRating.isBlank()) null else overallRating.toRatingOrNull()
                val parsedDishes = dishes.mapNotNull { draft ->
                    draft.name.takeIf(String::isNotBlank)?.let { name ->
                        NewDish(
                            name = name.trim(),
                            rating = if (draft.rating.isBlank()) null else draft.rating.toRatingOrNull(),
                            description = draft.description,
                            photoPath = draft.photoPath,
                            ingredients = draft.ingredients
                        )
                    }
                }
                val invalidDishRating = dishes.any { it.name.isNotBlank() && it.rating.isNotBlank() && it.rating.toRatingOrNull() == null }

                errorMessage = when {
                    restaurant.isBlank() -> context.getString(R.string.experience_restaurant_error)
                    parsedDate == null -> context.getString(R.string.experience_date_error)
                    price.isNotBlank() && parsedPrice == null -> context.getString(R.string.experience_price_error)
                    overallRating.isNotBlank() && parsedOverall == null -> context.getString(R.string.experience_rating_error)
                    invalidDishRating -> context.getString(R.string.experience_rating_error)
                    else -> null
                }
                if (errorMessage == null) {
                    isSaving = true
                    viewModel.addExperience(
                        NewExperience(
                            restaurantName = restaurant.trim(),
                            city = city.trim(),
                            visitDate = visitDate.trim(),
                            priceCents = parsedPrice,
                            overallRating = parsedOverall,
                            companions = companions,
                            notes = notes,
                            dishes = parsedDishes
                        )
                    )
                }
            },
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isSaving) CircularProgressIndicator() else Text(stringResource(R.string.experience_save))
        }
        OutlinedButton(onClick = onBack, enabled = !isSaving, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.experience_cancel)) }
        Spacer(Modifier.height(8.dp))
    }
}

private fun String.toDecimalOrNull() = trim().replace(',', '.').toBigDecimalOrNull()

private fun String.toRatingOrNull(): Double? =
    trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() && it in 0.0..5.0 }

private fun isoDateFormat() = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).apply { isLenient = false }

private fun parseIsoDate(value: String): Date? {
    val parser = isoDateFormat()
    val position = ParsePosition(0)
    val date = parser.parse(value, position)
    return date?.takeIf { position.index == value.length && parser.format(it) == value }
}
