package ru.rinuuri

import net.minecraft.core.particles.ParticleTypes
import net.minecraft.resources.ResourceLocation
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.commons.provider.mutable.mapNonNull
import xyz.xenondevs.invui.gui.Gui
import xyz.xenondevs.invui.inventory.event.ItemPreUpdateEvent
import xyz.xenondevs.invui.inventory.get
import xyz.xenondevs.nmsutils.particle.particle
import xyz.xenondevs.nova.data.config.entry
import xyz.xenondevs.nova.data.recipe.NovaRecipe
import xyz.xenondevs.nova.data.recipe.RecipeManager
import xyz.xenondevs.nova.data.world.block.state.NovaTileEntityState
import xyz.xenondevs.nova.tileentity.TileEntity
import xyz.xenondevs.nova.tileentity.menu.TileEntityMenuClass
import xyz.xenondevs.nova.ui.item.ProgressItem
import xyz.xenondevs.nova.util.BlockSide
import xyz.xenondevs.nova.util.advance
import xyz.xenondevs.nova.util.id
import xyz.xenondevs.nova.util.intValue
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.BlockManager
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class Bloomery(blockState: NovaTileEntityState) : TileEntity(blockState) {
    
    private val REQ_TEMP = Blocks.BLOOMERY.config.entry<Int>("required_temperature")
    private val BLOOM_TIME = Blocks.BLOOMERY.config.entry<Int>("bloom_time")
    
    private var temperature: Int = 0
    
    private var tempTick: Int = 0
    
    private var bonus = 0.0F
    
    private var timeLeft by storedValue("bloom_time_left") { BLOOM_TIME.value.toFloat() }
    
    private var currentRecipe: BloomeryRecipe? by storedValue<ResourceLocation>("currentRecipe").mapNonNull(
        { RecipeManager.getRecipe(RecipeTypes.BLOOMERY_RECIPE, it) },
        NovaRecipe::id
    )
    
    private var updateInv = false
    
    private val inputInv = getInventory("input", 2, ::handleInputUpdate)
    private val fuelInv = getInventory("fuel", 1, ::handleFuelUpdate)
    private val outputInv = getInventory("output", 1, ::handleOutputUpdate)
    
    private var active: Boolean = false
        set(active) {
            if (field != active) {
                field = active
                blockState.modelProvider.update(active.intValue)
                var state = BlockManager.getBlockState(BlockPos(location.world, location.x.toInt(), (location.y+2).toInt(), location.z.toInt()))
                if (state != null) state.modelProvider.update(active.intValue)
                else {
                    state = BlockManager.getBlockState(BlockPos(location.world, location.x.toInt(), (location.y+1).toInt(), location.z.toInt()))
                    if (state != null) state.modelProvider.update(active.intValue)
                }
            }
        }
    
    override fun handleInitialized(first: Boolean) {
        super.handleInitialized(first)
        val temp = storedValue<Int>("temperature").value
        if (temp != null) temperature = temp
        val tl = storedValue<Float>("timeLeft").value
        if (tl != null) timeLeft = tl
    }
    
    private fun getBloomeryRecipeFor(recipeInput: ItemStack?, recipeInput2: ItemStack?) : BloomeryRecipe?{
        if (recipeInput == null || recipeInput2 == null) return null
        for (recipe in RecipeManager.novaRecipes[RecipeTypes.BLOOMERY_RECIPE]!!.values.asSequence().map { it as BloomeryRecipe }){
            for (input in recipe.inputs) if (input.test(recipeInput2)) {
                var inputs = recipe.inputs
                inputs = inputs.take(inputs.indexOf(input))
                for (input2 in inputs){
                    if (input2.test(recipeInput)) return recipe
                }
            }
            for (input in recipe.inputs) if (input.test(recipeInput)) {
                var inputs = recipe.inputs
                inputs = inputs.take(inputs.indexOf(input))
                for (input2 in inputs){
                    if (input2.test(recipeInput2)) return recipe
                }
            }
            if (recipe.inputs[0] == recipe.inputs[1] && recipe.inputs[0].test(recipeInput) && recipe.inputs[0].test(recipeInput2)) return recipe
        }
        return null
    }
    
    private fun isIngredient(recipeInput: ItemStack) : Boolean{
        for (recipe in RecipeManager.novaRecipes[RecipeTypes.BLOOMERY_RECIPE]!!.values.asSequence().map { it as BloomeryRecipe }){
            for (input in recipe.inputs) if (input.test(recipeInput)) {
                return true
            }
        }
        return false
    }
    
    private fun handleInputUpdate(event: ItemPreUpdateEvent) {
        if (event.newItem != null && !isIngredient(event.newItem!!)) event.isCancelled = true
        else updateInv = true
    }
    
    private fun handleOutputUpdate(event: ItemPreUpdateEvent) {
        event.isCancelled = !event.isRemove && event.updateReason != SELF_UPDATE_REASON
    }
    
    private fun handleFuelUpdate(event: ItemPreUpdateEvent) {
        event.isCancelled = event.newItem != null && !fuel.containsKey(event.newItem!!.type)
    }
    
    private val particleTask = createPacketTask(listOf(
        particle(ParticleTypes.FLAME) {
            location(centerLocation.advance(getFace(BlockSide.FRONT), 0.6).add(0.0, 0.1, 0.0))
            offset(0.2, 0.2, 0.2)
            speed(0f)
            amount(1)
        }
    ), 10)
    
    override fun handleTick() {
        
        if (updateInv){
            updateInv = false
            currentRecipe = getBloomeryRecipeFor(inputInv[0], inputInv[1])
        }
        
        if (tempTick > 0) tempTick -=1
        if (!fuelInv.isEmpty){
            if (temperature <= REQ_TEMP.value) temperature += 1
            if (tempTick <= 0) {
                tempTick = fuel.get(fuelInv.get(0)!!.type)!!
                fuelInv.setItemAmount(SELF_UPDATE_REASON, 0, fuelInv.get(0)!!.amount-1)
            }
        }
        else if (temperature >= 2) temperature -= 2
        
        if (temperature >= REQ_TEMP.value/3) {
            if (!particleTask.isRunning()) particleTask.start()
            active = true
        }
        else if (particleTask.isRunning()) {
            particleTask.stop()
            active = false
        }
        
        if (temperature >= REQ_TEMP.value && inputInv[0] != null && inputInv[1] != null && currentRecipe != null && !outputInv.isFull) {
            if (location.add(0.0,1.0,0.0).block.id == Blocks.BLOOMERY_CHIMNEY.id){
                if (location.add(0.0,2.0,0.0).block.id == Blocks.BLOOMERY_CHIMNEY.id){
                    bonus = 0.4F
                } else bonus = 0.2F
            } else bonus = 0.0F
            
            timeLeft -= 1+bonus
            if (timeLeft <= 0){
                inputInv.setItemAmount(SELF_UPDATE_REASON, 0, inputInv[0]!!.amount-1)
                inputInv.setItemAmount(SELF_UPDATE_REASON, 1, inputInv[1]!!.amount-1)
                outputInv.addItem(SELF_UPDATE_REASON, currentRecipe!!.result)
                currentRecipe = null
                timeLeft = BLOOM_TIME.value.toFloat()
            }
        }
       
        menuContainer.forEachMenu(BloomeryMenu::updateProgress)
    }
    
    override fun saveData() {
        super.saveData()
        storeData<Int>("temperature", temperature)
        storeData<Float>("bloom_time_left", timeLeft)
    }
    
    @TileEntityMenuClass
    private inner class BloomeryMenu : GlobalTileEntityMenu() {
        private var progress: ProgressItem = BloomeryProgressItem()
        private var temperatureItem: ProgressItem = BloomeryTemperatureItem()
        
        override val gui = Gui.normal()
            .setStructure(
                "1 - - - - - - - 2",
                "| # # i # # # # |",
                "| f # # # p # o |",
                "| t # i # # # # |",
                "3 - - - - - - - 4")
            .addIngredient('i', inputInv)
            .addIngredient('o', outputInv)
            .addIngredient('p', progress)
            .addIngredient('t', temperatureItem)
            .addIngredient('f', fuelInv)
            .build()
        init {
            updateProgress()
        }
        
        fun updateProgress() {
            progress.percentage = if (timeLeft == 0.0F) 0.0 else (BLOOM_TIME.value - timeLeft).toDouble() / BLOOM_TIME.value
            val temp: Int = REQ_TEMP.value - temperature
            temperatureItem.percentage = if (temp == 0) 0.0 else (REQ_TEMP.value - temp).toDouble() / REQ_TEMP.value
        }
    }
    companion object {
        private val fuel: HashMap<Material, Int> = hashMapOf(
            Material.COAL to 100,
            Material.COAL_BLOCK to 1000,
            Material.CHARCOAL to 90
        )
        
        fun getFuel(): List<ItemStack> {
            val fuelItems = ArrayList<ItemStack>()
            for (m in fuel.keys) {
                fuelItems.add(ItemStack(m))
            }
            return fuelItems
            
        }
    }
}