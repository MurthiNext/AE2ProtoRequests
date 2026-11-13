package com.murthinext.ae2pr.block.level_emitter;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;

import appeng.api.stacks.GenericStack;
import appeng.menu.implementations.MenuTypeBuilder;

public class MultiThresholdLevelEmitterMenu extends AbstractMultiLevelEmitterMenu<MultiThresholdLevelEmitterPart> {

    private static final String ACTION_SET_UPPER_VALUE = "setUpperValue";
    private static final String ACTION_SET_LOWER_VALUE = "setLowerValue";

    public static final MenuType<MultiThresholdLevelEmitterMenu>
            TYPE = MenuTypeBuilder.create(MultiThresholdLevelEmitterMenu::new, MultiThresholdLevelEmitterPart.class)
                           .withMenuTitle(host -> Component.translatable("gui.ae2pr.multi_threshold_level_emitter"))
                           .withInitialData(
                                   (host, buffer)
                                           -> {
                                       GenericStack.writeBuffer(host.getConfig().getStack(0), buffer);
                                       buffer.writeVarLong(host.getReportingValue());
                                       buffer.writeVarLong(host.getLowerThreshold());
                                   },
                                   (host, menu, buffer)
                                           -> menu.loadValues(GenericStack.readBuffer(buffer), buffer.readVarLong(),
                                                              buffer.readVarLong()))
                           .build("multi_threshold_level_emitter");

    private long currentUpper;
    private long currentLower;

    public MultiThresholdLevelEmitterMenu(MenuType<MultiThresholdLevelEmitterMenu> menuType, int id, Inventory ip,
                                          MultiThresholdLevelEmitterPart host) {
        super(menuType, id, ip, host);
        registerClientAction(ACTION_SET_UPPER_VALUE, Long.class, this::setUpperValue);
        registerClientAction(ACTION_SET_LOWER_VALUE, Long.class, this::setLowerValue);
    }

    public long getCurrentUpper() {
        return currentUpper;
    }

    public long getCurrentLower() {
        return currentLower;
    }

    public void setUpperValue(long value) {
        if (isClientSide()) {
            if (value != this.currentUpper) {
                this.currentUpper = value;
                sendClientAction(ACTION_SET_UPPER_VALUE, value);
            }
        } else {
            getHost().setReportingValue(value);
        }
    }

    public void setLowerValue(long value) {
        if (isClientSide()) {
            if (value != this.currentLower) {
                this.currentLower = value;
                sendClientAction(ACTION_SET_LOWER_VALUE, value);
            }
        } else {
            getHost().setLowerThreshold(value);
        }
    }

    @Override
    protected void loadValues(GenericStack filter, long... values) {
        super.loadValues(filter);
        this.currentUpper = values[0];
        this.currentLower = values[1];
    }
}
