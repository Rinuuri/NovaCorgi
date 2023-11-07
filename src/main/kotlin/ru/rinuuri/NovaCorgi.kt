package com.example

import xyz.xenondevs.nova.addon.Addon
import xyz.xenondevs.nova.addon.registry.ItemRegistry
import xyz.xenondevs.nova.initialize.Init
import xyz.xenondevs.nova.initialize.InitStage

object ExampleAddon : Addon() {
    
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
object Items : ItemRegistry by ExampleAddon.registry {
    val titanium_ingot = registerItem("titanium_ingot")
    val aluminum_ingot = registerItem("aluminum_ingot")
    val plutonium_ingot = registerItem("plutonium_ingot")
    val steel_ingot = registerItem("steel_ingot")
    val uranium_ingot = registerItem("uranium_ingot")
}
