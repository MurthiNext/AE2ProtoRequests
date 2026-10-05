package com.murthinext.ae2pr.client.naming_factory;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import com.murthinext.ae2pr.ModNetwork;
import com.murthinext.ae2pr.network.NamingFactoryRenamePacket;

/**
 * 名称压印工厂重命名界面：手持石英切割刀右击机器时打开。
 * <p>
 * 回车确认提交（留空则清除名称），ESC 取消。
 */
public class NamingFactoryRenameScreen extends Screen {

    private static final int PANEL_WIDTH = 220;
    private static final int PANEL_HEIGHT = 96;
    private static final int EDIT_WIDTH = 180;
    private static final int EDIT_HEIGHT = 20;

    private final BlockPos pos;
    private final String initialName;

    private EditBox nameBox;

    private NamingFactoryRenameScreen(BlockPos pos, String initialName) {
        super(Component.translatable("screen.ae2pr.naming_factory.rename.title"));
        this.pos = pos;
        this.initialName = initialName;
    }

    /** 客户端：打开重命名界面。 */
    public static void open(BlockPos pos, String initialName) {
        Minecraft.getInstance().setScreen(new NamingFactoryRenameScreen(pos, initialName));
    }

    @Override
    protected void init() {
        int left = (width - PANEL_WIDTH) / 2;
        int top = (height - PANEL_HEIGHT) / 2;
        nameBox = new EditBox(font, left + (PANEL_WIDTH - EDIT_WIDTH) / 2, top + 34, EDIT_WIDTH, EDIT_HEIGHT,
                Component.translatable("screen.ae2pr.naming_factory.rename.title"));
        nameBox.setMaxLength(64);
        nameBox.setValue(initialName);
        nameBox.setHint(Component.translatable("screen.ae2pr.naming_factory.rename.hint"));
        addRenderableWidget(nameBox);
        setFocused(nameBox);
        nameBox.setFocused(true);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);

        int left = (width - PANEL_WIDTH) / 2;
        int top = (height - PANEL_HEIGHT) / 2;
        graphics.fill(left - 1, top - 1, left + PANEL_WIDTH + 1, top + PANEL_HEIGHT + 1, 0xFF101418);
        graphics.fill(left, top, left + PANEL_WIDTH, top + PANEL_HEIGHT, 0xFF2B2F3A);

        graphics.drawCenteredString(font, title, width / 2, top + 12, 0xFF55FFFF);
        graphics.drawCenteredString(font, Component.translatable("screen.ae2pr.naming_factory.rename.tip"),
                width / 2, top + 66, 0xFFAAB8C6);

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
            save();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void save() {
        ModNetwork.sendToServer(new NamingFactoryRenamePacket(pos, nameBox.getValue().trim()));
        onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
