package ru.rinuuri

import com.google.gson.JsonObject
import ru.rinuuri.RecipeTypes.WASHER_RECIPE
import net.minecraft.resources.ResourceLocation
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.RecipeChoice
import xyz.xenondevs.commons.gson.getArray
import xyz.xenondevs.nova.data.recipe.MultiResultRecipe
import xyz.xenondevs.nova.data.recipe.NovaRecipe
import xyz.xenondevs.nova.data.recipe.RecipeManager
import xyz.xenondevs.nova.data.recipe.SingleInputChoiceRecipe
import xyz.xenondevs.nova.data.serialization.json.serializer.RecipeDeserializer
import xyz.xenondevs.nova.data.serialization.json.serializer.RecipeDeserializer.Companion.getRecipeId
import xyz.xenondevs.nova.ui.menu.item.recipes.group.ConversionRecipeGroup
import xyz.xenondevs.nova.util.item.ItemUtils
import java.io.File

class WasherRecipe(
    override val results: List<ItemStack>,
    override val id: ResourceLocation,
    override val input: RecipeChoice,
    private val chances: List<Int>
) : NovaRecipe, MultiResultRecipe, SingleInputChoiceRecipe {
    override val type = WASHER_RECIPE
    fun getRandomResult(): ItemStack {
        val chance = (1..100).random()
        val previousChance : Int = 1
        for (index in (0..chances.size)){
            if (chance in (previousChance..previousChance+chances[chance])) return results[index]
        }
        return results[0]
    }
}

object WasherRecipeDeserializer : RecipeDeserializer<WasherRecipe> {
    override fun deserialize(json: JsonObject, file: File): WasherRecipe {
        var results: ArrayList<ItemStack> = ArrayList()
        var chances: ArrayList<Int> = ArrayList()
        
        for (result in json.getArray("results")) {
            results.add(ItemUtils.getItemBuilder(result.asJsonArray.get(0).asString).get())
            chances.add(result.asJsonArray.get(1).asInt)
        }
        
        return WasherRecipe(results, getRecipeId(file), RecipeDeserializer.parseRecipeChoice(json.get("input")), chances)
    }
}

fun getWasherRecipeFor(input: ItemStack): WasherRecipe? {
    return RecipeManager.novaRecipes[WASHER_RECIPE]?.values?.asSequence()
        ?.map { it as WasherRecipe }
        ?.firstOrNull { recipe ->
            recipe.input.test(input)
        }
}

object WasherRecipeGroup : ConversionRecipeGroup<WasherRecipe>() {
    override val priority = 4
    override val icon = Items.washer_item.basicClientsideProvider
    override val texture = GuiTextures.RECIPE_WASHER
}
