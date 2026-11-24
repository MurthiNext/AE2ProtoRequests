package com.murthinext.ae2pr.block.machine_part;

import static com.murthinext.ae2pr.block.machine_part.MEPatternBufferBlockEntity.SHARED_SIZE;
import static com.murthinext.ae2pr.block.machine_part.MEPatternBufferBlockEntity.SHARED_TANK_CAPACITY;
import static com.murthinext.ae2pr.block.machine_part.MEPatternBufferBlockEntity.SIZE;
import static com.murthinext.ae2pr.block.machine_part.MEPatternBufferMenu.FLUID_CLICK_START;
import static com.murthinext.ae2pr.block.machine_part.MEPatternBufferMenu.FLUID_PANEL;
import static com.murthinext.ae2pr.block.machine_part.MEPatternBufferMenu.ITEM_PANEL;
import static com.murthinext.ae2pr.block.machine_part.MEPatternBufferMenu.PANEL_CLOSED;
import static com.murthinext.ae2pr.block.machine_part.MEPatternBufferMenu.SHARED_PANEL_HEIGHT;
import static com.murthinext.ae2pr.block.machine_part.MEPatternBufferMenu.SHARED_PANEL_WIDTH;
import static com.murthinext.ae2pr.block.machine_part.MEPatternBufferMenu.SHARED_PANEL_X;
import static com.murthinext.ae2pr.block.machine_part.MEPatternBufferMenu.SHARED_PANEL_Y;

import com.murthinext.ae2pr.api.machine.client.AbstractMachineScreen;
import com.murthinext.ae2pr.api.machine.client.GuiTextures;
import com.murthinext.ae2pr.registry.ModGuide;
import com.murthinext.nextui.client.GuiExtraAreas;
import com.murthinext.nextui.client.GuiFluids;
import com.murthinext.nextui.client.GuiPanels;
import com.murthinext.nextui.client.GuiSidebar;
import com.murthinext.nextui.client.GuiSlots;
import com.murthinext.nextui.client.GuiTheme;
import com.murthinext.nextui.client.GuiTooltips;
import com.murthinext.nextui.client.IconButton;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

public final class MEPatternBufferScreen extends AbstractMachineScreen<MEPatternBufferMenu> {

    private static final String LANG = "gui.ae2pr.me_pattern_buffer.";
    private static final GuiTheme THEME = new GuiTheme(0xFFE0D0FF, 0xFFD1A6FF, 0x80634C8B);
    private static final int ONLINE = 0xFFCBABFF;
    private static final int OFFLINE = 0xFFAAA5B2;
    private static final int BUFFERED = 0xFFFFC46F;
    private static final int SHARED_BUTTONS = 2;
    private static final int SLOT_INNER_SIZE = GuiSlots.SIZE - 2;
    private static final ResourceLocation SHARED_ITEMS_ICON = texture("shared_items");
    private static final ResourceLocation SHARED_FLUIDS_ICON = texture("shared_fluids");
    private static final NumberFormat NUMBER = NumberFormat.getIntegerInstance();

    @Nullable
    private IconButton itemsButton;

    @Nullable
    private IconButton fluidsButton;

    public MEPatternBufferScreen(MEPatternBufferMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    public static void applyFluidSync(int containerId, int slot, FluidStack fluid) {
        var player = Minecraft.getInstance().player;
        if (player != null
                && player.containerMenu instanceof MEPatternBufferMenu menu
                && menu.containerId == containerId) {
            menu.applyFluidSync(slot, fluid);
        }
    }

    @Override
    protected void init() {
        super.init();
        var sidebar = sharedToolbar();
        itemsButton = addSharedButton(sidebar, 0, ITEM_PANEL, "shared_items", SHARED_ITEMS_ICON);
        fluidsButton = addSharedButton(sidebar, 1, FLUID_PANEL, "shared_fluids", SHARED_FLUIDS_ICON);
    }

    private IconButton addSharedButton(GuiSidebar sidebar, int row, int panel, String label, ResourceLocation icon) {
        return addRenderableWidget(new IconButton(
                sidebar.cellX(),
                sidebar.cellY(row),
                Component.translatable(LANG + label),
                GuiTextures.BUTTON_SKIN,
                () -> icon,
                () -> menu.activePanel() == panel,
                () -> sendButton(menu.activePanel() == panel ? PANEL_CLOSED : panel)));
    }

    @Override
    protected ResourceLocation guidePage() {
        return ModGuide.CHAMBERS_PAGE;
    }

    @Override
    protected GuiTheme theme() {
        return THEME;
    }

    @Override
    protected int minimumLeftMargin() {
        return 2 - SHARED_PANEL_X;
    }

    @Override
    public List<Rect2i> getGuiExtraAreas() {
        var areas = new ArrayList<>(super.getGuiExtraAreas());
        areas.add(sharedToolbar().bounds());
        if (menu.activePanel() != PANEL_CLOSED) {
            areas.add(sharedPanel());
        }
        return areas;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        super.renderBg(graphics, partialTick, mouseX, mouseY);
        sharedToolbar().draw(graphics);
        for (int i = 0; i < SIZE; i++) {
            var slot = menu.getSlot(i);
            drawSlot(graphics, leftPos + slot.x, topPos + slot.y);
        }
        if (menu.activePanel() != PANEL_CLOSED) {
            renderSharedPanel(graphics, mouseX, mouseY);
        }
    }

    private void renderSharedPanel(GuiGraphics graphics, int mouseX, int mouseY) {
        var panel = sharedPanel();
        GuiPanels.panel(graphics, panel.getX(), panel.getY(), panel.getWidth(), panel.getHeight(), 0);
        boolean fluidPanel = menu.activePanel() == FLUID_PANEL;
        var title = Component.translatable(LANG + (fluidPanel ? "shared_fluids" : "shared_items"));
        graphics.drawString(
                font,
                title,
                panel.getX() + (panel.getWidth() - font.width(title)) / 2,
                panel.getY() + 6,
                THEME.titleColor(),
                false);
        int hovered = hoveredTank(mouseX, mouseY);
        for (int i = 0; i < SHARED_SIZE; i++) {
            int x = leftPos + MEPatternBufferMenu.sharedX(i);
            int y = topPos + MEPatternBufferMenu.sharedY(i);
            drawSlot(graphics, x, y);
            if (fluidPanel) {
                GuiFluids.drawTankFill(
                        graphics, menu.sharedFluid(i), x, y, SLOT_INNER_SIZE, SLOT_INNER_SIZE, SHARED_TANK_CAPACITY);
                if (hovered == i) {
                    graphics.renderOutline(x - 1, y - 1, GuiSlots.SIZE, GuiSlots.SIZE, THEME.accent());
                }
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        boolean online = menu.isOnline();
        var status = Component.translatable(LANG + (online ? "online" : "offline"));
        graphics.drawString(font, status, imageWidth - 8 - font.width(status), 9, online ? ONLINE : OFFLINE, false);
        for (int i = 0; i < SIZE; i++) {
            if (menu.isLocked(i)) {
                var slot = menu.getSlot(i);
                graphics.fill(slot.x + 13, slot.y - 1, slot.x + 16, slot.y + 2, BUFFERED);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int tank = hoveredTank(mouseX, mouseY);
        if (tank >= 0 && (button == 0 || button == 1)) {
            sendButton(FLUID_CLICK_START + tank * 2 + button);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && menu.activePanel() != PANEL_CLOSED) {
            sendButton(PANEL_CLOSED);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    protected boolean renderToolbarTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        return super.renderToolbarTooltip(graphics, mouseX, mouseY)
                || renderSharedButtonTooltip(itemsButton, graphics, mouseX, mouseY)
                || renderSharedButtonTooltip(fluidsButton, graphics, mouseX, mouseY);
    }

    private boolean renderSharedButtonTooltip(
            @Nullable IconButton button, GuiGraphics graphics, int mouseX, int mouseY) {
        if (button == null || !GuiTooltips.isTooltipTarget(button)) {
            return false;
        }
        GuiTooltips.render(
                graphics,
                font,
                List.of(button.getMessage(), Component.translatable(LANG + "shared_hint")),
                mouseX,
                mouseY);
        return true;
    }

    @Override
    protected boolean renderCustomTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        int tank = hoveredTank(mouseX, mouseY);
        if (tank < 0) {
            return super.renderCustomTooltip(graphics, mouseX, mouseY);
        }
        var fluid = menu.sharedFluid(tank);
        GuiTooltips.render(
                graphics,
                font,
                List.of(
                        fluid.isEmpty()
                                ? Component.translatable("gui.ae2pr.machine_part.tank.empty")
                                : fluid.getDisplayName(),
                        Component.translatable(
                                LANG + "fluid_amount",
                                NUMBER.format(fluid.getAmount()),
                                NUMBER.format(SHARED_TANK_CAPACITY))),
                mouseX,
                mouseY);
        return true;
    }

    private int hoveredTank(double mouseX, double mouseY) {
        if (menu.activePanel() == FLUID_PANEL) {
            for (int i = 0; i < SHARED_SIZE; i++) {
                int x = leftPos + MEPatternBufferMenu.sharedX(i) - 1;
                int y = topPos + MEPatternBufferMenu.sharedY(i) - 1;
                if (GuiExtraAreas.contains(mouseX, mouseY, x, y, GuiSlots.SIZE, GuiSlots.SIZE)) {
                    return i;
                }
            }
        }
        return -1;
    }

    private GuiSidebar sharedToolbar() {
        int height = SHARED_BUTTONS * (IconButton.SIZE + 2) + 2;
        return new GuiSidebar(toolbar().x(), topPos + imageHeight - height, SHARED_BUTTONS);
    }

    private Rect2i sharedPanel() {
        return new Rect2i(leftPos + SHARED_PANEL_X, topPos + SHARED_PANEL_Y, SHARED_PANEL_WIDTH, SHARED_PANEL_HEIGHT);
    }

    private void sendButton(int id) {
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
        }
    }
}
