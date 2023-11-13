package ru.rinuuri

import org.bukkit.Material
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.addon.registry.BlockRegistry
import xyz.xenondevs.nova.addon.registry.ItemRegistry
import xyz.xenondevs.nova.addon.registry.RecipeTypeRegistry
import xyz.xenondevs.nova.data.world.block.property.Directional
import xyz.xenondevs.nova.initialize.Init
import xyz.xenondevs.nova.initialize.InitStage
import xyz.xenondevs.nova.item.options.BlockOptions
import xyz.xenondevs.nova.item.tool.VanillaToolCategories
import xyz.xenondevs.nova.item.tool.VanillaToolTiers
import xyz.xenondevs.nova.ui.item.ProgressItem
import xyz.xenondevs.nova.ui.overlay.character.gui.GuiTexture
import xyz.xenondevs.nova.world.block.sound.SoundGroup
object NovaCorgi : Addon() {
    
    override fun init() {
        // Called when the addon is initialized.
    }
    
    override fun onEnable() {
    }
    
    override fun onDisable() {
        // Called when the addon is disabled.
    }
}

@Init(stage = InitStage.PRE_PACK)
object Items : ItemRegistry by NovaCorgi.registry {
    val titanium_ingot = registerItem("titanium_ingot")
    val aluminum_ingot = registerItem("aluminum_ingot")
    val plutonium_ingot = registerItem("plutonium_ingot")
    val steel_ingot = registerItem("steel_ingot")
    val uranium_ingot = registerItem("uranium_ingot")
    val washer_item = registerItem(Blocks.WASHER)
    val sieve_item = registerItem(Blocks.SIEVE)
}
object GuiTextures {
    val RECIPE_WASHER = GuiTexture.of(NovaCorgi, "recipe_washer")
}
@Init(stage = InitStage.PRE_PACK)
object Blocks : BlockRegistry by NovaCorgi.registry {
    private val STONE = BlockOptions(3.0, VanillaToolCategories.PICKAXE, VanillaToolTiers.WOOD, true, SoundGroup.STONE, Material.NETHERITE_BLOCK)
    private val SIEVE_OPTIONS = BlockOptions(1.3, VanillaToolCategories.AXE, VanillaToolTiers.WOOD, false, SoundGroup.WOOD, Material.OAK_LOG)
    val WASHER = tileEntity("washer", ::Washer).blockOptions(STONE).properties(Directional.NORMAL).register()
    val SIEVE = tileEntity("sieve", ::Sieve).blockOptions(SIEVE_OPTIONS).register()
}
@Init(stage = InitStage.PRE_PACK)
object GuiMaterials : ItemRegistry by NovaCorgi.registry {
    val WASHER_PROGRESS = registerUnnamedHiddenItem("gui_washer_progress")
}
@Init(stage = InitStage.POST_PACK_PRE_WORLD)
object RecipeTypes : RecipeTypeRegistry by NovaCorgi.registry {
    val WASHER_RECIPE = registerRecipeType("washer", WasherRecipe::class, WasherRecipeGroup, WasherRecipeDeserializer)
}

class WasherProgressItem : ProgressItem(GuiMaterials.WASHER_PROGRESS, 12)