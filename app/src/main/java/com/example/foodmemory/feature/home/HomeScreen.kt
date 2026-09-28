package com.example.foodmemory.feature.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.foodmemory.R
import com.example.foodmemory.domain.model.FoodExperience
import com.example.foodmemory.feature.shared.decodeUprightBitmap
import com.example.foodmemory.ui.theme.FoodForest
import com.example.foodmemory.ui.theme.FoodMuted
import com.example.foodmemory.ui.theme.FoodTerracotta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

@Composable
fun HomeScreen(experiences: List<FoodExperience>, onAddExperience: () -> Unit, onCaptureDish: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 24.dp, top = 28.dp, end = 24.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Column {
                Text(stringResource(R.string.home_brand), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                Text(
                    stringResource(R.string.home_greeting),
                    modifier = Modifier.padding(top = 16.dp),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    lineHeight = MaterialTheme.typography.headlineLarge.lineHeight * 1.05f
                )
                Text(stringResource(R.string.home_subtitle), modifier = Modifier.padding(top = 10.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyLarge)
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                HomeAction(
                    marker = stringResource(R.string.home_camera_marker),
                    title = stringResource(R.string.home_camera_action),
                    subtitle = stringResource(R.string.home_camera_caption),
                    primary = true,
                    onClick = onCaptureDish
                )
                HomeAction(
                    marker = stringResource(R.string.home_manual_marker),
                    title = stringResource(R.string.home_manual_action),
                    subtitle = stringResource(R.string.home_manual_caption),
                    primary = false,
                    onClick = onAddExperience
                )
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.home_recent_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                if (experiences.isNotEmpty()) Text(experiences.size.toString(), color = FoodMuted, style = MaterialTheme.typography.labelLarge)
            }
        }
        if (experiences.isEmpty()) {
            item {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Text(stringResource(R.string.home_empty_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(stringResource(R.string.home_empty_message), modifier = Modifier.padding(top = 6.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        } else {
            items(experiences, key = { it.id }) { experience -> ExperienceCard(experience) }
        }
    }
}

@Composable
private fun HomeAction(marker: String, title: String, subtitle: String, primary: Boolean, onClick: () -> Unit) {
    val foreground = if (primary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    val supporting = if (primary) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f) else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (primary) FoodForest else MaterialTheme.colorScheme.surface,
        border = if (primary) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(14.dp), color = if (primary) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(48.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        marker,
                        color = if (primary) MaterialTheme.colorScheme.onPrimary else FoodForest,
                        style = if (primary) MaterialTheme.typography.labelMedium else MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Column(Modifier.weight(1f).padding(start = 14.dp, end = 8.dp)) {
                Text(title, color = foreground, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, modifier = Modifier.padding(top = 3.dp), color = supporting, style = MaterialTheme.typography.bodySmall)
            }
            Text("→", color = if (primary) foreground else FoodForest, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun ExperienceCard(experience: FoodExperience) {
    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(experience.restaurantName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    val subtitle = listOf(experience.city, experience.visitDate).filter(String::isNotBlank).joinToString(" · ")
                    if (subtitle.isNotBlank()) Text(subtitle, modifier = Modifier.padding(top = 3.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                experience.dishes.firstNotNullOfOrNull { it.photoPath }?.let { photoPath -> CachedDishPhoto(photoPath) }
            }
            val details = buildList {
                experience.overallRating?.let { add("★ ${formatRating(it)}") }
                experience.priceCents?.let { add(formatPrice(it)) }
            }.joinToString("  ·  ")
            if (details.isNotBlank()) Text(details, modifier = Modifier.padding(top = 10.dp), color = FoodTerracotta, style = MaterialTheme.typography.labelLarge)
            if (experience.dishes.isNotEmpty()) Text(experience.dishes.joinToString { it.name }, modifier = Modifier.padding(top = 6.dp), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun CachedDishPhoto(path: String) {
    val bitmap = produceState<ImageBitmap?>(initialValue = null, path) {
        value = withContext(Dispatchers.IO) {
            decodeUprightBitmap(path, maxDimension = 512)?.asImageBitmap()
        }
    }.value
    if (bitmap != null) Image(bitmap, contentDescription = null, modifier = Modifier.size(64.dp).clip(RoundedCornerShape(14.dp)))
}

private fun formatRating(rating: Double): String = String.format(Locale.getDefault(), "%.1f", rating).removeSuffix(",0").removeSuffix(".0")
private fun formatPrice(priceCents: Long): String = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-ES")).format(priceCents / 100.0)
