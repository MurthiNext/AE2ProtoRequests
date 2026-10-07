package com.murthinext.ae2pr.compat.jade;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.block.lava_smelter.HighReactivityLavaSmelterBlock;
import com.murthinext.ae2pr.block.lava_smelter.LavaSmelterControllerBlockEntity;

/**
 * Jade 兼容插件：显示高反应性熔岩冶炼炉的成型状态与首个不符结构位置。
 */
@WailaPlugin(ae2pr.MODID)
public class LavaSmelterJadePlugin implements IWailaPlugin {

    private static final String KEY_FORMED = "formed";
    private static final String KEY_MISMATCHES = "mismatches";
    private static final String KEY_MISMATCH_POS = "mismatchPos";

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(ServerData.INSTANCE, LavaSmelterControllerBlockEntity.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(ComponentProvider.INSTANCE, HighReactivityLavaSmelterBlock.class);
    }

    /** 服务端数据：成型状态与不符诊断。 */
    private enum ServerData implements IServerDataProvider<BlockAccessor> {
        INSTANCE;

        @Override
        public void appendServerData(CompoundTag data, BlockAccessor accessor) {
            if (!(accessor.getBlockEntity() instanceof LavaSmelterControllerBlockEntity controller)) {
                return;
            }
            data.putBoolean(KEY_FORMED, controller.isFormed());
            data.putInt(KEY_MISMATCHES, controller.getLastMismatches());
            BlockPos pos = controller.getLastMismatchPos();
            if (pos != null) {
                data.putLong(KEY_MISMATCH_POS, pos.asLong());
            }
        }

        @Override
        public ResourceLocation getUid() {
            return new ResourceLocation(ae2pr.MODID, "lava_smelter_data");
        }
    }

    /** 客户端展示：状态与首个不符位置。 */
    private enum ComponentProvider implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            CompoundTag data = accessor.getServerData();
            if (!data.contains(KEY_FORMED)) {
                return;
            }
            boolean formed = data.getBoolean(KEY_FORMED);
            tooltip.add(Component.translatable("jade.ae2pr.lava_smelter.status",
                    Component.translatable(formed
                            ? "gui.ae2pr.lava_smelter.status.formed"
                            : "gui.ae2pr.lava_smelter.status.unformed")));
            if (!formed && data.contains(KEY_MISMATCH_POS)) {
                BlockPos pos = BlockPos.of(data.getLong(KEY_MISMATCH_POS));
                tooltip.add(Component.translatable("gui.ae2pr.lava_smelter.mismatch",
                        data.getInt(KEY_MISMATCHES), pos.getX() + ", " + pos.getY() + ", " + pos.getZ()));
            }
        }

        @Override
        public ResourceLocation getUid() {
            return new ResourceLocation(ae2pr.MODID, "lava_smelter");
        }
    }
}
