package com.murthinext.ae2pr.block.multi_factory;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraftforge.common.extensions.IForgeMenuType;

import com.murthinext.ae2pr.multiblock.AbstractMultiblockControllerMenu;

public class MultiFactoryMenu extends AbstractMultiblockControllerMenu<MultiFactoryControllerBlockEntity> {

    public static final MenuType<MultiFactoryMenu> TYPE = IForgeMenuType.create(MultiFactoryMenu::new);
    public static final int SWITCH_MODE = 0;

    private static final int DATA_MODE = DATA_COUNT;
    private static final int EXTRA_DATA = 1;

    public MultiFactoryMenu(int id, Inventory inventory, MultiFactoryControllerBlockEntity controller) {
        super(TYPE, id, inventory, controller, EXTRA_DATA);
    }

    public MultiFactoryMenu(int id, Inventory inventory, FriendlyByteBuf buffer) {
        this(id, inventory, readController(inventory.player, buffer, MultiFactoryControllerBlockEntity.class));
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

    public MultiFactoryMode getMode() {
        return data().get(DATA_MODE) == MultiFactoryMode.CHARGING.ordinal()
                ? MultiFactoryMode.CHARGING : MultiFactoryMode.INSCRIBING;
    }
}
