package com.murthinext.ae2pr.block.crystal_reaction_chamber;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;

import com.murthinext.ae2pr.multiblock.AbstractMultiblockControllerMenu;

/**
 * 水晶反应仓主机的容器菜单。
 */
public class CrystalReactionChamberMenu
        extends AbstractMultiblockControllerMenu<CrystalReactionChamberControllerBlockEntity> {

    public static final MenuType<CrystalReactionChamberMenu> TYPE = IForgeMenuType
            .create(CrystalReactionChamberMenu::new);

    public CrystalReactionChamberMenu(int id, Inventory playerInventory,
            CrystalReactionChamberControllerBlockEntity controller) {
        super(TYPE, id, playerInventory, controller, 0);
    }

    public CrystalReactionChamberMenu(int id, Inventory playerInventory, FriendlyByteBuf buffer) {
        this(id, playerInventory, readController(playerInventory.player, buffer,
                CrystalReactionChamberControllerBlockEntity.class));
    }
}
