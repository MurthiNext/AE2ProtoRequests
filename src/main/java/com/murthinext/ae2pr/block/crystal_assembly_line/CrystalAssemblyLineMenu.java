package com.murthinext.ae2pr.block.crystal_assembly_line;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;

import com.murthinext.ae2pr.multiblock.AbstractMultiblockControllerMenu;

public class CrystalAssemblyLineMenu extends AbstractMultiblockControllerMenu<CrystalAssemblyLineControllerBlockEntity> {

    public static final MenuType<CrystalAssemblyLineMenu> TYPE = IForgeMenuType.create(CrystalAssemblyLineMenu::new);

    public CrystalAssemblyLineMenu(int id, Inventory playerInventory, CrystalAssemblyLineControllerBlockEntity controller) {
        super(TYPE, id, playerInventory, controller, 0);
    }

    public CrystalAssemblyLineMenu(int id, Inventory playerInventory, FriendlyByteBuf buffer) {
        this(id, playerInventory, readController(playerInventory.player, buffer,
                CrystalAssemblyLineControllerBlockEntity.class));
    }
}
