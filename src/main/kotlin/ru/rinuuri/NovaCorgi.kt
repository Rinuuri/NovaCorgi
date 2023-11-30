package ru.rinuuri

import org.bukkit.Material
import org.bukkit.inventory.EquipmentSlot
import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.addon.registry.BlockRegistry
import xyz.xenondevs.nova.addon.registry.ItemRegistry
import xyz.xenondevs.nova.addon.registry.RecipeTypeRegistry
import xyz.xenondevs.nova.data.world.block.property.Directional
import xyz.xenondevs.nova.initialize.Init
import xyz.xenondevs.nova.initialize.InitStage
import xyz.xenondevs.nova.item.behavior.Damageable
import xyz.xenondevs.nova.item.behavior.Enchantable
import xyz.xenondevs.nova.item.behavior.Extinguishing
import xyz.xenondevs.nova.item.behavior.Flattening
import xyz.xenondevs.nova.item.behavior.Stripping
import xyz.xenondevs.nova.item.behavior.Tilling
import xyz.xenondevs.nova.item.behavior.Tool
import xyz.xenondevs.nova.item.behavior.Wearable
import xyz.xenondevs.nova.item.options.BlockOptions
import xyz.xenondevs.nova.item.tool.VanillaToolCategories
import xyz.xenondevs.nova.item.tool.VanillaToolTiers
import xyz.xenondevs.nova.ui.item.ProgressItem
import xyz.xenondevs.nova.ui.overlay.character.gui.GuiTexture
import xyz.xenondevs.nova.world.block.sound.SoundGroup
object NovaCorgi : Addon() {
    override fun onEnable() {
    }
}

@Init(stage = InitStage.PRE_PACK)
object Items : ItemRegistry by NovaCorgi.registry {
    val titanium_ingot = registerItem("titanium_ingot")
    val aluminum_ingot = registerItem("aluminum_ingot")
    val plutonium_ingot = registerItem("plutonium_ingot")
    val uranium_ingot = registerItem("uranium_ingot")
    val steel_ingot = registerItem("steel_ingot")
    val bronze_ingot = registerItem("bronze_ingot")
    val titanium_plate = registerItem("titanium_plate")
    val titanium_gear = registerItem("titanium_gear")
    val bronze_plate = registerItem("bronze_plate")
    val bronze_gear = registerItem("bronze_gear")
    val aluminum_gear = registerItem("aluminum_gear")
    val aluminum_plate = registerItem("aluminum_plate")
    val bauxit_dust = registerItem("bauxit_dust")
    val steel_gear = registerItem("steel_gear")
    val steel_plate = registerItem("steel_plate")
    val basalt_dust = registerItem("basalt_dust")
    val copper_nugget = registerItem("copper_nugget")
    val aluminum_nugget = registerItem("aluminum_nugget")
    val bronze_nugget = registerItem("bronze_nugget")
    val finery_iron = registerItem("finery_iron")
    val coal_coke = registerItem("coal_coke")
    
    val bronze_helmet = registerItem("bronze_helmet", Wearable(EquipmentSlot.HEAD, "minecraft:item.armor.equip_gold"), Damageable)
    val bronze_chestplate = registerItem("bronze_chestplate", Wearable(EquipmentSlot.CHEST, "minecraft:item.armor.equip_gold"), Damageable)
    val bronze_leggings = registerItem("bronze_leggings", Wearable(EquipmentSlot.LEGS, "minecraft:item.armor.equip_gold"), Damageable)
    val bronze_boots = registerItem("bronze_boots", Wearable(EquipmentSlot.FEET, "minecraft:item.armor.equip_gold"), Damageable)
    
    val bronze_axe = registerItem("bronze_axe", Tool, Damageable, Enchantable, Stripping)
    val bronze_pickaxe = registerItem("bronze_pickaxe", Tool, Damageable, Enchantable)
    val bronze_hoe = registerItem("bronze_hoe", Tool, Damageable, Enchantable, Tilling)
    val bronze_shovel = registerItem("bronze_shovel", Tool, Damageable, Enchantable, Flattening, Extinguishing)
    val bronze_sword = registerItem("bronze_sword", Tool, Damageable, Enchantable)
    
    val copper_axe = registerItem("copper_axe", Tool, Damageable, Enchantable, Stripping)
    val copper_pickaxe = registerItem("copper_pickaxe", Tool, Damageable, Enchantable)
    val copper_hoe = registerItem("copper_hoe", Tool, Damageable, Enchantable, Tilling)
    val copper_shovel = registerItem("copper_shovel", Tool, Damageable, Enchantable, Flattening, Extinguishing)
    val copper_sword = registerItem("copper_sword", Tool, Damageable, Enchantable)
    //val copper_shears = registerItem("copper_shears", Tool, Damageable, Enchantable)
    
    val washer_item = registerItem(Blocks.WASHER)
    val sieve_item = registerItem(Blocks.SIEVE)
    val bloomery_item = registerItem(Blocks.BLOOMERY)
    val bloomery_chimney_item = registerItem(Blocks.BLOOMERY_CHIMNEY)
    val bronze_block_item = registerItem(Blocks.BRONZE_BLOCK)
    val aluminum_block_item = registerItem(Blocks.ALUMINUM_BLOCK)
    val blast_furnace_item = registerItem(Blocks.BLAST_FURNACE)
}
object GuiTextures {
    val RECIPE_WASHER = GuiTexture.of(NovaCorgi, "recipe_washer")
    val RECIPE_BLOOMERY = GuiTexture.of(NovaCorgi, "recipe_bloomery")
    val RECIPE_BLAST_FURNACE = GuiTexture.of(NovaCorgi, "recipe_blast_furnace")
}
@Init(stage = InitStage.PRE_PACK)
object Blocks : BlockRegistry by NovaCorgi.registry {
    private val STONE = BlockOptions(3.0, VanillaToolCategories.PICKAXE, VanillaToolTiers.WOOD, true, SoundGroup.STONE, Material.NETHERITE_BLOCK)
    private val COPPER = BlockOptions(4.0, VanillaToolCategories.PICKAXE, VanillaToolTiers.STONE, true, SoundGroup.METAL)
    private val BRONZE = BlockOptions(6.0, VanillaToolCategories.PICKAXE, VanillaToolTiers.IRON, true, SoundGroup.METAL)
    private val SIEVE_OPTIONS = BlockOptions(1.3, VanillaToolCategories.AXE, VanillaToolTiers.WOOD, false, SoundGroup.WOOD, Material.OAK_LOG)
    private val BLOOMERY_OPTIONS = BlockOptions(3.0, VanillaToolCategories.PICKAXE, VanillaToolTiers.STONE, true, SoundGroup.NETHER_BRICKS, Material.BRICKS)
    val WASHER = tileEntity("washer", ::Washer).blockOptions(STONE).properties(Directional.NORMAL).register()
    val SIEVE = tileEntity("sieve", ::Sieve).blockOptions(SIEVE_OPTIONS).register()
    val BLOOMERY = tileEntity("bloomery", ::Bloomery).blockOptions(BLOOMERY_OPTIONS).properties(Directional.NORMAL).register()
    val BLOOMERY_CHIMNEY = block("bloomery_chimney").blockOptions(BLOOMERY_OPTIONS).properties(Directional.NORMAL).register()
    val BLAST_FURNACE = tileEntity("blast_furnace", ::BlastFurnace).blockOptions(BRONZE).properties(Directional.NORMAL).register()
    
    val BRONZE_BLOCK = block("bronze_block").blockOptions(BRONZE).register()
    val ALUMINUM_BLOCK = block("aluminum_block").blockOptions(COPPER).register()
    
    val MACHINE = block("machine").blockOptions(BRONZE).behaviors(Machine).register()
}
@Init(stage = InitStage.PRE_PACK)
object GuiMaterials : ItemRegistry by NovaCorgi.registry {
    val WASHER_PROGRESS = registerUnnamedHiddenItem("gui_washer_progress")
    val BLOOMERY_PROGRESS = registerUnnamedHiddenItem("gui_bloomery_progress")
    val BLOOMERY_TEMPERATURE = registerUnnamedHiddenItem("gui_bloomery_temperature")
}
@Init(stage = InitStage.POST_PACK_PRE_WORLD)
object RecipeTypes : RecipeTypeRegistry by NovaCorgi.registry {
    val WASHER_RECIPE = registerRecipeType("washer", WasherRecipe::class, WasherRecipeGroup, WasherRecipeDeserializer)
    val BLOOMERY_RECIPE = registerRecipeType("bloomery", BloomeryRecipe::class, BloomeryRecipeGroup, BloomeryRecipeDeserializer)
    val BLAST_FURNACE_RECIPE = registerRecipeType("blast_furnace", BlastFurnaceRecipe::class, BlastFurnaceRecipeGroup, BlastFurnaceRecipeDeserializer)
}

class BloomeryProgressItem : ProgressItem(GuiMaterials.BLOOMERY_PROGRESS, 16)
class BloomeryTemperatureItem : ProgressItem(GuiMaterials.BLOOMERY_TEMPERATURE, 16)
class WasherProgressItem : ProgressItem(GuiMaterials.WASHER_PROGRESS, 12)