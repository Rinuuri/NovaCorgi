package ru.rinuuri

import net.minecraft.resources.ResourceLocation
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.commons.provider.mutable.mapNonNull
import xyz.xenondevs.invui.gui.Gui
import xyz.xenondevs.invui.inventory.event.ItemPreUpdateEvent
import xyz.xenondevs.nova.data.recipe.NovaRecipe
import xyz.xenondevs.nova.data.recipe.RecipeManager
import xyz.xenondevs.nova.data.world.block.state.NovaTileEntityState
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.tileentity.menu.TileEntityMenuClass
import xyz.xenondevs.nova.ui.item.ProgressItem
import xyz.xenondevs.nova.world.block.context.BlockInteractContext

class Sieve(blockState: NovaTileEntityState) : TileEntity(blockState) {
    private val PROGRESS_PER_BUCKET = 80
    private val WATER_CONSUMPTION = 100
    
    private val inputInv = getInventory("input", 1, ::handleInputUpdate)
    private val outputInv = getInventory("output", 3, ::handleOutputUpdate)
    
    private var waterLeft: Int = 100
    
    private var menu: SieveMenu? = null
    
    private var currentRecipe: WasherRecipe? by storedValue<ResourceLocation>("currentRecipe").mapNonNull(
        { RecipeManager.getRecipe(RecipeTypes.WASHER_RECIPE, it) },
        NovaRecipe::id
    )
    
    private var result: ItemStack? = currentRecipe?.getRandomResult()
    
    private val progress: ProgressItem = WasherProgressItem()
    
    override fun handleInitialized(first: Boolean) {
        super.handleInitialized(first)
        val wl = storedValue<Int>("waterLeft").value
        if (wl != null) waterLeft = wl
        else storeData<Int>("waterLeft", waterLeft)
        result = currentRecipe?.getRandomResult()
        menu = SieveMenu()
    }
    
    private fun handleInputUpdate(event: ItemPreUpdateEvent) {
        event.isCancelled = event.newItem != null && getWasherRecipeFor(event.newItem!!) == null
        if (!event.isCancelled && event.previousItem != null && event.previousItem!!.type != event.newItem!!.type)  currentRecipe = getWasherRecipeFor(event.newItem!!)
    }
    
    private fun handleOutputUpdate(event: ItemPreUpdateEvent) {
        event.isCancelled = !event.isRemove && event.updateReason != SELF_UPDATE_REASON
    }
    
    override fun handleRightClick(ctx: BlockInteractContext): Boolean {
        
        if (ctx.source !is Player) return true
        if (outputInv.isFull) return true
        
        if (ctx.item == null ||
            ctx.item!!.type != Material.WATER_BUCKET ||
            (result != null && !outputInv.canHold(result!!))){
            
            menu?.openWindow(ctx.source as Player)
            return true
        }
        if (inputInv.isEmpty) return true
        
        ctx.item!!.type = Material.BUCKET
        
        waterLeft -= PROGRESS_PER_BUCKET
        if (waterLeft <= 0) takeItem()
        progress.percentage = if (waterLeft <= 0) 0.0 else (WATER_CONSUMPTION - waterLeft).toDouble() / WATER_CONSUMPTION.toDouble()
        return true
    }
    
    private fun takeItem(){
        val inputItem = inputInv.getItem(0)
        if (inputItem != null) {
            val recipe =  getWasherRecipeFor(inputItem)!!
            result = recipe.getRandomResult()
            if (outputInv.canHold(result!!)) {
                inputInv.addItemAmount(SELF_UPDATE_REASON, 0, -1)
                waterLeft += WATER_CONSUMPTION
                currentRecipe = recipe
                outputInv.addItem(SELF_UPDATE_REASON, result!!)
            }
        }
    }
    
    override fun saveData() {
        super.saveData()
        storeData<Int>("waterLeft", waterLeft)
    }
    
    @TileEntityMenuClass
    private inner class SieveMenu : GlobalTileEntityMenu() {
        
        override val gui = Gui.normal()
            .setStructure(
                "1 - - - - - - - 2",
                "| # # # # # o # |",
                "| # i # p # o # |",
                "| # # # # # o # |",
                "3 - - - - - - - 4")
            .addIngredient('i', inputInv)
            .addIngredient('o', outputInv)
            .addIngredient('p', progress)
            .build()
        init {
            progress.percentage = if (waterLeft == WATER_CONSUMPTION) 0.0 else (WATER_CONSUMPTION - waterLeft).toDouble() / WATER_CONSUMPTION.toDouble()
        }
    }
}