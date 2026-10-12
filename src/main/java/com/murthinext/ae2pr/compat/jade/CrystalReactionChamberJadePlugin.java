package com.murthinext.ae2pr.compat.jade;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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
import com.murthinext.ae2pr.block.crystal_reaction_chamber.CrystalReactionChamberBlock;
import com.murthinext.ae2pr.block.crystal_reaction_chamber.CrystalReactionChamberControllerBlockEntity;

/**
 * 显示水晶反应仓连接状态与作业的 Jade 插件。
 */
@WailaPlugin(ae2pr.MODID)
public class CrystalReactionChamberJadePlugin implements IWailaPlugin {

    private static final String KEY_FORMED = "formed";
    private static final String KEY_ENERGY_CONNECTED = "energyConnected";
    private static final String KEY_ELAPSED = "elapsed";
    private static final String KEY_DURATION = "duration";
    private static final String KEY_ITEM = "item";
    private static final String KEY_ITEM_COUNT = "itemCount";
    private static final String KEY_FLUID = "fluid";
    private static final String KEY_AMOUNT = "amount";

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(ServerData.INSTANCE, CrystalReactionChamberControllerBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(ComponentProvider.INSTANCE, CrystalReactionChamberBlock.class);
    }

    /**
     * 服务端数据提供器。
     */
    private enum ServerData implements IServerDataProvider<BlockAccessor> {
        INSTANCE;

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof CrystalReactionChamberControllerBlockEntity controller)) {
                return;
            }
            data.putBoolean(KEY_FORMED, controller.isFormed());
            data.putBoolean(KEY_ENERGY_CONNECTED, controller.isEnergyConnected());
            data.putInt(KEY_ELAPSED, controller.getJobElapsed());
            data.putInt(KEY_DURATION, controller.getJobDuration());
            ItemStack item = controller.getJobItem();
            if (!item.isEmpty()) {
                data.putString(KEY_ITEM, ForgeRegistries.ITEMS.getKey(item.getItem()).toString());
                data.putInt(KEY_ITEM_COUNT, item.getCount());
            }
            FluidStack fluid = controller.getJobFluid();
            if (!fluid.isEmpty()) {
                data.putString(KEY_FLUID, ForgeRegistries.FLUIDS.getKey(fluid.getFluid()).toString());
                data.putInt(KEY_AMOUNT, fluid.getAmount());
            }
        }

        @Override
        public ResourceLocation getUid() {
            return ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, "crystal_reaction_chamber_data");
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
            if (duration <= 0) {
                return;
            }
            String itemId = data.getString(KEY_ITEM);
            if (!itemId.isEmpty()) {
                Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(itemId));
                if (item != null) {
                    ItemStack output = new ItemStack(item, data.getInt(KEY_ITEM_COUNT));
                    tooltip.add(Component.translatable("jade.ae2pr.crystal_reaction_chamber.job",
                            output.getHoverName(), output.getCount()));
                }
            } else {
                String fluidId = data.getString(KEY_FLUID);
                Fluid fluid = fluidId.isEmpty() ? null
                        : ForgeRegistries.FLUIDS.getValue(ResourceLocation.parse(fluidId));
                if (fluid != null) {
                    FluidStack output = new FluidStack(fluid, data.getInt(KEY_AMOUNT));
                    tooltip.add(Component.translatable("jade.ae2pr.crystal_reaction_chamber.job_fluid",
                            output.getDisplayName(), output.getAmount()));
                }
            }
            int elapsed = data.getInt(KEY_ELAPSED);
            float progress = Math.min(1.0F, (float) elapsed / duration);
            IProgressStyle style = tooltip.getElementHelper().progressStyle().color(0xFF55A3A8, 0xFF1B222B);
            tooltip.add(tooltip.getElementHelper().progress(progress,
                    Component.translatable("jade.ae2pr.crystal_reaction_chamber.progress",
                            seconds(elapsed), seconds(duration)),
                    style, BoxStyle.DEFAULT, false));
        }

        @Override
        public ResourceLocation getUid() {
            return ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, "crystal_reaction_chamber");
        }

        private static String seconds(int ticks) {
            return String.format(java.util.Locale.ROOT, "%.1f", ticks / 20.0D);
        }
    }
}
