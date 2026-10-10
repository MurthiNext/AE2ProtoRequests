package com.murthinext.ae2pr.compat.jade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import appeng.util.ReadableNumberConverter;

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
import com.murthinext.ae2pr.ModModules;
import com.murthinext.ae2pr.block.assembly_line.AssemblyLineControllerBlockEntity;
import com.murthinext.ae2pr.block.assembly_line.CrystalAssemblyLineBlock;

/**
 * Jade 兼容插件：显示水晶装配线的成型状态、当前作业产物与进度。
 */
@WailaPlugin(ae2pr.MODID)
public class AssemblyLineJadePlugin implements IWailaPlugin {

    private static final String KEY_FORMED = "formed";
    private static final String KEY_RUNNING = "running";
    private static final String KEY_PAUSED = "paused";
    private static final String KEY_ELAPSED = "elapsed";
    private static final String KEY_DURATION = "duration";
    private static final String KEY_ITEM = "item";
    private static final String KEY_COUNT = "count";
    private static final String KEY_ENERGY_CONNECTED = "energyConnected";
    private static final String KEY_POWER = "power";
    private static final String KEY_CHARGING = "charging";
    private static final String KEY_PARALLEL = "parallel";
    private static final String KEY_SPEED = "speed";

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(ServerData.INSTANCE, AssemblyLineControllerBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(ComponentProvider.INSTANCE, CrystalAssemblyLineBlock.class);
    }

    /** 服务端数据：成型状态、当前作业与 ME 能源。 */
    private enum ServerData implements IServerDataProvider<BlockAccessor> {
        INSTANCE;

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof AssemblyLineControllerBlockEntity controller)) {
                return;
            }
            data.putBoolean(KEY_FORMED, controller.isFormed());
            data.putBoolean(KEY_RUNNING, controller.isRunning());
            data.putBoolean(KEY_PAUSED, controller.isPaused());
            data.putInt(KEY_ELAPSED, controller.getJobElapsed());
            data.putInt(KEY_DURATION, controller.getJobDuration());
            ItemStack output = controller.getJobOutput();
            if (!output.isEmpty()) {
                data.putString(KEY_ITEM, ForgeRegistries.ITEMS.getKey(output.getItem()).toString());
                data.putInt(KEY_COUNT, output.getCount());
            }
            data.putBoolean(KEY_ENERGY_CONNECTED, controller.isEnergyConnected());
            data.putDouble(KEY_POWER, controller.getNetworkStoredPower());
            data.putInt(KEY_CHARGING, controller.getModuleLevel(ModModules.CHARGING));
            data.putInt(KEY_PARALLEL, controller.maxParallel());
            data.putDouble(KEY_SPEED, controller.getSpeedMultiplier());
        }

        @Override
        public ResourceLocation getUid() {
            return new ResourceLocation(ae2pr.MODID, "assembly_line_data");
        }
    }

    /** 客户端展示：状态、产物 x 数量、进度条与 ME 能源。 */
    private enum ComponentProvider implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (!data.contains(KEY_FORMED)) {
                return;
            }
            tooltip.add(Component.translatable("jade.ae2pr.crystal_assembly_line.status",
                    Component.translatable(statusKey(data))));

            int charging = data.getInt(KEY_CHARGING);
            if (charging > 0) {
                tooltip.add(Component.translatable("jade.ae2pr.crystal_assembly_line.charging", charging));
            }
            tooltip.add(Component.translatable("jade.ae2pr.crystal_assembly_line.parallel", data.getInt(KEY_PARALLEL)));
            double speed = data.getDouble(KEY_SPEED);
            if (speed > 1.0D) {
                tooltip.add(Component.translatable("jade.ae2pr.crystal_assembly_line.speed", Math.round(speed * 100)));
            }

            int duration = data.getInt(KEY_DURATION);
            String itemId = data.getString(KEY_ITEM);
            if (!itemId.isEmpty() && duration > 0) {
                Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemId));
                if (item != null) {
                    ItemStack output = new ItemStack(item, data.getInt(KEY_COUNT));
                    tooltip.add(Component.translatable("jade.ae2pr.crystal_assembly_line.job",
                            output.getHoverName(), output.getCount()));
                }
                int elapsed = data.getInt(KEY_ELAPSED);
                float progress = Math.min(1.0F, (float) elapsed / duration);
                IProgressStyle style = tooltip.getElementHelper().progressStyle().color(0xFF3E9BC8, 0xFF1B222B);
                tooltip.add(tooltip.getElementHelper().progress(progress,
                        Component.translatable("jade.ae2pr.crystal_assembly_line.progress",
                                seconds(elapsed), seconds(duration)),
                        style, BoxStyle.DEFAULT, false));
            }
            if (data.getBoolean(KEY_ENERGY_CONNECTED)) {
                long power = (long) Math.min(Math.max(data.getDouble(KEY_POWER), 0), Long.MAX_VALUE);
                tooltip.add(Component.translatable("jade.ae2pr.crystal_assembly_line.energy",
                        ReadableNumberConverter.format(power, 5) + " AE"));
            }
        }

        @Override
        public ResourceLocation getUid() {
            return new ResourceLocation(ae2pr.MODID, "assembly_line");
        }

        private static String statusKey(CompoundTag data) {
            if (!data.getBoolean(KEY_FORMED)) {
                return "gui.ae2pr.crystal_assembly_line.status.unformed";
            }
            if (data.getBoolean(KEY_RUNNING)) {
                return "gui.ae2pr.crystal_assembly_line.status.running";
            }
            if (data.getBoolean(KEY_PAUSED)) {
                return "gui.ae2pr.crystal_assembly_line.status.paused";
            }
            return "gui.ae2pr.crystal_assembly_line.status.formed";
        }

        private static String seconds(int ticks) {
            return String.format(java.util.Locale.ROOT, "%.1f", ticks / 20.0D);
        }
    }
}
