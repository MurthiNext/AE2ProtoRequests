package com.murthinext.ae2pr.integration.jade;

import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
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
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.IProgressStyle;

import com.murthinext.ae2pr.multiblock.AbstractMultiblockControllerBlockEntity;

public abstract class AbstractMultiblockJadePlugin implements IWailaPlugin {

    protected static final String KEY_FORMED = "formed";
    protected static final String KEY_NETWORK_CONNECTED = "networkConnected";
    protected static final String KEY_ALLOCATED_CHANNELS = "allocatedChannels";
    protected static final String KEY_REQUIRED_CHANNELS = "requiredChannels";
    protected static final String KEY_ELAPSED = "elapsed";
    protected static final String KEY_DURATION = "duration";
    protected static final String KEY_ITEM = "item";
    protected static final String KEY_COUNT = "count";
    protected static final String KEY_FLUID = "fluid";
    protected static final String KEY_AMOUNT = "amount";

    private static final int PROGRESS_BACKGROUND = 0xFF1B222B;

    private final ServerData serverData = new ServerData();
    private final ComponentProvider componentProvider = new ComponentProvider();

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(serverData, controllerType());
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(componentProvider, blockType());
    }

    protected abstract Class<? extends BlockEntity> controllerType();

    protected abstract Class<? extends Block> blockType();

    protected abstract ResourceLocation dataUid();

    protected abstract ResourceLocation componentUid();

    protected abstract int progressColor();

    // 作业状态，默认从控制器基类读取
    protected JobState jobState(BlockEntity controller) {
        AbstractMultiblockControllerBlockEntity base = (AbstractMultiblockControllerBlockEntity) controller;
        return new JobState(base.isFormed(), base.isNetworkConnected(), base.getJobElapsed(), base.getJobDuration(),
                base.getJobItem(), base.getJobFluid());
    }

    protected void appendExtraServerData(CompoundTag data, BlockEntity controller) {
    }

    protected void appendExtraTooltip(ITooltip tooltip, CompoundTag data) {
    }

    protected record JobState(boolean formed, boolean networkConnected, int elapsed, int duration, ItemStack item,
                              FluidStack fluid) {
    }

    private final class ServerData implements IServerDataProvider<BlockAccessor> {

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            BlockEntity controller = accessor.getBlockEntity();
            if (controller == null || !controllerType().isInstance(controller)) {
                return;
            }
            JobState state = jobState(controller);
            data.putBoolean(KEY_FORMED, state.formed());
            data.putBoolean(KEY_NETWORK_CONNECTED, state.networkConnected());
            AbstractMultiblockControllerBlockEntity base = (AbstractMultiblockControllerBlockEntity) controller;
            data.putInt(KEY_ALLOCATED_CHANNELS, base.getAllocatedChannels());
            data.putInt(KEY_REQUIRED_CHANNELS, base.getRequiredChannels());
            data.putInt(KEY_ELAPSED, state.elapsed());
            data.putInt(KEY_DURATION, state.duration());
            if (!state.item().isEmpty()) {
                data.putString(KEY_ITEM, ForgeRegistries.ITEMS.getKey(state.item().getItem()).toString());
                data.putInt(KEY_COUNT, state.item().getCount());
            } else if (!state.fluid().isEmpty()) {
                data.putString(KEY_FLUID, ForgeRegistries.FLUIDS.getKey(state.fluid().getFluid()).toString());
                data.putInt(KEY_AMOUNT, state.fluid().getAmount());
            }
            appendExtraServerData(data, controller);
        }

        @Override
        public ResourceLocation getUid() {
            return dataUid();
        }
    }

    private final class ComponentProvider implements IBlockComponentProvider {

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (!data.contains(KEY_FORMED)) {
                return;
            }
            tooltip.add(Component.translatable(data.getBoolean(KEY_NETWORK_CONNECTED)
                    ? "jade.ae2pr.multiblock.connected"
                    : "jade.ae2pr.multiblock.disconnected"));
            tooltip.add(Component.translatable("gui.ae2pr.multiblock.channels",
                    data.getInt(KEY_ALLOCATED_CHANNELS), data.getInt(KEY_REQUIRED_CHANNELS)));
            appendJob(tooltip, data);
            appendExtraTooltip(tooltip, data);
        }

        private void appendJob(ITooltip tooltip, CompoundTag data) {
            int duration = data.getInt(KEY_DURATION);
            if (duration <= 0) {
                return;
            }
            Component job = jobComponent(data);
            if (job != null) {
                tooltip.add(job);
            }
            int elapsed = data.getInt(KEY_ELAPSED);
            float progress = Math.min(1.0F, (float) elapsed / duration);
            IProgressStyle style = tooltip.getElementHelper().progressStyle().color(progressColor(),
                    PROGRESS_BACKGROUND);
            tooltip.add(tooltip.getElementHelper().progress(progress,
                    Component.translatable("jade.ae2pr.multiblock.progress", elapsed, duration),
                    style, BoxStyle.DEFAULT, false));
        }

        @Nullable
        private Component jobComponent(CompoundTag data) {
            String itemId = data.getString(KEY_ITEM);
            if (!itemId.isEmpty()) {
                Item item = ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(itemId));
                if (item == null) {
                    return null;
                }
                ItemStack output = new ItemStack(item, data.getInt(KEY_COUNT));
                return Component.translatable("jade.ae2pr.multiblock.job", output.getHoverName(), output.getCount());
            }
            String fluidId = data.getString(KEY_FLUID);
            Fluid fluid = fluidId.isEmpty() ? null : ForgeRegistries.FLUIDS.getValue(ResourceLocation.parse(fluidId));
            if (fluid == null) {
                return null;
            }
            FluidStack output = new FluidStack(fluid, data.getInt(KEY_AMOUNT));
            return Component.translatable("jade.ae2pr.multiblock.job", output.getDisplayName(), output.getAmount());
        }

        @Override
        public ResourceLocation getUid() {
            return componentUid();
        }
    }
}
