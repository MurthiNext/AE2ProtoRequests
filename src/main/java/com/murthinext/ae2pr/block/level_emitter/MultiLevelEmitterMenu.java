package com.murthinext.ae2pr.block.level_emitter;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;

import appeng.api.stacks.GenericStack;
import appeng.menu.implementations.MenuTypeBuilder;

public class MultiLevelEmitterMenu extends AbstractMultiLevelEmitterMenu<MultiLevelEmitterPart> {

    private static final String ACTION_SET_REPORTING_VALUE = "setReportingValue";

    public static final MenuType<MultiLevelEmitterMenu> TYPE = MenuTypeBuilder
            .create(MultiLevelEmitterMenu::new, MultiLevelEmitterPart.class)
            .withMenuTitle(host -> Component.translatable("gui.ae2pr.multi_level_emitter"))
            .withInitialData((host, buffer) -> {
                GenericStack.writeBuffer(host.getConfig().getStack(0), buffer);
                buffer.writeVarLong(host.getReportingValue());
            }, (host, menu, buffer) -> {
                menu.loadValues(GenericStack.readBuffer(buffer), buffer.readVarLong());
            })
            .build("multi_level_emitter");

    private long currentValue;

    public MultiLevelEmitterMenu(MenuType<MultiLevelEmitterMenu> menuType, int id, Inventory ip,
            MultiLevelEmitterPart host) {
        super(menuType, id, ip, host);
        registerClientAction(ACTION_SET_REPORTING_VALUE, Long.class, this::setValue);
    }

    public long getCurrentValue() {
        return currentValue;
    }

    public void setValue(long initialValue) {
        if (isClientSide()) {
            if (initialValue != this.currentValue) {
                this.currentValue = initialValue;
                sendClientAction(ACTION_SET_REPORTING_VALUE, initialValue);
            }
        } else {
            getHost().setReportingValue(initialValue);
        }
    }

    @Override
    protected void loadValues(GenericStack filter, long... values) {
        super.loadValues(filter);
        this.currentValue = values[0];
    }
}
