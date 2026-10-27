package com.murthinext.ae2pr.requester.setup;

import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

import appeng.blockentity.AEBaseBlockEntity;

import com.murthinext.ae2pr.ModCreativeTabs;
import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.requester.RedstoneRequesterBlockEntity;
import com.murthinext.ae2pr.requester.network.RequesterNetwork;

@Mod.EventBusSubscriber(modid = ae2pr.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class RequesterCommonSetup {

    private RequesterCommonSetup() {
    }

    @SubscribeEvent
    public static void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            RequesterNetwork.init();
            var blockEntityType = RequesterRegistration.REDSTONE_REQUESTER_BLOCK_ENTITY.get();
            RequesterRegistration.REDSTONE_REQUESTER_BLOCK.get()
                    .setBlockEntity(RedstoneRequesterBlockEntity.class, blockEntityType, null, null);
            AEBaseBlockEntity.registerBlockEntityItem(blockEntityType,
                    RequesterRegistration.REDSTONE_REQUESTER_ITEM.get());
        });
    }

    @SubscribeEvent
    public static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (ModCreativeTabs.MAIN.getId().equals(event.getTabKey().location())) {
            event.accept(RequesterRegistration.REDSTONE_REQUESTER_ITEM.get());
        }
    }
}
