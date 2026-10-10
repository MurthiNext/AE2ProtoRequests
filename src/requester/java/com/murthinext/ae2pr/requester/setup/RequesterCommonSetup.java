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

/**
 * 独立源码集内红石请求器的通用初始化：网络通道、AE2 方块实体绑定与创造模式标签页。
 */
@Mod.EventBusSubscriber(modid = ae2pr.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class RequesterCommonSetup {

    private RequesterCommonSetup() {
    }

    /** 注册网络通道并绑定 AE2 方块实体类型。 */
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

    /** 把请求器条目加入本模组主创造标签页。 */
    @SubscribeEvent
    public static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (ModCreativeTabs.MAIN.getId().equals(event.getTabKey().location())) {
            event.accept(RequesterRegistration.REDSTONE_REQUESTER_ITEM.get());
        }
    }
}
