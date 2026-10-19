package com.murthinext.ae2pr.block.assembly_line;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;

import com.murthinext.ae2pr.multiblock.AbstractMultiblockControllerMenu;

/**
 * 水晶装配线主机的容器菜单。
 */
public class AssemblyLineMenu extends AbstractMultiblockControllerMenu<AssemblyLineControllerBlockEntity> {

    public static final MenuType<AssemblyLineMenu> TYPE = IForgeMenuType.create(AssemblyLineMenu::new);

    public AssemblyLineMenu(int id, Inventory playerInventory, AssemblyLineControllerBlockEntity controller) {
        super(TYPE, id, playerInventory, controller, 0);
    }

    public AssemblyLineMenu(int id, Inventory playerInventory, FriendlyByteBuf buffer) {
        this(id, playerInventory, readController(playerInventory.player, buffer,
                AssemblyLineControllerBlockEntity.class));
    }
}
