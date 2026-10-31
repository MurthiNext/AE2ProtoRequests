package com.murthinext.ae2pr.block.filter_press;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;

import com.murthinext.ae2pr.multiblock.AbstractMultiblockControllerMenu;

public class FilterPressMenu extends AbstractMultiblockControllerMenu<FilterPressControllerBlockEntity> {

    public static final MenuType<FilterPressMenu> TYPE = IForgeMenuType.create(FilterPressMenu::new);

    public FilterPressMenu(int id, Inventory inventory, FilterPressControllerBlockEntity controller) {
        super(TYPE, id, inventory, controller, 0);
    }

    public FilterPressMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, readController(inventory.player, buffer, FilterPressControllerBlockEntity.class));
    }
}
