package com.murthinext.ae2pr.compat.jei;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenPosition;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.block.Block;

import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.inputs.IJeiGuiEventListener;
import mezz.jei.api.gui.widgets.IRecipeWidget;

import com.murthinext.ae2pr.client.gui.MachineGui;
import com.murthinext.ae2pr.client.multiblock.MultiblockPreview;
import com.murthinext.ae2pr.client.multiblock.MultiblockPreviewRenderer;

public final class MultiblockPreviewWidget implements IRecipeWidget, IJeiGuiEventListener {

    private static final ScreenRectangle AREA = new ScreenRectangle(0, 0,
            MultiblockPreviewJeiCategory.WIDTH, MultiblockPreviewJeiCategory.HEIGHT);
    private static final ScreenRectangle VIEW = new ScreenRectangle(4, 22, 124, 148);
    private static final ScreenRectangle REPLACEMENTS = new ScreenRectangle(132, 34, 24, 126);
    private static final ScreenRectangle LAYER = new ScreenRectangle(86, 2, 42, 14);
    private static final ScreenRectangle LENGTH = new ScreenRectangle(36, 2, 46, 14);
    private static final ScreenRectangle RESET = new ScreenRectangle(132, 2, 24, 14);
    private static final ScreenRectangle MATERIAL_PREV = new ScreenRectangle(2, 200, 12, 18);
    private static final ScreenRectangle MATERIAL_NEXT = new ScreenRectangle(144, 200, 12, 18);
    private static final int MATERIAL_X = 16;
    private static final int MATERIAL_Y = 201;
    private static final int MATERIAL_COLUMNS = 7;
    private static final int MATERIAL_PAGE_SIZE = MATERIAL_COLUMNS - 1;
    private static final int REPLACEMENT_ROWS = 7;
    private static final int SLOT = 18;
    private static final int TEXT = 0xFF404850;
    private static final int MUTED = 0xFF687987;
    private static final int ACCENT = MachineGui.ACCENT_CRYSTAL;

    private final MultiblockPreviewRecipe recipe;
    private final MultiblockPreview preview;
    private final MultiblockPreviewRenderer renderer;
    @Nullable
    private MultiblockPreview.Cell selected;
    private int replacementOffset;
    private int materialOffset;
    private boolean rotating;
    private double dragDistance;

    public MultiblockPreviewWidget(MultiblockPreviewRecipe recipe) {
        this.recipe = recipe;
        preview = new MultiblockPreview(recipe.pattern(), Minecraft.getInstance().level.getLightEngine());
        renderer = new MultiblockPreviewRenderer(preview, VIEW.left(), VIEW.top(), VIEW.width(), VIEW.height());
    }

    @Override
    public ScreenPosition getPosition() {
        return AREA.position();
    }

    @Override
    public ScreenRectangle getScreenRectangle() {
        return AREA;
    }

    @Override
    public ScreenRectangle getArea() {
        return AREA;
    }

    @Override
    public void drawWidget(GuiGraphics graphics, double mouseX, double mouseY) {
        var font = Minecraft.getInstance().font;
        drawButton(graphics, RESET, "R", true, mouseX, mouseY);
        drawButton(graphics, LAYER, preview.layer() < 0 ? "L: *" : "L:" + (preview.layer() + 1),
                true, mouseX, mouseY);
        if (hasVariableLength()) {
            drawButton(graphics, LENGTH, "E: " + preview.slices(), true, mouseX, mouseY);
        }

        graphics.fill(VIEW.left() - 1, VIEW.top() - 1, VIEW.right() + 1, VIEW.bottom() + 1, 0xFF8B9AA6);
        graphics.fill(VIEW.left(), VIEW.top(), VIEW.right(), VIEW.bottom(), 0xFFDFE6EB);
        MultiblockPreview.Cell hovered = renderer.pick(mouseX, mouseY);
        renderer.render(graphics, selected == null ? null : selected.pos(), hovered == null ? null : hovered.pos());

        drawCentered(graphics, text("replace"), REPLACEMENTS.left() - 2, VIEW.top(), 30);
        List<Block> replacements = replacements();
        for (int i = 0; i < REPLACEMENT_ROWS && replacementOffset + i < replacements.size(); i++) {
            Block block = replacements.get(replacementOffset + i);
            int x = REPLACEMENTS.left() + 2;
            int y = REPLACEMENTS.top() + i * SLOT;
            boolean current = selected != null && preview.getBlockState(selected.pos()).is(block);
            graphics.fill(x, y, x + SLOT, y + SLOT, current ? ACCENT : 0xFFC2CCD4);
            graphics.renderItem(new ItemStack(block), x + 1, y + 1);
            if (selected != null && !preview.canReplace(selected, block)) {
                graphics.fill(x, y, x + SLOT, y + SLOT, 0x99838A91);
            }
        }
        if (replacements.size() > REPLACEMENT_ROWS) {
            int trackHeight = REPLACEMENTS.height();
            int thumb = Math.max(8, trackHeight * REPLACEMENT_ROWS / replacements.size());
            int top = REPLACEMENTS.top() + (trackHeight - thumb) * replacementOffset
                    / (replacements.size() - REPLACEMENT_ROWS);
            int x = REPLACEMENTS.right();
            graphics.fill(x, REPLACEMENTS.top(), x + 2, REPLACEMENTS.bottom(), 0xFFC2CCD4);
            graphics.fill(x, top, x + 2, top + thumb, MUTED);
        }
        graphics.drawString(font, text("materials"), 4, 189, TEXT, false);
        List<ItemStack> materials = preview.materials();
        materialOffset = Math.min(materialOffset, Math.max(0, materials.size() - MATERIAL_COLUMNS));
        for (int i = 0; i < MATERIAL_COLUMNS; i++) {
            int index = i == 0 ? 0 : materialOffset + i;
            if (index >= materials.size()) {
                break;
            }
            ItemStack stack = materials.get(index);
            int x = MATERIAL_X + i * SLOT;
            MachineGui.drawSlot(graphics, x, MATERIAL_Y);
            ItemStackAmountRenderer.INSTANCE.render(graphics, stack, x, MATERIAL_Y);
        }
        if (materials.size() > MATERIAL_COLUMNS) {
            drawButton(graphics, MATERIAL_PREV, "<", materialOffset > 0, mouseX, mouseY);
            drawButton(graphics, MATERIAL_NEXT, ">", materialOffset + MATERIAL_COLUMNS < materials.size(),
                    mouseX, mouseY);
        }
    }

    @Override
    public void getTooltip(ITooltipBuilder tooltip, double mouseX, double mouseY) {
        if (contains(RESET, mouseX, mouseY)) {
            tooltip.add(text("reset"));
            return;
        }
        if (contains(LAYER, mouseX, mouseY)) {
            tooltip.add(preview.layer() < 0 ? text("all_layers") : text("layer", preview.layer() + 1));
            return;
        }
        if (hasVariableLength() && contains(LENGTH, mouseX, mouseY)) {
            tooltip.add(text("length_hint", recipe.pattern().minSlices(), recipe.maxSlices().getAsInt()));
            return;
        }
        int material = materialIndex(mouseX, mouseY);
        if (material >= 0) {
            ItemStack stack = preview.materials().get(material);
            addItemTooltip(tooltip, stack);
            tooltip.add(text("total", stack.getCount()));
            return;
        }
        int replacement = replacementIndex(mouseX, mouseY);
        if (replacement >= 0 && selected != null) {
            tooltip.add(text("apply"));
            return;
        }
        MultiblockPreview.Cell hovered = renderer.pick(mouseX, mouseY);
        if (hovered != null) {
            addItemTooltip(tooltip, new ItemStack(preview.getBlockState(hovered.pos()).getBlock()));
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        rotating = false;
        dragDistance = 0;
        if (button != 0 && button != 1) {
            return false;
        }
        if (contains(LAYER, mouseX, mouseY)) {
            changeLayer(button == 0 ? 1 : -1);
            return true;
        }
        if (hasVariableLength() && contains(LENGTH, mouseX, mouseY)) {
            int step = Screen.hasShiftDown() ? 5 : 1;
            changeLength(button == 0 ? step : -step);
            return true;
        }
        if (button != 0) {
            return false;
        }
        if (contains(VIEW, mouseX, mouseY)) {
            rotating = true;
            return true;
        }
        if (contains(RESET, mouseX, mouseY)) {
            renderer.reset();
            preview.setLayer(-1);
        } else if (contains(MATERIAL_PREV, mouseX, mouseY)) {
            materialOffset = Math.max(0, materialOffset - MATERIAL_PAGE_SIZE);
        } else if (contains(MATERIAL_NEXT, mouseX, mouseY)) {
            materialOffset = Math.min(Math.max(0, preview.materials().size() - MATERIAL_COLUMNS),
                    materialOffset + MATERIAL_PAGE_SIZE);
        } else {
            int index = replacementIndex(mouseX, mouseY);
            if (index < 0 || selected == null) {
                return false;
            }
            preview.replace(selected, replacements().get(index));
        }
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!rotating || button != 0) {
            return false;
        }
        dragDistance += Math.abs(dragX) + Math.abs(dragY);
        if (dragDistance > 3.0) {
            renderer.rotate(dragX, dragY);
        }
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button != 0 || !rotating) {
            return false;
        }
        rotating = false;
        if (dragDistance <= 3.0) {
            selected = renderer.pick(mouseX, mouseY);
            replacementOffset = 0;
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollDelta) {
        if (contains(VIEW, mouseX, mouseY)) {
            renderer.zoom(scrollDelta);
        } else if (mouseX >= REPLACEMENTS.left() - 2
                && mouseY >= REPLACEMENTS.top() && mouseY < REPLACEMENTS.bottom()) {
            replacementOffset = Mth.clamp(replacementOffset - (int) Math.signum(scrollDelta), 0,
                    Math.max(0, replacements().size() - REPLACEMENT_ROWS));
        } else if (mouseY >= MATERIAL_Y - 2) {
            materialOffset = Mth.clamp(materialOffset - (int) Math.signum(scrollDelta), 0,
                    Math.max(0, preview.materials().size() - MATERIAL_COLUMNS));
        } else {
            return false;
        }
        return true;
    }

    private void changeLayer(int delta) {
        int layer = preview.layer() + delta;
        preview.setLayer(layer < -1 ? preview.getHeight() - 1 : layer >= preview.getHeight() ? -1 : layer);
        if (selected != null && !preview.isVisible(selected.pos())) {
            selected = null;
            replacementOffset = 0;
        }
    }

    private void changeLength(int delta) {
        int length = Mth.clamp(preview.slices() + delta, recipe.pattern().minSlices(), recipe.maxSlices().getAsInt());
        if (length != preview.slices()) {
            preview.setSlices(length);
            selected = null;
            replacementOffset = 0;
            materialOffset = 0;
        }
    }

    private boolean hasVariableLength() {
        return recipe.maxSlices().getAsInt() > recipe.pattern().minSlices();
    }

    private List<Block> replacements() {
        return selected == null ? List.of() : selected.candidates();
    }

    private int replacementIndex(double mouseX, double mouseY) {
        if (!contains(REPLACEMENTS, mouseX, mouseY)) {
            return -1;
        }
        int index = replacementOffset + (int) (mouseY - REPLACEMENTS.top()) / SLOT;
        return index < replacements().size() ? index : -1;
    }

    private int materialIndex(double mouseX, double mouseY) {
        if (mouseX < MATERIAL_X || mouseX >= MATERIAL_X + MATERIAL_COLUMNS * SLOT
                || mouseY < MATERIAL_Y || mouseY >= MATERIAL_Y + 16) {
            return -1;
        }
        int column = (int) (mouseX - MATERIAL_X) / SLOT;
        int index = column == 0 ? 0 : materialOffset + column;
        return index < preview.materials().size() ? index : -1;
    }

    private static boolean contains(ScreenRectangle area, double mouseX, double mouseY) {
        return mouseX >= area.left() && mouseX < area.right() && mouseY >= area.top() && mouseY < area.bottom();
    }

    private static void drawButton(GuiGraphics graphics, ScreenRectangle area, String label, boolean enabled,
            double mouseX, double mouseY) {
        int color = enabled && contains(area, mouseX, mouseY) ? ACCENT : 0xFFC2CCD4;
        graphics.fill(area.left(), area.top(), area.right(), area.bottom(), color);
        var font = Minecraft.getInstance().font;
        graphics.drawString(font, label, area.left() + (area.width() - font.width(label)) / 2,
                area.top() + (area.height() - font.lineHeight) / 2, enabled ? TEXT : 0xFF9AA5AE, false);
    }

    private static void drawCentered(GuiGraphics graphics, Component label, int x, int y, int width) {
        var font = Minecraft.getInstance().font;
        var clipped = font.plainSubstrByWidth(label.getString(), width);
        graphics.drawString(font, clipped, x + (width - font.width(clipped)) / 2, y, TEXT, false);
    }

    private static void addItemTooltip(ITooltipBuilder tooltip, ItemStack stack) {
        var minecraft = Minecraft.getInstance();
        tooltip.addAll(stack.getTooltipLines(minecraft.player,
                minecraft.options.advancedItemTooltips ? TooltipFlag.Default.ADVANCED : TooltipFlag.Default.NORMAL));
    }

    private static MutableComponent text(String key, Object... args) {
        return Component.translatable("jei.ae2pr.multiblock." + key, args);
    }
}
