package com.murthinext.ae2pr.integration.jade;

import java.util.Map;

import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.BoxStyle;
import snownee.jade.api.ui.IProgressStyle;

import com.murthinext.ae2pr.Config;
import com.murthinext.ae2pr.registry.ModMachines;
import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.machine.MachineBlock;
import com.murthinext.ae2pr.machine.ModeMachineBlockEntity;
import com.murthinext.ae2pr.machine.high_reactivity_lava_smelter.HighReactivityLavaSmelterControllerBlockEntity;
import com.murthinext.ae2pr.machine.multiblock.AbstractMultiblockControllerBlockEntity;

@WailaPlugin(ae2pr.MODID)
public final class MultiblockJadePlugin implements IWailaPlugin {

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
    private static final String KEY_MACHINE = "machine";
    private static final String KEY_MODE = "mode";
    private static final String KEY_DURABILITY = "durability";
    private static final Map<String, Integer> PROGRESS_COLORS = Map.of(
            ModMachines.CRYSTAL_ASSEMBLY_LINE.id(), 0xFF3E9BC8,
            ModMachines.CRYSTAL_REACTION_CHAMBER.id(), 0xFF55A3A8,
            ModMachines.CRYSTAL_WORKSTATION.id(), 0xFF55A3A8,
            ModMachines.JIG.id(), 0xFF7DA9D2,
            ModMachines.FILTER_PRESS.id(), 0xFF7DA9D2,
            ModMachines.HIGH_REACTIVITY_LAVA_SMELTER.id(), 0xFFE0681C,
            ModMachines.MATTER_PHASE_TRANSFORMER.id(), 0xFF62D0DA,
            ModMachines.MULTI_FACTORY.id(), 0xFFA47BD6);

    private final ServerData serverData = new ServerData();
    private final ComponentProvider componentProvider = new ComponentProvider();

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(serverData, AbstractMultiblockControllerBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(componentProvider, MachineBlock.class);
    }

    // 作业状态，默认从控制器基类读取
    protected JobState jobState(BlockEntity controller) {
        AbstractMultiblockControllerBlockEntity base = (AbstractMultiblockControllerBlockEntity) controller;
        return new JobState(base.isFormed(), base.isNetworkConnected(), base.getJobElapsed(), base.getJobDuration(),
                base.getJobItem(), base.getJobFluid());
    }

    protected void appendExtraServerData(CompoundTag data, BlockEntity controller) {
        if (controller instanceof HighReactivityLavaSmelterControllerBlockEntity smelter) {
            data.putInt(KEY_DURABILITY, smelter.getDurability());
        } else if (controller instanceof ModeMachineBlockEntity<?> machine) {
            data.putString(KEY_MODE, machine.getMode().translationKey());
        }
    }

    protected void appendExtraTooltip(ITooltip tooltip, CompoundTag data) {
        if (data.contains(KEY_DURABILITY)) {
            tooltip.add(Component.translatable("gui.ae2pr.high_reactivity_lava_smelter.durability",
                    data.getInt(KEY_DURABILITY), Config.highReactivityLavaSmelterMaxDurability()));
        }
        String mode = data.getString(KEY_MODE);
        if (!mode.isEmpty()) {
            tooltip.add(Component.translatable("gui.ae2pr.multiblock.mode", Component.translatable(mode)));
        }
    }

    protected record JobState(boolean formed, boolean networkConnected, int elapsed, int duration, ItemStack item,
                              FluidStack fluid) {
    }

    private final class ServerData implements IServerDataProvider<BlockAccessor> {

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            BlockEntity controller = accessor.getBlockEntity();
            if (!(controller instanceof AbstractMultiblockControllerBlockEntity base)
                    || !(controller.getBlockState().getBlock() instanceof MachineBlock<?> block)
                    || block.definition().controller(controller) == null) {
                return;
            }
            JobState state = jobState(controller);
            data.putString(KEY_MACHINE, base.machineKey());
            data.putBoolean(KEY_FORMED, state.formed());
            data.putBoolean(KEY_NETWORK_CONNECTED, state.networkConnected());
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
            return ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, "multiblock_data");
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
                    ? "gui.ae2pr.multiblock.network.connected"
                    : "gui.ae2pr.multiblock.network.disconnected"));
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
            IProgressStyle style = tooltip.getElementHelper().progressStyle().color(
                    PROGRESS_COLORS.getOrDefault(data.getString(KEY_MACHINE), 0xFF7DA9D2),
                    PROGRESS_BACKGROUND);
            tooltip.add(tooltip.getElementHelper().progress(progress,
                    Component.translatable("gui.ae2pr.multiblock.job.progress", elapsed, duration),
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
                return Component.translatable("gui.ae2pr.multiblock.job.output", output.getHoverName(),
                        output.getCount());
            }
            String fluidId = data.getString(KEY_FLUID);
            Fluid fluid = fluidId.isEmpty() ? null : ForgeRegistries.FLUIDS.getValue(ResourceLocation.parse(fluidId));
            if (fluid == null) {
                return null;
            }
            FluidStack output = new FluidStack(fluid, data.getInt(KEY_AMOUNT));
            return Component.translatable("gui.ae2pr.multiblock.job.output", output.getDisplayName(),
                    output.getAmount());
        }

        @Override
        public ResourceLocation getUid() {
            return ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, "multiblock");
        }
    }
}
