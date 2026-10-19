package com.murthinext.ae2pr.block.matter_phase_transformer;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;

import com.murthinext.ae2pr.multiblock.AbstractMultiblockControllerMenu;

/**
 * 物质相变仪主机的容器菜单。
 */
public class MatterPhaseTransformerMenu
        extends AbstractMultiblockControllerMenu<MatterPhaseTransformerControllerBlockEntity> {

    public static final MenuType<MatterPhaseTransformerMenu> TYPE = IForgeMenuType
            .create(MatterPhaseTransformerMenu::new);

    public MatterPhaseTransformerMenu(int id, Inventory playerInventory,
            MatterPhaseTransformerControllerBlockEntity controller) {
        super(TYPE, id, playerInventory, controller, 0);
    }

    public MatterPhaseTransformerMenu(int id, Inventory playerInventory, FriendlyByteBuf buffer) {
        this(id, playerInventory, readController(playerInventory.player, buffer,
                MatterPhaseTransformerControllerBlockEntity.class));
    }
}
