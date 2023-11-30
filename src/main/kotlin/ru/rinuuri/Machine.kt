package ru.rinuuri

import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import xyz.xenondevs.nova.data.world.block.state.NovaBlockState
import xyz.xenondevs.nova.world.block.BlockBehavior
import xyz.xenondevs.nova.world.block.context.BlockBreakContext

object Machine : BlockBehavior.Default<NovaBlockState>() {
    override fun getDrops(state: NovaBlockState, ctx: BlockBreakContext): List<ItemStack> = listOf(ItemStack(Material.IRON_BLOCK))
}
