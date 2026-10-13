package com.murthinext.ae2pr.compat.jade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
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
import com.murthinext.ae2pr.block.matter_phase_transformer.MatterPhaseTransformerBlock;
import com.murthinext.ae2pr.block.matter_phase_transformer.MatterPhaseTransformerControllerBlockEntity;

/**
 * 显示物质相变仪连接状态与作业的 Jade 插件。
 */
@WailaPlugin(ae2pr.MODID)
public class MatterPhaseTransformerJadePlugin implements IWailaPlugin {

    private static final String KEY_FORMED = "formed";
    private static final String KEY_ENERGY_CONNECTED = "energyConnected";
    private static final String KEY_ELAPSED = "elapsed";
    private static final String KEY_DURATION = "duration";
    private static final String KEY_FLUID = "fluid";
    private static final String KEY_AMOUNT = "amount";

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(ServerData.INSTANCE, MatterPhaseTransformerControllerBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(ComponentProvider.INSTANCE, MatterPhaseTransformerBlock.class);
    }

    /**
     * 服务端数据提供器。
     */
    private enum ServerData implements IServerDataProvider<BlockAccessor> {
        INSTANCE;

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof MatterPhaseTransformerControllerBlockEntity controller)) {
                return;
            }
            data.putBoolean(KEY_FORMED, controller.isFormed());
            data.putBoolean(KEY_ENERGY_CONNECTED, controller.isEnergyConnected());
            data.putInt(KEY_ELAPSED, controller.getJobElapsed());
            data.putInt(KEY_DURATION, controller.getJobDuration());
            FluidStack output = controller.getJobFluid();
            if (!output.isEmpty()) {
                data.putString(KEY_FLUID, ForgeRegistries.FLUIDS.getKey(output.getFluid()).toString());
                data.putInt(KEY_AMOUNT, output.getAmount());
            }
        }

        @Override
        public ResourceLocation getUid() {
            return ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, "matter_phase_transformer_data");
        }
    }

    /**
     * 客户端提示提供器。
     */
    private enum ComponentProvider implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (!data.contains(KEY_FORMED)) {
                return;
            }
            tooltip.add(Component.translatable(data.getBoolean(KEY_ENERGY_CONNECTED)
                    ? "jade.ae2pr.multiblock.connected"
                    : "jade.ae2pr.multiblock.disconnected"));

            int duration = data.getInt(KEY_DURATION);
            String fluidId = data.getString(KEY_FLUID);
            if (!fluidId.isEmpty() && duration > 0) {
                Fluid fluid = ForgeRegistries.FLUIDS.getValue(ResourceLocation.parse(fluidId));
                if (fluid != null) {
                    FluidStack output = new FluidStack(fluid, data.getInt(KEY_AMOUNT));
                    tooltip.add(Component.translatable("jade.ae2pr.matter_phase_transformer.job",
                            output.getDisplayName(), data.getInt(KEY_AMOUNT)));
                }
                int elapsed = data.getInt(KEY_ELAPSED);
                float progress = Math.min(1.0F, (float) elapsed / duration);
                IProgressStyle style = tooltip.getElementHelper().progressStyle().color(0xFF62D0DA, 0xFF1B222B);
                tooltip.add(tooltip.getElementHelper().progress(progress,
                        Component.translatable("jade.ae2pr.matter_phase_transformer.progress",
                                seconds(elapsed), seconds(duration)),
                        style, BoxStyle.DEFAULT, false));
            }
        }

        @Override
        public ResourceLocation getUid() {
            return ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, "matter_phase_transformer");
        }

        private static String seconds(int ticks) {
            return String.format(java.util.Locale.ROOT, "%.1f", ticks / 20.0D);
        }
    }
}
