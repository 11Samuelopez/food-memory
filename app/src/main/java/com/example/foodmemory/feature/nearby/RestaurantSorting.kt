package com.example.foodmemory.feature.nearby

import com.example.foodmemory.domain.model.NearbyRestaurant
import java.text.Collator
import java.util.Locale

/** Spanish collation treats accented vowels as their base letters instead of sorting them after Z. */
internal fun sortBySpanishName(
    places: List<NearbyRestaurant>,
    direction: SortDirection
): List<NearbyRestaurant> {
    val collator = Collator.getInstance(Locale.forLanguageTag("es-ES")).apply {
        strength = Collator.PRIMARY
        decomposition = Collator.CANONICAL_DECOMPOSITION
    }
    val sorted = places.sortedWith { first, second ->
        collator.compare(first.name, second.name).takeIf { it != 0 }
            ?: first.id.compareTo(second.id)
    }
    return if (direction == SortDirection.DESCENDING) sorted.asReversed() else sorted
}
