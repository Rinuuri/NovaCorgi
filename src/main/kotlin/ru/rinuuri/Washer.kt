package ru.rinuuri

import net.minecraft.core.particles.ParticleTypes
import net.minecraft.resources.ResourceLocation
import org.bukkit.inventory.ItemStack
import ru.rinuuri.Blocks.WASHER
import xyz.xenondevs.commons.provider.mutable.mapNonNull
import xyz.xenondevs.invui.gui.Gui
import xyz.xenondevs.invui.inventory.event.ItemPreUpdateEvent
import xyz.xenondevs.nmsutils.particle.particle
import xyz.xenondevs.nova.data.config.entry
import xyz.xenondevs.nova.data.recipe.NovaRecipe
import xyz.xenondevs.nova.data.recipe.RecipeManager
import xyz.xenondevs.nova.data.world.block.state.NovaTileEntityState
import xyz.xenondevs.nova.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.tileentity.menu.TileEntityMenuClass
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.energy.holder.ConsumerEnergyHolder
import xyz.xenondevs.nova.tileentity.network.fluid.FluidType
import xyz.xenondevs.nova.tileentity.network.fluid.holder.NovaFluidHolder
import xyz.xenondevs.nova.tileentity.network.item.holder.NovaItemHolder
import xyz.xenondevs.nova.ui.EnergyBar
import xyz.xenondevs.nova.ui.FluidBar
import xyz.xenondevs.nova.ui.config.side.OpenSideConfigItem
import xyz.xenondevs.nova.ui.config.side.SideConfigMenu
import xyz.xenondevs.nova.ui.item.ProgressItem
import xyz.xenondevs.nova.util.BlockSide
import xyz.xenondevs.nova.util.advance
import xyz.xenondevs.nova.util.intValue
import kotlin.math.max

private val MAX_ENERGY = WASHER.config.entry<Long>("capacity")
private val ENERGY_PER_TICK = WASHER.config.entry<Long>("energy_per_tick")
private val WATER_CAPACITY = WASHER.config.entry<Long>("water_capacity")
private val WATER_CONSUMPTION = WASHER.config.entry<Long>("water_consumption")
private val WASH_TIME = 100
class Washer(blockState: NovaTileEntityState) : NetworkedTileEntity(blockState) {
    
    private var timeLeft by storedValue("wash_time_left") { 0 }
    
    private var currentRecipe: WasherRecipe? by storedValue<ResourceLocation>("currentRecipe").mapNonNull(
        { RecipeManager.getRecipe(RecipeTypes.WASHER_RECIPE, it) },
        NovaRecipe::id
    )
    
    private var result: ItemStack? = null
    
    
    private val inputInv = getInventory("input", 1, ::handleInputUpdate)
    private val outputInv = getInventory("output", 3, ::handleOutputUpdate)
    override val energyHolder = ConsumerEnergyHolder(
        this,
        MAX_ENERGY,
        ENERGY_PER_TICK,
        null
    ) { createSideConfig(NetworkConnectionType.INSERT, BlockSide.FRONT) }
    override val itemHolder = NovaItemHolder(
        this,
        inputInv to NetworkConnectionType.INSERT,
        outputInv to NetworkConnectionType.EXTRACT
    ) { createSideConfig(NetworkConnectionType.BUFFER, BlockSide.FRONT) }
    private val waterTank = getFluidContainer(
        "water_tank",
        setOf(FluidType.WATER),
        WATER_CAPACITY,
        0,
        ::updateWaterLevel
    )
    
    //override val fluidHolder = NovaFluidHolder(this, waterTank to NetworkConnectionType.INSERT) { createSideConfig(NetworkConnectionType.INSERT, BlockSide.FRONT) }
    override val fluidHolder = NovaFluidHolder(
        this,
        waterTank to NetworkConnectionType.INSERT
    ) { createSideConfig(NetworkConnectionType.INSERT, BlockSide.FRONT) }
    
    private var active: Boolean = false
        set(active) {
            if (field != active) {
                field = active
                blockState.modelProvider.update(active.intValue)
            }
        }
    private fun updateWaterLevel() {
        if (active && waterTank.amount == 0.toLong()) active = false
        else if (!active) active = true
    }
    
    private fun handleInputUpdate(event: ItemPreUpdateEvent) {
        event.isCancelled = event.newItem != null && getWasherRecipeFor(event.newItem!!) == null
    }
    
    private fun handleOutputUpdate(event: ItemPreUpdateEvent) {
        event.isCancelled = !event.isRemove && event.updateReason != SELF_UPDATE_REASON
    }
    
    override fun handleInitialized(first: Boolean) {
        super.handleInitialized(first)
        active = waterTank.amount > 0
    }
    
    private val particleTask = createPacketTask(listOf(
        particle(ParticleTypes.UNDERWATER) {
            location(centerLocation.advance(getFace(BlockSide.FRONT), 0.6).apply { y += 0.5 })
            offset(0.2, 0.2, 0.2)
            speed(0f)
            amount(2)
        }
    ), 6)
    
    override fun handleTick() {
        if ((energyHolder.energy >= energyHolder.energyConsumption && waterTank.amount >= WATER_CONSUMPTION.value) && !outputInv.isFull) {
            if (timeLeft == 0) {
                takeItem()
                if (particleTask.isRunning()) particleTask.stop()
            } else {
                timeLeft = max(timeLeft - 1, 0)
                energyHolder.energy -= energyHolder.energyConsumption
                waterTank.takeFluid(WATER_CONSUMPTION.value)
                
                if (!particleTask.isRunning()) particleTask.start()
                
                if (timeLeft == 0) {
                    outputInv.addItem(SELF_UPDATE_REASON, result!!)
                    currentRecipe = null
                }
                
                menuContainer.forEachMenu(WasherMenu::updateProgress)
            }
            
        } else if (particleTask.isRunning()) particleTask.stop()
    }
    
    private fun takeItem() {
        val inputItem = inputInv.getItem(0)
        if (inputItem != null) {
            val recipe =  getWasherRecipeFor(inputItem)!!
            result = recipe.getRandomResult()
            if (outputInv.canHold(result!!)) {
                inputInv.addItemAmount(SELF_UPDATE_REASON, 0, -1)
                timeLeft = 100
                currentRecipe = recipe
            }
        }
    }
    
    @TileEntityMenuClass
    private inner class WasherMenu : GlobalTileEntityMenu() {
        private var progress: ProgressItem = WasherProgressItem()
        
        private val sideConfigGui = SideConfigMenu(this@Washer,
            listOf(itemHolder.getNetworkedInventory(inputInv) to "inventory.nova.input", itemHolder.getNetworkedInventory(outputInv) to "inventory.nova.output"),
            listOf(waterTank to "container.nova.water_tank"),
            ::openWindow
        )
        override val gui = Gui.normal()
            .setStructure(
                "1 - - - - - - - 2",
                "| c # # # o w e |",
                "| i # p # o w e |",
                "| # # # # o w e |",
                "3 - - - - - - - 4")
            .addIngredient('e', EnergyBar(3, energyHolder))
            .addIngredient('w', FluidBar(3, fluidHolder, waterTank))
            .addIngredient('c', OpenSideConfigItem(sideConfigGui))
            .addIngredient('i', inputInv)
            .addIngredient('o', outputInv)
            .addIngredient('p', progress)
            .build()
        init {
            updateProgress()
        }
        
        fun updateProgress() {
            val percentage = if (timeLeft == 0) 0.0 else (WASH_TIME - timeLeft).toDouble() / WASH_TIME.toDouble()
            progress.percentage = percentage
        }
    }
}