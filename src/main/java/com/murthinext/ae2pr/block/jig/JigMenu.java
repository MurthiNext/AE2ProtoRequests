package com.murthinext.ae2pr.block.jig;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;

import com.murthinext.ae2pr.multiblock.AbstractMultiblockControllerMenu;

public class JigMenu extends AbstractMultiblockControllerMenu<JigControllerBlockEntity> {

    public static final MenuType<JigMenu> TYPE = IForgeMenuType.create(JigMenu::new);

    public JigMenu(int id, Inventory inventory, JigControllerBlockEntity controller) {
        super(TYPE, id, inventory, controller, 0);
    }

    public JigMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, readController(inventory.player, buffer, JigControllerBlockEntity.class));
    }
}
