package ru.rinuuri

import net.minecraft.core.particles.ParticleTypes
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.block.BlockFace
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import ru.rinuuri.Bloomery.Companion.getBloomeryRecipeFor
import xyz.xenondevs.invui.gui.Gui
import xyz.xenondevs.invui.inventory.event.ItemPreUpdateEvent
import xyz.xenondevs.invui.inventory.get
import xyz.xenondevs.nmsutils.particle.particle
import xyz.xenondevs.nova.data.config.entry
import xyz.xenondevs.nova.data.recipe.RecipeManager
import xyz.xenondevs.nova.data.world.block.state.NovaTileEntityState
import xyz.xenondevs.nova.tileentity.NetworkedTileEntity
import xyz.xenondevs.nova.tileentity.menu.TileEntityMenuClass
import xyz.xenondevs.nova.tileentity.network.NetworkConnectionType
import xyz.xenondevs.nova.tileentity.network.energy.holder.ConsumerEnergyHolder
import xyz.xenondevs.nova.tileentity.network.fluid.FluidType
import xyz.xenondevs.nova.tileentity.network.item.holder.NovaItemHolder
import xyz.xenondevs.nova.ui.EnergyBar
import xyz.xenondevs.nova.ui.config.side.OpenSideConfigItem
import xyz.xenondevs.nova.ui.config.side.SideConfigMenu
import xyz.xenondevs.nova.ui.item.ProgressItem
import xyz.xenondevs.nova.util.BlockSide
import xyz.xenondevs.nova.util.advance
import xyz.xenondevs.nova.util.axis
import xyz.xenondevs.nova.util.center
import xyz.xenondevs.nova.util.intValue
import xyz.xenondevs.nova.util.isSourceFluid
import xyz.xenondevs.nova.util.item.ItemUtils
import xyz.xenondevs.nova.util.place
import xyz.xenondevs.nova.util.placeVanilla
import xyz.xenondevs.nova.util.plus
import xyz.xenondevs.nova.util.runTaskLater
import xyz.xenondevs.nova.world.BlockPos
import xyz.xenondevs.nova.world.block.BlockManager
import xyz.xenondevs.nova.world.block.context.BlockBreakContext
import xyz.xenondevs.nova.world.block.context.BlockInteractContext
import xyz.xenondevs.nova.world.block.context.BlockPlaceContext
import java.util.LinkedList

class BlastFurnace(blockState: NovaTileEntityState) : NetworkedTileEntity(blockState){
    
    private val MAX_ENERGY = Blocks.BLAST_FURNACE.config.entry<Long>("capacity")
    private val ENERGY_PER_TICK = Blocks.BLAST_FURNACE.config.entry<Long>("energy_per_tick")
    private val BLAST_TIME = Blocks.BLAST_FURNACE.config.entry<Int>("blast_time")
    private val REQ_TEMP = Blocks.BLAST_FURNACE.config.entry<Int>("required_temperature")
    
    private var timeLeft : Int = BLAST_TIME.value
    private var intact : Boolean = false
        set(intact) {
            if (field != intact) {
                field = intact
                if (intact) construct()
                else deconstruct()
            }
        }
    private var lastIntactCheck = 200
    
    private var temperature = 0
    
    private var result: ItemStack? = null
        set(result) {
            if (field != result) {
                field = result
                if (field == null || result == null || field!!.type != result.type){
                    timeLeft = REQ_TEMP.value
                }
            }
        }
    private var speed: Int = 1
    private val inputInv = getInventory("input", 2, ::handleInputUpdate)
    private val outputInv = getInventory("output", 1, ::handleOutputUpdate)
    
    private var struct: HashMap<BlockPos, Material> = HashMap()
    private val placestack = ItemStack(Material.IRON_BLOCK)
    
    private var updateInv = false
    private var active: Boolean = false
        set(active) {
            if (field != active) {
                field = active
                blockState.modelProvider.update(active.intValue)
            }
        }
    
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
    
    private val particleTask = createPacketTask(listOf(
        particle(ParticleTypes.SMOKE) {
            location(centerLocation.advance(getFace(BlockSide.FRONT), 0.6).apply { y += 0.5 })
            offset(0.3, 0.3, 0.3)
            speed(0f)
            amount(6)
        }
    ), 3)
    
    private val lavaParticleTask = createPacketTask(listOf(
        particle(ParticleTypes.LAVA) {
            location(centerLocation.advance(getFace(BlockSide.FRONT), 0.6).apply { y += 0.6 })
            offset(getFace(BlockSide.RIGHT).axis, 0.15f)
            offsetY(0.1f)
        }
    ), 200)
    
    private fun getRecipeFor(recipeInput: ItemStack?, recipeInput2: ItemStack?) : BlastFurnaceRecipe?{
        if (recipeInput == null || recipeInput2 == null) return null
        val r = RecipeManager.novaRecipes[RecipeTypes.BLAST_FURNACE_RECIPE] ?: return null
        for (recipe in r.values.asSequence().map { it as BlastFurnaceRecipe }){
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
        val r = RecipeManager.novaRecipes[RecipeTypes.BLAST_FURNACE_RECIPE]
        if (r != null) {
            for (recipe in r.values.asSequence().map { it as BlastFurnaceRecipe }) {
                for (input in recipe.inputs) if (input.test(recipeInput)) {
                    return true
                }
            }
        }
        for (recipe in RecipeManager.novaRecipes[RecipeTypes.BLOOMERY_RECIPE]!!.values.asSequence().map { it as BloomeryRecipe }){
            for (input in recipe.inputs) if (input.test(recipeInput)) {
                return true
            }
        }
        return false
    }
    
    override fun handleInitialized(first: Boolean) {
        super.handleInitialized(first)
        val map = storedValue<HashMap<Location, Material>>("struct").value
        if (map == null) generateStructure()
        else if (struct.isEmpty())
            for (l in map.keys.toTypedArray())
                struct.put(BlockPos(l.world, l.blockX, l.blockY, l.blockZ), map[l]!!)
        
        if (!first) {
            temperature = storedValue<Int>("temperature").value!!
            if (inputInv[0] != null && inputInv[1] != null) result = getResult(inputInv[0], inputInv[1])
            intact = true
        }
    }
    
    override fun saveData() {
        super.saveData()
        storeData("temperature", temperature)
        val data = HashMap<Location, Material>()
        for (bp in struct.keys.toTypedArray())
            data.put(bp.location, struct[bp]!!)
        storeData<HashMap<Location, Material>>("struct", data)
    }
    
    override fun handleRemoved(unload: Boolean) {
        super.handleRemoved(unload)
        if (unload) return
        intact = false
    }
    
    private fun handleInputUpdate(event: ItemPreUpdateEvent) {
        if (event.newItem != null && !isIngredient(event.newItem!!)) event.isCancelled = true
        else updateInv = true
    }
    private fun handleOutputUpdate(event: ItemPreUpdateEvent) {
        event.isCancelled = !event.isRemove && event.updateReason != SELF_UPDATE_REASON
    }
    
    private fun construct(){
        for (sblock in struct.keys.toTypedArray()) {
            if (sblock.block.type == Material.IRON_BLOCK) {
                BlockManager.placeBlockState(Blocks.MACHINE,
                    BlockPlaceContext(sblock, placestack, null, null, null, sblock, BlockFace.SELF))
            }
        }
    }
    
    private fun deconstruct(){
        val states = LinkedList<Location>()
        for (sblock in struct.keys.toTypedArray()){
            val sblockstate = BlockManager.getBlockState(sblock)
            if (sblockstate != null && sblockstate.block == Blocks.MACHINE){
                BlockManager.removeBlockState(BlockBreakContext(sblock), false)
                states.add(sblock.location)
            }
        }
        runTaskLater(1){
            for (state in states){
                state.block.type = Material.IRON_BLOCK
            }
        }
    }
    
    private fun getResult(ing1: ItemStack?, ing2: ItemStack?) : ItemStack?{
        val rec = getRecipeFor(ing1, ing2)
        if (rec != null) {speed = 1;return rec.result}
        val brec = getBloomeryRecipeFor(ing1, ing2)
        if (brec != null) {speed = 2;return brec.result}
        return null
    }
    override fun handleTick() {
        if (struct.isEmpty()) return
        
        lastIntactCheck -= 1
        if (lastIntactCheck <= 0){
            lastIntactCheck = 200
            if (!isIntact()) return
        }
        if (!intact) return
        temperature -= 1
        if (energyHolder.energy >= energyHolder.energyConsumption) {
            if (temperature < REQ_TEMP.value) {
                energyHolder.energy -= energyHolder.energyConsumption
                temperature += 3
            }
        }
        menuContainer.forEachMenu(BlastFurnaceMenu::updateTemperature)
        if (temperature >= REQ_TEMP.value/2) {
            if (!particleTask.isRunning()) particleTask.start()
            if (!lavaParticleTask.isRunning()) lavaParticleTask.start()
            active = true
        } else {
            if (particleTask.isRunning()) particleTask.stop()
            if (lavaParticleTask.isRunning()) lavaParticleTask.stop()
            active = false
        }
        
        if (inputInv[0] == null || inputInv[1] == null) return
        if (updateInv) {
            result = getResult(inputInv[0], inputInv[1])
            updateInv = false
        }
        
        if (result == null) return
        if (temperature+1 < REQ_TEMP.value) return
        if (outputInv.isFull || !outputInv.canHold(result!!)) return
        timeLeft -= speed
        menuContainer.forEachMenu(BlastFurnaceMenu::updateProgress)
        if (timeLeft > 0) return
        timeLeft = BLAST_TIME.value
        inputInv.setItemAmount(SELF_UPDATE_REASON, 0, inputInv[0]!!.amount-1)
        inputInv.setItemAmount(SELF_UPDATE_REASON, 1, inputInv[1]!!.amount-1)
        outputInv.addItem(SELF_UPDATE_REASON, result!!)
    }
    
    private fun isIntact(): Boolean{
        for (sblock in struct.keys.toTypedArray()){
            if (sblock.block.type != struct[sblock]) {
                if (struct[sblock]!! == Material.IRON_BLOCK){
                    val blockState = BlockManager.getBlockState(sblock)
                    if (blockState != null && blockState.block == Blocks.MACHINE) continue
                }
                sblock.world.spawnParticle(Particle.VILLAGER_ANGRY, sblock.location.center(), 3, 0.2,0.2,0.2)
                if (intact) intact = false
                return false
            }
            else if (sblock.block.type == Material.LAVA && !sblock.block.isSourceFluid()) {
                if (intact) intact = false
                return false
            }
        }
        if (!intact) intact = true
        return true
    }
    
    @SuppressWarnings("DEPRECATED")
    override fun handleUnknownRightClick(ctx: BlockInteractContext): Boolean {
        if (ctx.source !is Player) return false
        
        if (intact) {
            menuContainer.openWindow(ctx.source as Player)
            return true
        }
        if (isIntact()) {
            (ctx.source as Player).sendMessage(ChatColor.GREEN+"Вы успешно построили доменную печь!")
            timeLeft = BLAST_TIME.value
            return true
        }
        (ctx.source as Player).sendMessage(ChatColor.RED+"Многоблочная структура построена неверно!")
        return true
    }
    
    @TileEntityMenuClass
    private inner class BlastFurnaceMenu : GlobalTileEntityMenu() {
        private var progress: ProgressItem = BloomeryProgressItem()
        private var temperatureItem: ProgressItem = BloomeryTemperatureItem()
        
        private val sideConfigGui = SideConfigMenu(this@BlastFurnace,
            listOf(itemHolder.getNetworkedInventory(inputInv) to "inventory.nova.input", itemHolder.getNetworkedInventory(outputInv) to "inventory.nova.output"),
            ::openWindow
        )
        
        override val gui = Gui.normal()
            .setStructure(
                "1 - - - - - - - 2",
                "| # # i # # # e |",
                "| t # # p o # e |",
                "| # # i # # # e |",
                "3 - - - - - - - 4")
            .addIngredient('e', EnergyBar(3, energyHolder))
            .addIngredient('c', OpenSideConfigItem(sideConfigGui))
            .addIngredient('i', inputInv)
            .addIngredient('o', outputInv)
            .addIngredient('t', temperatureItem)
            .addIngredient('p', progress)
            .build()
        init {
            updateProgress()
        }
        
        fun updateProgress() {
            progress.percentage = if (timeLeft == 0) 0.0 else (BLAST_TIME.value - timeLeft).toDouble() / BLAST_TIME.value
        }
        fun updateTemperature(){
            val temp: Int = REQ_TEMP.value - (temperature)
            temperatureItem.percentage = if (temperature == 0) 0.0 else (REQ_TEMP.value - temp).toDouble() / REQ_TEMP.value
        }
    }
    
    private fun generateStructure(){
        when (facing) {
            BlockFace.NORTH -> struct = hashMapOf(
                //I I 00
                blockState.pos.add(1,0,0) to Material.IRON_BLOCK,
                blockState.pos.add(-1,0,0) to Material.IRON_BLOCK,
                //III 01
                blockState.pos.add(1,0,1) to Material.IRON_BLOCK,
                blockState.pos.add(-1,0,1) to Material.IRON_BLOCK,
                blockState.pos.add(0,0,1) to Material.IRON_BLOCK,
                //III 02
                blockState.pos.add(1,0,2) to Material.IRON_BLOCK,
                blockState.pos.add(-1,0,2) to Material.IRON_BLOCK,
                blockState.pos.add(0,0,2) to Material.IRON_BLOCK,
                //BIB 10
                blockState.pos.add(1,1,0) to Material.IRON_BARS,
                blockState.pos.add(-1,1,0) to Material.IRON_BARS,
                blockState.pos.add(0,1,0) to Material.IRON_BLOCK,
                //BLB 11
                blockState.pos.add(1,1,1) to Material.IRON_BLOCK,
                blockState.pos.add(-1,1,1) to Material.IRON_BLOCK,
                blockState.pos.add(0,1,1) to Material.LAVA,
                //BIB 12
                blockState.pos.add(1,1,2) to Material.IRON_BARS,
                blockState.pos.add(-1,1,2) to Material.IRON_BARS,
                blockState.pos.add(0,1,2) to Material.IRON_BLOCK,
                //BBB 20
                blockState.pos.add(1,2,0) to Material.IRON_BARS,
                blockState.pos.add(-1,2,0) to Material.IRON_BARS,
                blockState.pos.add(0,2,0) to Material.IRON_BARS,
                //B B 21
                blockState.pos.add(1,2,1) to Material.IRON_BARS,
                blockState.pos.add(-1,2,1) to Material.IRON_BARS,
                //BBB 22
                blockState.pos.add(1,2,2) to Material.IRON_BARS,
                blockState.pos.add(-1,2,2) to Material.IRON_BARS,
                blockState.pos.add(0,2,2) to Material.IRON_BARS,
            )
            BlockFace.SOUTH -> struct = hashMapOf(
                //I I 00
                blockState.pos.add(1,0,0) to Material.IRON_BLOCK,
                blockState.pos.add(-1,0,0) to Material.IRON_BLOCK,
                //III 01
                blockState.pos.add(1,0,-1) to Material.IRON_BLOCK,
                blockState.pos.add(-1,0,-1) to Material.IRON_BLOCK,
                blockState.pos.add(0,0,-1) to Material.IRON_BLOCK,
                //III 02
                blockState.pos.add(1,0,-2) to Material.IRON_BLOCK,
                blockState.pos.add(-1,0,-2) to Material.IRON_BLOCK,
                blockState.pos.add(0,0,-2) to Material.IRON_BLOCK,
                //BIB 10
                blockState.pos.add(1,1,0) to Material.IRON_BARS,
                blockState.pos.add(-1,1,0) to Material.IRON_BARS,
                blockState.pos.add(0,1,0) to Material.IRON_BLOCK,
                //BLB 11
                blockState.pos.add(1,1,-1) to Material.IRON_BLOCK,
                blockState.pos.add(-1,1,-1) to Material.IRON_BLOCK,
                blockState.pos.add(0,1,-1) to Material.LAVA,
                //BIB 12
                blockState.pos.add(1,1,-2) to Material.IRON_BARS,
                blockState.pos.add(-1,1,-2) to Material.IRON_BARS,
                blockState.pos.add(0,1,-2) to Material.IRON_BLOCK,
                //BBB 20
                blockState.pos.add(1,2,0) to Material.IRON_BARS,
                blockState.pos.add(-1,2,0) to Material.IRON_BARS,
                blockState.pos.add(0,2,0) to Material.IRON_BARS,
                //B B 21
                blockState.pos.add(1,2,-1) to Material.IRON_BARS,
                blockState.pos.add(-1,2,-1) to Material.IRON_BARS,
                //BBB 22
                blockState.pos.add(1,2,-2) to Material.IRON_BARS,
                blockState.pos.add(-1,2,-2) to Material.IRON_BARS,
                blockState.pos.add(0,2,-2) to Material.IRON_BARS,
            )
            BlockFace.EAST -> struct = hashMapOf(
                //I I 00
                blockState.pos.add(0,0,1) to Material.IRON_BLOCK,
                blockState.pos.add(0,0,-1) to Material.IRON_BLOCK,
                //III 01
                blockState.pos.add(-1,0,1) to Material.IRON_BLOCK,
                blockState.pos.add(-1,0,-1) to Material.IRON_BLOCK,
                blockState.pos.add(-1,0,0) to Material.IRON_BLOCK,
                //III 02
                blockState.pos.add(-2,0,1) to Material.IRON_BLOCK,
                blockState.pos.add(-2,0,-1) to Material.IRON_BLOCK,
                blockState.pos.add(-2,0,0) to Material.IRON_BLOCK,
                //BIB 10
                blockState.pos.add(0,1,1) to Material.IRON_BARS,
                blockState.pos.add(0,1,-1) to Material.IRON_BARS,
                blockState.pos.add(0,1,0) to Material.IRON_BLOCK,
                //BLB 11
                blockState.pos.add(-1,1,1) to Material.IRON_BLOCK,
                blockState.pos.add(-1,1,-1) to Material.IRON_BLOCK,
                blockState.pos.add(-1,1,0) to Material.LAVA,
                //BIB 12
                blockState.pos.add(-2,1,1) to Material.IRON_BARS,
                blockState.pos.add(-2,1,-1) to Material.IRON_BARS,
                blockState.pos.add(-2,1,0) to Material.IRON_BLOCK,
                //BBB 20
                blockState.pos.add(0,2,1) to Material.IRON_BARS,
                blockState.pos.add(0,2,-1) to Material.IRON_BARS,
                blockState.pos.add(0,2,0) to Material.IRON_BARS,
                //B B 21
                blockState.pos.add(-1,2,-1) to Material.IRON_BARS,
                blockState.pos.add(-1,2,1) to Material.IRON_BARS,
                //BBB 22
                blockState.pos.add(-2,2,-1) to Material.IRON_BARS,
                blockState.pos.add(-2,2,1) to Material.IRON_BARS,
                blockState.pos.add(-2,2,0) to Material.IRON_BARS,
            )
            BlockFace.WEST -> struct = hashMapOf(
                //I I 00
                blockState.pos.add(0,0,1) to Material.IRON_BLOCK,
                blockState.pos.add(0,0,-1) to Material.IRON_BLOCK,
                //III 01
                blockState.pos.add(1,0,1) to Material.IRON_BLOCK,
                blockState.pos.add(1,0,-1) to Material.IRON_BLOCK,
                blockState.pos.add(1,0,0) to Material.IRON_BLOCK,
                //III 02
                blockState.pos.add(2,0,1) to Material.IRON_BLOCK,
                blockState.pos.add(2,0,-1) to Material.IRON_BLOCK,
                blockState.pos.add(2,0,0) to Material.IRON_BLOCK,
                //BIB 10
                blockState.pos.add(0,1,1) to Material.IRON_BARS,
                blockState.pos.add(0,1,-1) to Material.IRON_BARS,
                blockState.pos.add(0,1,0) to Material.IRON_BLOCK,
                //BLB 11
                blockState.pos.add(1,1,1) to Material.IRON_BLOCK,
                blockState.pos.add(1,1,-1) to Material.IRON_BLOCK,
                blockState.pos.add(1,1,0) to Material.LAVA,
                //BIB 12
                blockState.pos.add(2,1,1) to Material.IRON_BARS,
                blockState.pos.add(2,1,-1) to Material.IRON_BARS,
                blockState.pos.add(2,1,0) to Material.IRON_BLOCK,
                //BBB 20
                blockState.pos.add(0,2,1) to Material.IRON_BARS,
                blockState.pos.add(0,2,-1) to Material.IRON_BARS,
                blockState.pos.add(0,2,0) to Material.IRON_BARS,
                //B B 21
                blockState.pos.add(1,2,-1) to Material.IRON_BARS,
                blockState.pos.add(1,2,1) to Material.IRON_BARS,
                //BBB 22
                blockState.pos.add(2,2,-1) to Material.IRON_BARS,
                blockState.pos.add(2,2,1) to Material.IRON_BARS,
                blockState.pos.add(2,2,0) to Material.IRON_BARS,
            )
            else -> return
        }
    }
}