package com.murthinext.ae2pr.client.ctm;

import java.util.List;
import java.util.function.BiPredicate;

import org.jetbrains.annotations.Nullable;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import com.murthinext.ae2pr.api.Ids;
import com.murthinext.ae2pr.api.ctm.CtmBinding;
import com.murthinext.ae2pr.api.ctm.CtmMask;
import com.murthinext.ae2pr.api.ctm.CtmTextures;
import com.murthinext.ae2pr.api.machine.MachineBlock;
import com.murthinext.ae2pr.api.multiblock.MachinePartFacade;
import com.murthinext.ae2pr.block.MeteorSteelPipeBlock;
import com.murthinext.ae2pr.block.machine_part.MENetworkAccessHatchBlock;
import com.murthinext.ae2pr.block.machine_part.MachinePartBlock;
import com.murthinext.ae2pr.client.multiblock.MultiblockPreview;
import com.murthinext.ae2pr.machine.crystal_workstation.CrystalWorkstationCtm;
import com.murthinext.ae2pr.registry.ModBlocks;
import com.murthinext.ae2pr.registry.ModMachines;

public final class MachineCtm implements CtmBinding {

    public static final MachineCtm INSTANCE = new MachineCtm();

    public static final ResourceLocation AEV_PART_CASING = Ids.of("block/aev_machine_casing");
    public static final ResourceLocation HIGH_REACTIVITY_LAVA_SMELTER_CONTROLLER_CASING = Ids.of(
            "block/high_reactivity_lava_smelter/controller_casing");
    public static final ResourceLocation MATTER_PHASE_TRANSFORMER_CONTROLLER_CASING = Ids.of(
            "block/matter_phase_transformer/controller_casing");
    public static final ResourceLocation CRYSTAL_REACTION_CONTROLLER_CASING = Ids.of(
            "block/crystal_reaction_chamber/controller_casing");
    public static final ResourceLocation CRYSTAL_ASSEMBLY_LINE_CONTROLLER_CASING = Ids.of(
            "block/crystal_assembly_line/controller_casing");
    public static final ResourceLocation CRYSTAL_WORKSTATION_CONTROLLER_CASING = Ids.of(
            "block/crystal_workstation/controller_casing");
    public static final ResourceLocation JIG_CONTROLLER_CASING = Ids.of("block/jig/controller_casing");
    public static final ResourceLocation MULTI_FACTORY_CONTROLLER_CASING = Ids.of(
            "block/multi_factory/controller_casing");
    public static final ResourceLocation FILTER_PRESS_CONTROLLER_CASING = Ids.of(
            "block/filter_press/controller_casing");
    public static final ResourceLocation WORKSTATION_PANEL_INNER = Ids.of("block/crystal_workstation/panel_face");

    private MachineCtm() {
    }

    public static void registerTextures() {
        CtmTextures.registerTile47("block/crystal_reinforced_composite_machine_casing");
        CtmTextures.registerTile47("block/crystal_glass");
        CtmTextures.registerTile47("block/fireproof_crystal_glass");
        CtmTextures.registerTile47("block/aev_machine_casing");
        CtmTextures.registerTile47("block/crystal_energy_casing");
        CtmTextures.registerTile47("block/zirconia_corundum_bricks");
        CtmTextures.registerQuadrant("block/zirconium_iron_alloy_machine_casing");
        for (String casing : List.of("me_computer_casing", "me_advanced_computer_casing")) {
            for (String face : List.of("front", "back", "side", "top", "bottom")) {
                CtmTextures.registerQuadrant("block/" + casing + "/" + face);
            }
        }
        CtmTextures.registerTile47("block/meteor_steel_pipe_side");
        // 仅换装用的底图，不参与连接
        for (ResourceLocation base :
             List.of(HIGH_REACTIVITY_LAVA_SMELTER_CONTROLLER_CASING, MATTER_PHASE_TRANSFORMER_CONTROLLER_CASING,
                     CRYSTAL_REACTION_CONTROLLER_CASING, CRYSTAL_ASSEMBLY_LINE_CONTROLLER_CASING,
                     CRYSTAL_WORKSTATION_CONTROLLER_CASING, JIG_CONTROLLER_CASING, MULTI_FACTORY_CONTROLLER_CASING,
                     FILTER_PRESS_CONTROLLER_CASING, WORKSTATION_PANEL_INNER)) {
            CtmTextures.registerSprite(base);
        }
        // 成型外观贴图随注册表自动登记
        for (MachinePartFacade facade : MachinePartFacade.all()) {
            if (facade.ctmFamily() != 0) {
                CtmTextures.registerTile47(facade.texture());
            } else {
                CtmTextures.registerSprite(facade.texture());
            }
        }
    }

    @Override
    public boolean enabled(BlockState state) {
        return familyKey(state) != 0;
    }

    @Override
    public int mask(BlockAndTintGetter level, BlockPos pos, BlockState state) {
        // 棱方向不做轴向判断，只看方块族归属
        return CtmMask.compute(level, pos,
                               (direction, neighbor, neighborState)
                                       -> direction == null ? connects(neighborState, state)
                                                            : connects(neighborState, state, direction));
    }

    @Override
    @Nullable
    public List<BakedQuad> appearance(List<BakedQuad> quads, @Nullable Direction side, BlockAndTintGetter level,
                                      BlockPos pos) {
        // 预览环境下不做成型换装
        return level instanceof MultiblockPreview ? null : CrystalWorkstationCtm.appearance(quads, side, level, pos);
    }

    @Override
    @Nullable
    public CtmBinding.Retarget retarget(BlockState state) {
        TextureAtlasSprite from = facadeBaseSprite(state);
        TextureAtlasSprite to = facadeSprite(state);
        return from == null || to == null ? null : new CtmBinding.Retarget(from, to);
    }

    /**
     * 以自定义连接判定计算掩码，用于跨方块族的成型外观。
     *
     * @param joined 判定某个邻居位置是否与本方块相连
     */
    public static int mask(BlockAndTintGetter level, BlockPos pos, BiPredicate<BlockPos, BlockState> joined) {
        return CtmMask.compute(level, pos,
                               (direction, neighbor, neighborState) -> joined.test(neighbor, neighborState));
    }

    public static boolean connects(BlockState neighbor, BlockState self) {
        int key = familyKey(self);
        return key != 0 && key == familyKey(neighbor);
    }

    // 邻居是否可沿 {@code dir} 方向连接，管道还需轴向一致
    public static boolean connects(BlockState neighbor, BlockState self, Direction dir) {
        if (!connects(neighbor, self)) {
            return false;
        }
        if (self.getBlock() == ModBlocks.METEOR_STEEL_PIPE_BLOCK.get()) {
            return dir.getAxis() == self.getValue(MeteorSteelPipeBlock.FACING).getAxis();
        }
        return true;
    }

    // 连接族标识：同一标识之间才连接
    private static int familyKey(BlockState state) {
        Block block = state.getBlock();
        if (block == ModBlocks.CRYSTAL_REINFORCED_COMPOSITE_MACHINE_CASING.get()) {
            return 1;
        }
        if (block == ModBlocks.CERTUS_QUARTZ_CRYSTAL_INPUT_BUS.get() ||
            block == ModBlocks.CERTUS_QUARTZ_CRYSTAL_INPUT_HATCH.get() ||
            block == ModBlocks.CERTUS_QUARTZ_CRYSTAL_OUTPUT_BUS.get() ||
            block == ModBlocks.CERTUS_QUARTZ_CRYSTAL_OUTPUT_HATCH.get()) {

            return partFamily(state, 1);
        }
        if (block == ModBlocks.ME_NETWORK_ACCESS_HATCH.get()) {
            // 接入仓仅成型后加入结构机壳的连接族
            return partFamily(state, 0);
        }
        if (block == ModBlocks.AEV_MACHINE_CASING.get()) {
            return 9;
        }
        if (block == ModBlocks.CRYSTAL_ENERGY_CASING.get()) {
            return 10;
        }
        if (MachinePartBlock.isAevPart(block)) {

            return partFamily(state, 9);
        }
        if (block == ModBlocks.CRYSTAL_GLASS.get()) {
            return 3;
        }
        if (block == ModBlocks.FIREPROOF_CRYSTAL_GLASS.get()) {
            return 8;
        }
        if (block instanceof MachineBlock<?>) {
            MachinePartFacade facade = facade(state);
            return facade != null ? facade.ctmFamily() : 0;
        }
        if (block == ModBlocks.ZIRCONIA_CORUNDUM_BRICKS.get()) {
            return 4;
        }
        if (block == ModBlocks.ZIRCONIUM_IRON_ALLOY_MACHINE_CASING.get()) {
            return 11;
        }
        if (block == ModBlocks.ME_COMPUTER_CASING.get()) {
            return 12;
        }
        if (block == ModBlocks.ME_ADVANCED_COMPUTER_CASING.get()) {
            return 13;
        }
        if (block == ModBlocks.METEOR_STEEL_PIPE_BLOCK.get()) {
            // 管道按轴向分族
            return 5 + state.getValue(MeteorSteelPipeBlock.FACING).getAxis().ordinal();
        }
        return 0;
    }

    private static int partFamily(BlockState state, int casingFamily) {
        MachinePartFacade facade = facade(state);
        return facade != null ? facade.ctmFamily() : casingFamily;
    }

    @Nullable
    public static MachinePartFacade facade(BlockState state) {
        if (state.is(ModMachines.JIG.block()) && state.getValue(MachineBlock.FORMED)) {
            return MachinePartFacade.ZIRCONIUM_IRON;
        }
        if (state.is(ModMachines.FILTER_PRESS.block()) && state.getValue(MachineBlock.FORMED)) {
            return MachinePartFacade.ZIRCONIUM_IRON;
        }
        if ((state.is(ModMachines.CRYSTAL_WORKSTATION.block()) || state.is(ModMachines.MULTI_FACTORY.block())) &&
            state.getValue(MachineBlock.FORMED)) {
            return MachinePartFacade.CRYSTAL_ENERGY;
        }
        if (state.is(ModMachines.CRYSTAL_ASSEMBLY_LINE.block()) && state.getValue(MachineBlock.FORMED)) {
            return MachinePartFacade.CRYSTAL;
        }
        if (state.is(ModMachines.HIGH_REACTIVITY_LAVA_SMELTER.block()) && state.getValue(MachineBlock.FORMED)) {
            return MachinePartFacade.ZIRCONIA;
        }
        if (state.is(ModMachines.MATTER_PHASE_TRANSFORMER.block()) && state.getValue(MachineBlock.FORMED)) {
            return MachinePartFacade.ZIRCONIA;
        }
        if (state.is(ModMachines.CRYSTAL_REACTION_CHAMBER.block()) && state.getValue(MachineBlock.FORMED)) {
            return MachinePartFacade.CRYSTAL;
        }
        if (state.getBlock() instanceof MachinePartBlock && state.getValue(MachinePartBlock.FORMED)) {
            return MachinePartFacade.byStyle(state.getValue(MachinePartBlock.STYLE));
        }
        if (state.is(ModBlocks.ME_NETWORK_ACCESS_HATCH.get()) && state.getValue(MENetworkAccessHatchBlock.FORMED)) {
            return MachinePartFacade.byStyle(state.getValue(MENetworkAccessHatchBlock.STYLE));
        }
        return null;
    }

    @Nullable
    private static TextureAtlasSprite facadeSprite(BlockState state) {
        MachinePartFacade facade = facade(state);
        return facade != null ? CtmTextures.baseSprite(facade.texture()) : null;
    }

    @Nullable
    private static TextureAtlasSprite facadeBaseSprite(BlockState state) {
        Block block = state.getBlock();
        if (block == ModMachines.MULTI_FACTORY.block()) {
            return CtmTextures.baseSprite(MULTI_FACTORY_CONTROLLER_CASING);
        }
        if (block == ModMachines.JIG.block()) {
            return CtmTextures.baseSprite(JIG_CONTROLLER_CASING);
        }
        if (block == ModMachines.FILTER_PRESS.block()) {
            return CtmTextures.baseSprite(FILTER_PRESS_CONTROLLER_CASING);
        }
        if (block == ModMachines.CRYSTAL_WORKSTATION.block()) {
            return CtmTextures.baseSprite(CRYSTAL_WORKSTATION_CONTROLLER_CASING);
        }
        if (block == ModMachines.CRYSTAL_ASSEMBLY_LINE.block()) {
            return CtmTextures.baseSprite(CRYSTAL_ASSEMBLY_LINE_CONTROLLER_CASING);
        }
        if (block == ModMachines.HIGH_REACTIVITY_LAVA_SMELTER.block()) {
            return CtmTextures.baseSprite(HIGH_REACTIVITY_LAVA_SMELTER_CONTROLLER_CASING);
        }
        if (block == ModMachines.MATTER_PHASE_TRANSFORMER.block()) {
            return CtmTextures.baseSprite(MATTER_PHASE_TRANSFORMER_CONTROLLER_CASING);
        }
        if (block == ModMachines.CRYSTAL_REACTION_CHAMBER.block()) {
            return CtmTextures.baseSprite(CRYSTAL_REACTION_CONTROLLER_CASING);
        }
        if (block == ModBlocks.ME_NETWORK_ACCESS_HATCH.get()) {
            // 接入仓成型外壳以水晶强化复合机壳为基础贴图
            return CtmTextures.baseSprite(MachinePartFacade.CRYSTAL.texture());
        }
        if (!(block instanceof MachinePartBlock)) {
            return null;
        }
        return MachinePartBlock.isAevPart(block) ? CtmTextures.baseSprite(AEV_PART_CASING)
                                                 : CtmTextures.baseSprite(MachinePartFacade.CRYSTAL.texture());
    }
}
