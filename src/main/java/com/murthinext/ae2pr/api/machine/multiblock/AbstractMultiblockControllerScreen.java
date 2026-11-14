package com.murthinext.ae2pr.api.machine.multiblock;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;

import appeng.util.ReadableNumberConverter;

import com.murthinext.ae2pr.api.machine.MachineStat;
import com.murthinext.ae2pr.api.machine.MachineStats;
import com.murthinext.ae2pr.api.machine.client.AbstractMachineScreen;
import com.murthinext.ae2pr.api.machine.client.CustomIngredientSource;
import com.murthinext.ae2pr.api.machine.client.GuiTooltips;
import com.murthinext.ae2pr.api.machine.client.MachineStyles;
import com.murthinext.ae2pr.api.machine.client.MachineTabButton;
import com.murthinext.ae2pr.api.machine.part.MachinePart;

public abstract class AbstractMultiblockControllerScreen<M extends AbstractMultiblockControllerMenu<?>>
        extends AbstractMachineScreen<M> implements CustomIngredientSource {

    protected static final int TEXT_X = 7;
    protected static final int EXTRA_Y = 104;
    private static final int STATUS_Y = 30;
    private static final int POWER_Y = 41;
    private static final int PARALLEL_Y = 52;
    private static final int JOB_Y = 63;
    private static final int JOB_ITEM_X = 7;
    private static final int JOB_ITEM_Y = 74;
    private static final int JOB_TEXT_X = 27;
    private static final int JOB_TEXT_Y = 78;
    private static final int ERROR_Y = 92;
    private static final int JOB_TAIL_GAP = 4;
    private static final double JOB_NAME_SCROLL_SPEED = 12.0D;
    private static final int JOB_NAME_SCROLL_GAP = 24;
    private static final int TAB_Y = 1 - MachineTabButton.HEIGHT;
    private static final int DATA_Y = 30;
    private static final int DATA_STEP = 11;

    private static final int COLOR_OK = 0x55FF55;
    private static final int COLOR_FAIL = 0xFF5555;
    private static final int COLOR_RUNNING = 0xFFD060;
    private static final int COLOR_PAUSED = 0xFFDE00;
    private static final int COLOR_INFO = 0xACE9FF;
    private static final int COLOR_GRAY = 0x7A8794;
    private boolean dataPage;

    protected AbstractMultiblockControllerScreen(M menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(new MachineTabButton(leftPos, topPos + TAB_Y,
                                                 Component.translatable("gui.ae2pr.multiblock.tab.home"),
                                                 separatorAccent(), () -> !dataPage, () -> dataPage = false));
        addRenderableWidget(new MachineTabButton(leftPos + MachineTabButton.WIDTH, topPos + TAB_Y,
                                                 Component.translatable("gui.ae2pr.multiblock.tab.data"),
                                                 separatorAccent(), () -> dataPage, () -> dataPage = true));
    }

    public boolean isDataPage() {
        return dataPage;
    }

    @Override
    protected int topExtension() {
        return -TAB_Y;
    }

    @Override
    public List<Rect2i> getGuiExtraAreas() {
        List<Rect2i> areas = new ArrayList<>(super.getGuiExtraAreas());
        areas.add(new Rect2i(leftPos, topPos + TAB_Y, MachineTabButton.WIDTH * 2, MachineTabButton.HEIGHT));
        return areas;
    }

    @Override
    protected boolean hasClickedOutside(double mouseX, double mouseY, int guiLeft, int guiTop, int button) {
        for (Rect2i area : getGuiExtraAreas()) {
            if (mouseX >= area.getX() && mouseX < area.getX() + area.getWidth() && mouseY >= area.getY() &&
                mouseY < area.getY() + area.getHeight()) {
                return false;
            }
        }
        return super.hasClickedOutside(mouseX, mouseY, guiLeft, guiTop, button);
    }

    @Override
    protected ResourceLocation guidePage() {
        return MachineStyles.of(menu.definition()).guidePage();
    }

    @Override
    protected ItemLike upgradeMachine() {
        return menu.definition().block();
    }

    @Override
    protected int titleColor() {
        return MachineStyles.of(menu.definition()).titleColor();
    }

    @Override
    protected int separatorAccent() {
        return MachineStyles.of(menu.definition()).accent();
    }

    @Override
    protected int separatorAccentDark() {
        return MachineStyles.of(menu.definition()).accentDark();
    }

    @Nullable
    protected Component errorText() {
        AbstractMultiblockControllerBlockEntity.Error error = menu.getError();
        return error == AbstractMultiblockControllerBlockEntity.Error.NONE ? null : errorText(error);
    }

    protected Component errorText(AbstractMultiblockControllerBlockEntity.Error error) {
        return Component.translatable("gui.ae2pr.multiblock.error." + error.key());
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        super.renderLabels(graphics, mouseX, mouseY);
        if (dataPage) {
            renderDataLabels(graphics);
            return;
        }
        graphics.drawString(font,
                            Component.translatable(menu.isFormed() ? "gui.ae2pr.multiblock.formed"
                                                                   : "gui.ae2pr.multiblock.unformed"),
                            TEXT_X, STATUS_Y, menu.isFormed() ? COLOR_OK : COLOR_FAIL, false);
        graphics.drawString(font, powerText(), TEXT_X, POWER_Y, menu.isNetworkConnected() ? COLOR_INFO : COLOR_GRAY,
                            false);
        graphics.drawString(font, Component.translatable("gui.ae2pr.multiblock.parallel", menu.getMaxParallel()),
                            TEXT_X, PARALLEL_Y, COLOR_INFO, false);
        graphics.drawString(font, Component.translatable(statusKey()), TEXT_X, JOB_Y, jobColor(), false);
        Component error = errorText();
        if (error != null) {
            graphics.drawString(font, error, TEXT_X, ERROR_Y, COLOR_FAIL, false);
        }
        renderJob(graphics);
        renderExtraLabels(graphics, mouseX, mouseY);
    }

    protected void renderExtraLabels(GuiGraphics graphics, int mouseX, int mouseY) {
    }

    private void renderDataLabels(GuiGraphics graphics) {
        MachineStats stats = menu.getMachineStats();
        if (stats == null) {
            graphics.drawString(font, Component.translatable("gui.ae2pr.multiblock.data.waiting"), TEXT_X, DATA_Y,
                                COLOR_GRAY, false);
            return;
        }
        for (int row = 0; row < 3; row++) {
            graphics.drawString(font, dataText(stats, row), TEXT_X, DATA_Y + row * DATA_STEP, COLOR_INFO, false);
        }
    }

    private Component dataText(MachineStats stats, int row) {
        String value = row == 1 && !stats.recipeEnergy()
                ? Component.translatable("gui.ae2pr.multiblock.data.not_applicable").getString()
                : formatStat(stat(stats, row).value(), row != 2);
        return Component.translatable("gui.ae2pr.multiblock.data." + switch (row) {
            case 0 -> "speed";
            case 1 -> "energy";
            default -> "parallel";
        }, value);
    }

    private static MachineStat stat(MachineStats stats, int row) {
        return switch (row) {
            case 0 -> stats.speed();
            case 1 -> stats.energy();
            default -> stats.parallel();
        };
    }

    private static String formatStat(double value, boolean multiplier) {
        double magnitude = Math.abs(value);
        NumberFormat format = magnitude != 0 && (magnitude < 0.0001 || magnitude >= 1_000_000_000)
                ? new DecimalFormat("0.####E0")
                : NumberFormat.getNumberInstance();
        format.setMaximumFractionDigits(4);
        return format.format(value) + (multiplier ? "×" : "");
    }

    private boolean renderDataTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        MachineStats stats = menu.getMachineStats();
        if (stats == null) {
            return false;
        }
        for (int row = 0; row < 3; row++) {
            Component text = dataText(stats, row);
            if (!isHovering(TEXT_X, DATA_Y + row * DATA_STEP, font.width(text), font.lineHeight, mouseX, mouseY)) {
                continue;
            }
            List<Component> lines = new ArrayList<>();
            lines.add(text);
            if (row != 1 || stats.recipeEnergy()) {
                double scale = row == 1 ? stats.energyBase() : 1;
                for (MachineStat.Source source : stat(stats, row).sources()) {
                    String before = formatStat(source.before() / scale, row != 2);
                    String after = formatStat(source.after() / scale, row != 2);
                    lines.add(source.before() == source.after()
                                      ? Component.translatable("gui.ae2pr.multiblock.data.source.value", source.name(),
                                                               after)
                                      : Component.translatable("gui.ae2pr.multiblock.data.source.change", source.name(),
                                                               before, after));
                }
            }
            if (row == 1 && stats.energyContext() != null) {
                lines.add(stats.energyContext());
            }
            GuiTooltips.render(graphics, font, lines, mouseX, mouseY);
            return true;
        }
        return false;
    }

    private void renderJob(GuiGraphics graphics) {
        int duration = menu.getJobDuration();
        if (duration <= 0) {
            return;
        }
        Component name;
        String amount;
        ItemStack output = menu.getJobItem();
        if (!output.isEmpty()) {
            graphics.renderItem(output, JOB_ITEM_X, JOB_ITEM_Y);
            name = output.getHoverName();
            amount = "x" + NumberFormat.getIntegerInstance().format(output.getCount());
        } else {
            FluidStack fluid = menu.getJobFluid();
            if (fluid.isEmpty()) {
                return;
            }
            renderFluidIcon(graphics, fluid);
            name = fluid.getDisplayName();
            amount = NumberFormat.getIntegerInstance().format(fluid.getAmount()) + " mB";
        }
        Component tail = Component.literal(amount).append(Component.translatable("gui.ae2pr.multiblock.job.time",
                                                                                 menu.getJobElapsed(), duration));
        int tailX = imageWidth - 2 - font.width(tail);
        renderJobName(graphics, name.getString(), tailX - JOB_TAIL_GAP - JOB_TEXT_X);
        graphics.drawString(font, tail, tailX, JOB_TEXT_Y, COLOR_RUNNING, false);
    }

    private void renderFluidIcon(GuiGraphics graphics, FluidStack fluid) {
        IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(fluid.getFluid());
        TextureAtlasSprite sprite = Minecraft.getInstance()
                                            .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                                            .apply(extensions.getStillTexture(fluid));
        int color = extensions.getTintColor(fluid);
        graphics.setColor(((color >> 16) & 0xFF) / 255.0F, ((color >> 8) & 0xFF) / 255.0F, (color & 0xFF) / 255.0F,
                          ((color >>> 24) & 0xFF) / 255.0F);
        graphics.blit(JOB_ITEM_X, JOB_ITEM_Y, 0, 16, 16, sprite);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void renderJobName(GuiGraphics graphics, String name, int maxWidth) {
        if (maxWidth <= 0) {
            return;
        }
        int width = font.width(name);
        if (width <= maxWidth) {
            graphics.drawString(font, name, JOB_TEXT_X, JOB_TEXT_Y, COLOR_RUNNING, false);
            return;
        }
        int cycle = width + JOB_NAME_SCROLL_GAP;
        int shift = (int) (Util.getMillis() * JOB_NAME_SCROLL_SPEED / 1000.0D % cycle);
        // 绘制使用局部坐标，裁剪使用屏幕绝对坐标。
        graphics.flush();
        graphics.enableScissor(leftPos + JOB_TEXT_X, topPos + JOB_TEXT_Y, leftPos + JOB_TEXT_X + maxWidth,
                               topPos + JOB_TEXT_Y + font.lineHeight);
        graphics.drawString(font, name, JOB_TEXT_X - shift, JOB_TEXT_Y, COLOR_RUNNING, false);
        graphics.drawString(font, name, JOB_TEXT_X - shift + cycle, JOB_TEXT_Y, COLOR_RUNNING, false);
        graphics.flush();
        graphics.disableScissor();
    }

    private Component powerText() {
        if (!menu.isNetworkConnected()) {
            return Component.translatable("gui.ae2pr.multiblock.network.disconnected");
        }
        double power = menu.getNetworkStoredPower();
        return Component.translatable(
                "gui.ae2pr.multiblock.power.stored",
                ReadableNumberConverter.format((long) Math.min(Math.max(power, 0), Long.MAX_VALUE), 5) + " AE");
    }

    private String statusKey() {
        if (menu.isRunning()) {
            return "gui.ae2pr.multiblock.status.running";
        }
        return menu.isPaused() ? "gui.ae2pr.multiblock.status.paused" : "gui.ae2pr.multiblock.status.idle";
    }

    private int jobColor() {
        if (menu.isRunning()) {
            return COLOR_RUNNING;
        }
        return menu.isPaused() ? COLOR_PAUSED : COLOR_GRAY;
    }

    @Override
    protected boolean renderCustomTooltip(GuiGraphics graphics, int mouseX, int mouseY) {
        if (super.renderCustomTooltip(graphics, mouseX, mouseY)) {
            return true;
        }
        if (dataPage) {
            return renderDataTooltip(graphics, mouseX, mouseY);
        }
        if (mouseX >= leftPos + TEXT_X && mouseX < leftPos + 174 && mouseY >= topPos + POWER_Y &&
            mouseY < topPos + POWER_Y + font.lineHeight) {
            GuiTooltips.render(graphics, font,
                               List.of(Component.translatable("gui.ae2pr.multiblock.channels",
                                                              menu.getAllocatedChannels(), menu.getRequiredChannels()),
                                       Component.translatable("gui.ae2pr.multiblock.channels.limit",
                                                              MachinePart.NetworkPart.MAX_CHANNELS)),
                               mouseX, mouseY);
            return true;
        }
        if (jobItemArea().contains(mouseX, mouseY)) {
            ItemStack item = menu.getJobItem();
            if (!item.isEmpty()) {
                graphics.renderTooltip(font, Screen.getTooltipFromItem(minecraft, item), item.getTooltipImage(), item,
                                       mouseX, mouseY);
                return true;
            }
            FluidStack fluid = menu.getJobFluid();
            if (!fluid.isEmpty()) {
                GuiTooltips.render(
                        graphics, font,
                        List.of(fluid.getDisplayName(),
                                Component.translatable("gui.ae2pr.machine_part.tank.mb",
                                                       String.format(java.util.Locale.ROOT, "%,d", fluid.getAmount()))),
                        mouseX, mouseY);
                return true;
            }
        }
        return false;
    }

    @Override
    public List<Area> getCustomAreas() {
        if (dataPage) {
            return List.of();
        }
        ItemStack item = menu.getJobItem();
        if (!item.isEmpty()) {
            return List.of(Area.ofItem(item, jobItemArea()));
        }
        FluidStack fluid = menu.getJobFluid();
        if (!fluid.isEmpty()) {
            return List.of(Area.ofFluid(fluid, jobItemArea()));
        }
        return List.of();
    }

    public Rect2i jobItemArea() {
        return new Rect2i(leftPos + JOB_ITEM_X - 1, topPos + JOB_ITEM_Y - 1, 18, 18);
    }

    // 界面局部坐标，供配方查看器注册点击区域。
    public Rect2i jobStatusArea() {
        return new Rect2i(TEXT_X, JOB_Y, 100, 9);
    }
}
