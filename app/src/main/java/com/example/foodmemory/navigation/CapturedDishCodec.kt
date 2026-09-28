package com.example.foodmemory.navigation

import com.example.foodmemory.domain.model.DishIngredient
import com.example.foodmemory.domain.model.IngredientEvidence
import com.example.foodmemory.domain.model.NewDish
import org.json.JSONArray
import org.json.JSONObject

object CapturedDishCodec {
    fun encode(dish: NewDish): String = JSONObject()
        .put("name", dish.name)
        .put("description", dish.description)
        .put("photoPath", dish.photoPath)
        .put("ingredients", JSONArray().apply {
            dish.ingredients.forEach { ingredient ->
                put(JSONObject().put("name", ingredient.name).put("evidence", ingredient.evidence.name))
            }
        })
        .toString()

    fun decode(json: String): NewDish {
        val value = JSONObject(json)
        val ingredientsJson = value.optJSONArray("ingredients") ?: JSONArray()
        val ingredients = buildList {
            for (index in 0 until ingredientsJson.length()) {
                val ingredient = ingredientsJson.optJSONObject(index) ?: continue
                val name = ingredient.optString("name").trim()
                if (name.isNotBlank()) {
                    add(
                        DishIngredient(
                            name,
                            runCatching { IngredientEvidence.valueOf(ingredient.optString("evidence")) }
                                .getOrDefault(IngredientEvidence.TYPICAL)
                        )
                    )
                }
            }
        }
        return NewDish(
            name = value.optString("name"),
            rating = null,
            description = value.optString("description"),
            photoPath = value.optString("photoPath").takeIf(String::isNotBlank),
            ingredients = ingredients
        )
    }
}
