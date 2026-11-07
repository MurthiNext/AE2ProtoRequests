package com.murthinext.ae2pr.api.machine.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import com.murthinext.ae2pr.api.Ids;

// 界面通用贴图与配色：贴图全部集中在 textures/gui 下，按钮框统一 18x18
public final class GuiTextures {

    public static final int SIZE = 18;

    public static final ResourceLocation BUTTON = Ids.of("textures/gui/button.png");
    public static final ResourceLocation BUTTON_HOVER = Ids.of("textures/gui/button_hover.png");
    public static final ResourceLocation BUTTON_SELECTED = Ids.of("textures/gui/button_selected.png");

    public static final ResourceLocation CLEAR = Ids.of("textures/gui/clear.png");
    public static final ResourceLocation GUIDE = Ids.of("textures/gui/guide.png");
    public static final ResourceLocation SIDE_CONFIG = Ids.of("textures/gui/side_config.png");
    public static final ResourceLocation ISOLATION_ON = Ids.of("textures/gui/isolation_on.png");
    public static final ResourceLocation ISOLATION_OFF = Ids.of("textures/gui/isolation_off.png");
    public static final ResourceLocation PULL_ON = Ids.of("textures/gui/pull_on.png");
    public static final ResourceLocation PULL_OFF = Ids.of("textures/gui/pull_off.png");
    public static final ResourceLocation PUSH_ON = Ids.of("textures/gui/push_on.png");
    public static final ResourceLocation PUSH_OFF = Ids.of("textures/gui/push_off.png");

    // 纯色块的描边，取 AE2 的深钢色
    public static final int COLOR_SOLID_BORDER = 0xFF413F54;
    // 面模式语义色：红=输入（拉取），蓝=输出（推出），紫=均可，灰=无
    public static final int COLOR_INPUT = 0xFFDB7573;
    public static final int COLOR_OUTPUT = 0xFF73A3DB;
    public static final int COLOR_BOTH = 0xFF8773DB;
    public static final int COLOR_NONE = 0xFF6E7381;

    private GuiTextures() {
    }

    public static void frame(GuiGraphics graphics, ResourceLocation texture, int x, int y) {
        graphics.blit(texture, x, y, 0, 0.0F, 0.0F, SIZE, SIZE, SIZE, SIZE);
    }

    // 纯色立体块：深色描边 + 顶面亮边 + 底部压边，配色由调用方按语义给定
    public static void solidBlock(GuiGraphics graphics, int x, int y, int color, boolean hovered) {
        int rim = lighten(color, hovered ? 0.45F : 0.28F);
        int shade = darken(color, 0.22F);
        graphics.fill(x, y, x + SIZE, y + SIZE, COLOR_SOLID_BORDER);
        graphics.fill(x + 1, y + 1, x + SIZE - 1, y + SIZE - 1, color);
        graphics.fill(x + 1, y + 1, x + SIZE - 1, y + 2, rim);
        graphics.fill(x + 1, y + SIZE - 2, x + SIZE - 1, y + SIZE - 1, shade);
    }

    // 把颜色朝白色提亮，amount 为 0~1
    public static int lighten(int color, float amount) {
        int r = (int) (((color >> 16) & 0xFF) + (0xFF - ((color >> 16) & 0xFF)) * amount);
        int g = (int) (((color >> 8) & 0xFF) + (0xFF - ((color >> 8) & 0xFF)) * amount);
        int b = (int) ((color & 0xFF) + (0xFF - (color & 0xFF)) * amount);
        return (color & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    // 把颜色朝黑色压暗，amount 为 0~1
    public static int darken(int color, float amount) {
        float k = 1.0F - amount;
        int r = (int) (((color >> 16) & 0xFF) * k);
        int g = (int) (((color >> 8) & 0xFF) * k);
        int b = (int) ((color & 0xFF) * k);
        return (color & 0xFF000000) | (r << 16) | (g << 8) | b;
    }
}
