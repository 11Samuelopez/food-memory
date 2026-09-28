package com.example.foodmemory.feature.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.foodmemory.R
import com.example.foodmemory.domain.model.RecommendationType
import com.example.foodmemory.domain.model.UserSession
import com.example.foodmemory.ui.theme.FoodForest
import com.example.foodmemory.ui.theme.FoodInk
import com.example.foodmemory.ui.theme.FoodMuted
import com.example.foodmemory.ui.theme.FoodPeach
import com.example.foodmemory.ui.theme.FoodProfileBorder
import com.example.foodmemory.ui.theme.FoodProfileCard
import com.example.foodmemory.ui.theme.FoodCorners
import com.example.foodmemory.ui.theme.FoodSizes
import com.example.foodmemory.ui.theme.FoodSpacing
import com.example.foodmemory.ui.theme.FoodSage
import java.text.NumberFormat
import java.util.Locale

private val ProfileCard = FoodProfileCard
private val ProfileBorder = FoodProfileBorder

@Composable
fun ProfileScreen(state: ProfileUiState, user: UserSession?, onSignOut: () -> Unit) {
    val profile = state.tasteProfile
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = FoodSpacing.ScreenHorizontal,
            top = FoodSpacing.Large,
            end = FoodSpacing.ScreenHorizontal,
            bottom = FoodSpacing.ScreenBottom
        ),
        verticalArrangement = Arrangement.spacedBy(FoodSpacing.CardInset)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    stringResource(R.string.profile_eyebrow),
                    color = FoodForest,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(stringResource(R.string.profile_title), color = FoodInk, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.profile_subtitle), color = FoodMuted, style = MaterialTheme.typography.bodyMedium)
            }
        }

        if (user != null) {
            item {
                Surface(
                    color = ProfileCard,
                    contentColor = FoodInk,
                    shape = FoodCorners.Feature,
                    border = BorderStroke(1.dp, ProfileBorder)
                ) {
                    Column(Modifier.fillMaxWidth().padding(FoodSpacing.CardInset)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            val initials = user.displayName.trim().split(" ").take(2).mapNotNull(String::firstOrNull).joinToString("").uppercase()
                Surface(color = FoodForest, contentColor = Color.White, shape = CircleShape, modifier = Modifier.size(FoodSizes.ProfileAvatar)) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(initials, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                }
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(stringResource(R.string.profile_account_label), color = FoodForest, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                Text(user.displayName, color = FoodInk, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text(user.email, color = FoodMuted, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                        OutlinedButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                            Text(stringResource(R.string.session_sign_out), color = FoodForest, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        if (!profile.hasHistory) {
            item {
                Surface(color = FoodSage, contentColor = FoodInk, shape = FoodCorners.Large) {
                    Row(Modifier.fillMaxWidth().padding(20.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        AccentIcon("✦", FoodPeach)
                        Text(stringResource(R.string.profile_recommendation_empty), color = FoodInk, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        } else {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AccentIcon("✦", FoodPeach)
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(stringResource(R.string.profile_ai_badge), color = FoodForest, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.profile_ai_title), color = FoodInk, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
                }
            }
            if (state.isLoadingRecommendations) {
                item { ProfileMessage(stringResource(R.string.profile_ai_loading), FoodSage) }
            }
            if (state.hasRecommendationError) {
                item { ProfileMessage(stringResource(R.string.profile_ai_unavailable), FoodPeach) }
            }
            items(state.aiRecommendations) { recommendation ->
                val isDish = recommendation.type == RecommendationType.DISH
                RecommendationCard(
                    icon = if (isDish) "✦" else "⌖",
                    title = recommendation.name,
                    detail = recommendation.reason,
                    footnote = stringResource(if (isDish) R.string.profile_ai_dish else R.string.profile_ai_restaurant),
                    color = if (isDish) FoodPeach else FoodSage
                )
            }
            if (profile.favoriteDishes.isNotEmpty()) {
                item { SectionHeading(stringResource(R.string.profile_top_dishes)) }
                items(profile.favoriteDishes) { dish ->
                    RecommendationCard("✦", dish.name, stringResource(R.string.profile_recommendation_reason), "★ ${formatRating(dish.rating)}", FoodPeach)
                }
            }
            if (profile.placesToRepeat.isNotEmpty()) {
                item { SectionHeading(stringResource(R.string.profile_repeat_places)) }
                items(profile.placesToRepeat) { place ->
                    RecommendationCard("⌖", place.name, stringResource(R.string.profile_repeat_reason), "★ ${formatRating(place.rating)}", FoodSage)
                }
            }
            profile.averageSpendCents?.let { cents ->
                item {
                    Surface(color = ProfileCard, contentColor = FoodInk, shape = FoodCorners.Card, border = BorderStroke(1.dp, ProfileBorder)) {
                        Row(Modifier.fillMaxWidth().padding(18.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.profile_spend_average), color = FoodMuted)
                            Text(formatPrice(cents), color = FoodForest, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeading(text: String) {
    Text(text, modifier = Modifier.padding(top = 4.dp), color = FoodInk, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
}

@Composable
private fun ProfileMessage(text: String, color: Color) {
    Surface(color = color, contentColor = FoodInk, shape = FoodCorners.Card) {
        Text(text, modifier = Modifier.fillMaxWidth().padding(18.dp), color = FoodInk, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun AccentIcon(icon: String, color: Color) {
    Surface(color = color, contentColor = FoodForest, shape = FoodCorners.Compact, modifier = Modifier.size(46.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Text(icon, color = FoodForest, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RecommendationCard(icon: String, title: String, detail: String, footnote: String, color: Color) {
    Surface(color = color, contentColor = FoodInk, shape = FoodCorners.Profile) {
        Row(Modifier.fillMaxWidth().padding(17.dp), horizontalArrangement = Arrangement.spacedBy(13.dp), verticalAlignment = Alignment.CenterVertically) {
            AccentIcon(icon, Color.White.copy(alpha = 0.72f))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, color = FoodInk, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(detail, color = FoodInk.copy(alpha = 0.78f), style = MaterialTheme.typography.bodySmall)
                Text(footnote, color = FoodForest, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private fun formatRating(rating: Double) = String.format(Locale.getDefault(), "%.1f", rating)
private fun formatPrice(cents: Long) = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-ES")).format(cents / 100.0)
