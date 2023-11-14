package ru.rinuuri

import com.google.gson.JsonObject
import net.md_5.bungee.api.chat.TranslatableComponent
import ru.rinuuri.RecipeTypes.WASHER_RECIPE
import net.minecraft.resources.ResourceLocation
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.RecipeChoice
import xyz.xenondevs.commons.gson.getArray
import xyz.xenondevs.invui.gui.Gui
import xyz.xenondevs.invui.gui.ScrollGui
import xyz.xenondevs.nova.data.recipe.MultiResultRecipe
import xyz.xenondevs.nova.data.recipe.NovaRecipe
import xyz.xenondevs.nova.data.recipe.RecipeManager
import xyz.xenondevs.nova.data.recipe.SingleInputChoiceRecipe
import xyz.xenondevs.nova.data.serialization.json.serializer.RecipeDeserializer
import xyz.xenondevs.nova.data.serialization.json.serializer.RecipeDeserializer.Companion.getRecipeId
import xyz.xenondevs.nova.ui.menu.item.recipes.createRecipeChoiceItem
import xyz.xenondevs.nova.ui.menu.item.recipes.group.RecipeGroup
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
        var previousChance = 0
        for (index in chances.indices) {
            val currentRange = previousChance + chances[index]
            if (chance <= currentRange) {
                return results[index]
            }
            previousChance = currentRange
        }
        return results[0]
    }
}

object WasherRecipeDeserializer : RecipeDeserializer<WasherRecipe> {
    override fun deserialize(json: JsonObject, file: File): WasherRecipe {
        val results: ArrayList<ItemStack> = ArrayList()
        val chances: ArrayList<Int> = ArrayList()
        
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

object WasherRecipeGroup : RecipeGroup<WasherRecipe>() {
    override val priority = 4
    override val icon = Items.sieve_item.basicClientsideProvider
    override val texture = GuiTextures.RECIPE_WASHER
    override fun createGui(recipe: WasherRecipe): Gui {
        val progressItem = GuiMaterials.WASHER_PROGRESS.createClientsideItemBuilder()
        val translate = "menu.corgidash.recipe.washer_sieve"
        
        progressItem.setDisplayName(TranslatableComponent(translate))
        
        return ScrollGui.items()
            .setStructure(
                ". x x x x x x x .",
                ". . i . . . r . .",
                ". . . . . . . . .")
            .addIngredient('i', createRecipeChoiceItem(recipe.input))
            .addIngredient('p', progressItem)
            .addIngredient('r', createRecipeChoiceItem(recipe.results))
            .build()
    }
    
}