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

/**
 * 独立源码集（src/requester）内红石请求器的注册入口。
 * <p>
 * 复用主源码集的 DeferredRegister；FMLConstructModEvent 触发类加载后，
 * 静态字段即把条目挂入注册器，保证发生在 RegisterEvent 之前。
 * <p>
 * 本类为原创注册胶水代码（MIT），与同源码集内的 LGPL-3.0 派生代码分开声明，
 * 见 licenses/ME-Requester-NOTICE.txt。
 */
@Mod.EventBusSubscriber(modid = ae2pr.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class RequesterRegistration {

    /** ME 红石请求器方块 */
    public static final RegistryObject<RedstoneRequesterBlock> REDSTONE_REQUESTER_BLOCK = ModBlocks.BLOCKS.register(
            "redstone_requester", RedstoneRequesterBlock::new);

    /** ME 红石请求器物品 */
    public static final RegistryObject<BlockItem> REDSTONE_REQUESTER_ITEM = ModItems.ITEMS.register(
            "redstone_requester", () -> new BlockItem(REDSTONE_REQUESTER_BLOCK.get(), new Item.Properties()));

    /** ME 红石请求器方块实体 */
    public static final RegistryObject<BlockEntityType<RedstoneRequesterBlockEntity>> REDSTONE_REQUESTER_BLOCK_ENTITY = ModBlockEntities.BLOCK_ENTITIES
            .register("redstone_requester", () -> BlockEntityType.Builder.of(
                    (pos, state) -> new RedstoneRequesterBlockEntity(
                            RequesterRegistration.REDSTONE_REQUESTER_BLOCK_ENTITY.get(), pos, state),
                    REDSTONE_REQUESTER_BLOCK.get()).build(null));

    /** ME 红石请求器菜单 */
    public static final RegistryObject<MenuType<RedstoneRequesterMenu>> REDSTONE_REQUESTER_MENU = ModMenus.MENUS
            .register("redstone_requester", () -> RedstoneRequesterMenu.TYPE);

    private RequesterRegistration() {
    }

    /** 触发本类静态字段注册；方法体无需逻辑。 */
    @SubscribeEvent
    public static void onConstruct(FMLConstructModEvent event) {
    }
}
