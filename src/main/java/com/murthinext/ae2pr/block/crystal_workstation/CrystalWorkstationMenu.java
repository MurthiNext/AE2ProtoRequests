package com.murthinext.ae2pr.block.crystal_workstation;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraftforge.common.extensions.IForgeMenuType;

import com.murthinext.ae2pr.multiblock.AbstractMultiblockControllerMenu;

/**
 * 晶能工作站主机的容器菜单。
 */
public class CrystalWorkstationMenu extends AbstractMultiblockControllerMenu<CrystalWorkstationControllerBlockEntity> {

    public static final MenuType<CrystalWorkstationMenu> TYPE = IForgeMenuType.create(CrystalWorkstationMenu::new);
    public static final int SWITCH_MODE = 0;

    private static final int DATA_MODE = DATA_COUNT;
    private static final int EXTRA_DATA = 1;

    public CrystalWorkstationMenu(int id, Inventory inventory, CrystalWorkstationControllerBlockEntity controller) {
        super(TYPE, id, inventory, controller, EXTRA_DATA);
    }

    public CrystalWorkstationMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, readController(inventory.player, buffer,
                CrystalWorkstationControllerBlockEntity.class));
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (clientSide || id != SWITCH_MODE || controller == null || !stillValid(player)
                || !controller.switchMode()) {
            return false;
        }
        broadcastChanges();
        return true;
    }

    @Override
    protected void writeExtraData(SimpleContainerData data) {
        data.set(DATA_MODE, controller.getMode().ordinal());
    }

    public CrystalWorkstationMode getMode() {
        return data().get(DATA_MODE) == CrystalWorkstationMode.CUTTING.ordinal()
                ? CrystalWorkstationMode.CUTTING : CrystalWorkstationMode.ETCHING;
    }
}
