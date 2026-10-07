package com.murthinext.ae2pr.compat.jade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.IProgressStyle;

import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.block.lava_smelter.HighReactivityLavaSmelterBlock;
import com.murthinext.ae2pr.block.lava_smelter.LavaSmelterControllerBlockEntity;

/**
 * Jade 兼容插件：显示熔岩冶炼炉的成型状态、当前作业与进度、配方耐久。
 */
@WailaPlugin(ae2pr.MODID)
public class LavaSmelterJadePlugin implements IWailaPlugin {

    private static final String KEY_FORMED = "formed";
    private static final String KEY_RUNNING = "running";
    private static final String KEY_PAUSED = "paused";
    private static final String KEY_DURABILITY = "durability";
    private static final String KEY_ELAPSED = "elapsed";
    private static final String KEY_DURATION = "duration";
    private static final String KEY_ITEM = "item";
    private static final String KEY_COUNT = "count";

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(ServerData.INSTANCE, LavaSmelterControllerBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(ComponentProvider.INSTANCE, HighReactivityLavaSmelterBlock.class);
    }

    /** 服务端数据：成型状态、当前作业与配方耐久。 */
    private enum ServerData implements IServerDataProvider<BlockAccessor> {
        INSTANCE;

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof LavaSmelterControllerBlockEntity controller)) {
                return;
            }
            data.putBoolean(KEY_FORMED, controller.isFormed());
            data.putBoolean(KEY_RUNNING, controller.isRunning());
            data.putBoolean(KEY_PAUSED, controller.isPaused());
            data.putInt(KEY_DURABILITY, controller.getDurability());
            data.putInt(KEY_ELAPSED, controller.getJobElapsed());
            data.putInt(KEY_DURATION, controller.getJobDuration());
            ItemStack output = controller.getJobOutput();
            if (!output.isEmpty()) {
                data.putString(KEY_ITEM, ForgeRegistries.ITEMS.getKey(output.getItem()).toString());
                data.putInt(KEY_COUNT, output.getCount());
            }
        }

        @Override
        public ResourceLocation getUid() {
            return new ResourceLocation(ae2pr.MODID, "lava_smelter_data");
        }
    }

    /** 客户端展示：状态、产物 x 数量、进度条与配方耐久。 */
    private enum ComponentProvider implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (!data.contains(KEY_FORMED)) {
                return;
            }
            tooltip.add(Component.translatable("jade.ae2pr.lava_smelter.status",
                    Component.translatable(statusKey(data))));

            int duration = data.getInt(KEY_DURATION);
            String itemId = data.getString(KEY_ITEM);
            if (!itemId.isEmpty() && duration > 0) {
                Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemId));
                if (item != null) {
                    ItemStack output = new ItemStack(item, data.getInt(KEY_COUNT));
                    tooltip.add(Component.translatable("jade.ae2pr.lava_smelter.job",
                            output.getHoverName(), output.getCount()));
                }
                int elapsed = data.getInt(KEY_ELAPSED);
                float progress = Math.min(1.0F, (float) elapsed / duration);
                IProgressStyle style = tooltip.getElementHelper().progressStyle().color(0xFFE0681C, 0xFF1B222B);
                tooltip.add(tooltip.getElementHelper().progress(progress,
                        Component.translatable("jade.ae2pr.lava_smelter.progress",
                                seconds(elapsed), seconds(duration)),
                        style, BoxStyle.DEFAULT, false));
            }
            tooltip.add(Component.translatable("jade.ae2pr.lava_smelter.durability",
                    data.getInt(KEY_DURABILITY), LavaSmelterControllerBlockEntity.MAX_DURABILITY));
        }

        @Override
        public ResourceLocation getUid() {
            return new ResourceLocation(ae2pr.MODID, "lava_smelter");
        }

        private static String statusKey(CompoundTag data) {
            if (!data.getBoolean(KEY_FORMED)) {
                return "gui.ae2pr.lava_smelter.status.unformed";
            }
            if (data.getBoolean(KEY_RUNNING)) {
                return "gui.ae2pr.lava_smelter.status.running";
            }
            if (data.getBoolean(KEY_PAUSED)) {
                return "gui.ae2pr.lava_smelter.status.paused";
            }
            return "gui.ae2pr.lava_smelter.status.formed";
        }

        private static String seconds(int ticks) {
            return String.format(java.util.Locale.ROOT, "%.1f", ticks / 20.0D);
        }
    }
}
