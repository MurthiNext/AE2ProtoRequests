package com.murthinext.ae2pr.requester.setup;

import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import net.minecraftforge.registries.RegistryObject;

import com.murthinext.ae2pr.ModBlockEntities;
import com.murthinext.ae2pr.ModBlocks;
import com.murthinext.ae2pr.ModItems;
import com.murthinext.ae2pr.ModMenus;
import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.requester.RedstoneRequesterBlock;
import com.murthinext.ae2pr.requester.RedstoneRequesterBlockEntity;
import com.murthinext.ae2pr.requester.RedstoneRequesterMenu;

@Mod.EventBusSubscriber(modid = ae2pr.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class RequesterRegistration {

    public static final RegistryObject<RedstoneRequesterBlock> REDSTONE_REQUESTER_BLOCK = ModBlocks.BLOCKS.register(
            "redstone_requester", RedstoneRequesterBlock::new);

    public static final RegistryObject<BlockItem> REDSTONE_REQUESTER_ITEM = ModItems.ITEMS.register(
            "redstone_requester", () -> new BlockItem(REDSTONE_REQUESTER_BLOCK.get(), new Item.Properties()));

    public static final RegistryObject<BlockEntityType<RedstoneRequesterBlockEntity>> REDSTONE_REQUESTER_BLOCK_ENTITY = ModBlockEntities.BLOCK_ENTITIES
            .register("redstone_requester", () -> BlockEntityType.Builder.of(
                    (pos, state) -> new RedstoneRequesterBlockEntity(
                            RequesterRegistration.REDSTONE_REQUESTER_BLOCK_ENTITY.get(), pos, state),
                    REDSTONE_REQUESTER_BLOCK.get()).build(null));

    public static final RegistryObject<MenuType<RedstoneRequesterMenu>> REDSTONE_REQUESTER_MENU = ModMenus.MENUS
            .register("redstone_requester", () -> RedstoneRequesterMenu.TYPE);

    private RequesterRegistration() {
    }

    @SubscribeEvent
    public static void onConstruct(FMLConstructModEvent event) {
    }
}
