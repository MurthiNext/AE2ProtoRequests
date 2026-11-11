package com.murthinext.ae2pr.requester.setup;

import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.registries.RegistryObject;

import com.murthinext.ae2pr.registry.ModBlockEntities;
import com.murthinext.ae2pr.registry.ModBlocks;
import com.murthinext.ae2pr.registry.ModItems;
import com.murthinext.ae2pr.registry.ModMenus;
import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.requester.RedstoneRequesterBlock;
import com.murthinext.ae2pr.requester.RedstoneRequesterBlockEntity;
import com.murthinext.ae2pr.requester.RedstoneRequesterMenu;
import com.murthinext.ae2pr.requester.RequesterConstants;

@Mod.EventBusSubscriber(modid = ae2pr.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class RequesterRegistration {

    public static final RegistryObject<RedstoneRequesterBlock> REDSTONE_REQUESTER_BLOCK = ModBlocks.BLOCKS.register(
            RequesterConstants.REQUESTER_ID, RedstoneRequesterBlock::new);

    public static final RegistryObject<BlockItem> REDSTONE_REQUESTER_ITEM = ModItems.ITEMS.register(
            RequesterConstants.REQUESTER_ID, () -> new BlockItem(REDSTONE_REQUESTER_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<BlockEntityType<RedstoneRequesterBlockEntity>> REDSTONE_REQUESTER_BLOCK_ENTITY = ModBlockEntities.BLOCK_ENTITIES
            .register(RequesterConstants.REQUESTER_ID, () -> BlockEntityType.Builder.of(
                    (pos, state) -> new RedstoneRequesterBlockEntity(
                            RequesterRegistration.REDSTONE_REQUESTER_BLOCK_ENTITY.get(), pos, state),
                    REDSTONE_REQUESTER_BLOCK.get()).build(null));

    public static final RegistryObject<MenuType<RedstoneRequesterMenu>> REDSTONE_REQUESTER_MENU = ModMenus.MENUS
            .register(RequesterConstants.REQUESTER_ID, () -> RedstoneRequesterMenu.TYPE);

    private RequesterRegistration() {
    }

    @SubscribeEvent
    public static void onConstruct(FMLConstructModEvent event) {
    }
}
