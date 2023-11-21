package ru.rinuuri

import com.google.gson.JsonObject
import ru.rinuuri.RecipeTypes.WASHER_RECIPE
import net.minecraft.resources.ResourceLocation
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.RecipeChoice
import ru.rinuuri.RecipeTypes.BLOOMERY_RECIPE
import xyz.xenondevs.commons.gson.getArray
import xyz.xenondevs.commons.gson.getString
import xyz.xenondevs.invui.gui.Gui
import xyz.xenondevs.invui.gui.ScrollGui
import xyz.xenondevs.nova.data.recipe.MultiInputChoiceRecipe
import xyz.xenondevs.nova.data.recipe.MultiResultRecipe
import xyz.xenondevs.nova.data.recipe.NovaRecipe
import xyz.xenondevs.nova.data.recipe.RecipeManager
import xyz.xenondevs.nova.data.recipe.SingleInputChoiceRecipe
import xyz.xenondevs.nova.data.recipe.SingleResultRecipe
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
        
        return ScrollGui.items()
            .setStructure(
                ". x x x x x x x .",
                ". . i . . . r . .",
                ". . . . . . . . .")
            .addIngredient('i', createRecipeChoiceItem(recipe.input))
            .addIngredient('r', createRecipeChoiceItem(recipe.results))
            .build()
    }
    
}


class BloomeryRecipe(
    override val inputs: List<RecipeChoice>,
    override val id: ResourceLocation,
    override val result: ItemStack
) : NovaRecipe, SingleResultRecipe, MultiInputChoiceRecipe {
    override val type = BLOOMERY_RECIPE
}

object BloomeryRecipeDeserializer : RecipeDeserializer<BloomeryRecipe> {
    override fun deserialize(json: JsonObject, file: File): BloomeryRecipe {
        val inputs: ArrayList<RecipeChoice> = ArrayList()
        
        for (result in json.getArray("inputs")) {
            inputs.add(RecipeChoice.ExactChoice(ItemUtils.getItemBuilder(result.asString).get()))
        }
        
        return BloomeryRecipe(inputs, getRecipeId(file), ItemUtils.getItemBuilder(json.getString("result")).get())
    }
}
object BloomeryRecipeGroup : RecipeGroup<BloomeryRecipe>() {
    override val priority = 3
    override val icon = Items.bloomery_item.basicClientsideProvider
    override val texture = GuiTextures.RECIPE_BLOOMERY
    override fun createGui(recipe: BloomeryRecipe): Gui {
        return ScrollGui.items()
            .setStructure(
                ". x x x x x x x .",
                ". f . i . . r . .",
                ". . . s . . . . .")
            .addIngredient('f', createRecipeChoiceItem(Bloomery.getFuel()))
            .addIngredient('i', createRecipeChoiceItem(recipe.inputs[0]))
            .addIngredient('s', createRecipeChoiceItem(recipe.inputs[1]))
            .addIngredient('r', recipe.result)
            .build()
    }
    
}
