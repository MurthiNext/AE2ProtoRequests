package com.murthinext.ae2pr.core.datagen;

import com.murthinext.ae2pr.api.Ids;
import com.murthinext.ae2pr.api.machine.MachineBlock;
import com.murthinext.ae2pr.block.casing.ComputerCasingBlock;
import com.murthinext.ae2pr.block.feature.HighEnergyCoreBlock;
import com.murthinext.ae2pr.block.feature.MeteorSteelPipeBlock;
import com.murthinext.ae2pr.block.feature.filter_press.FilterPressPlateBlock;
import com.murthinext.ae2pr.block.machine_part.MENetworkAccessHatchBlock;
import com.murthinext.ae2pr.block.machine_part.MEPatternBufferBlock;
import com.murthinext.ae2pr.block.machine_part.MachinePartBlock;
import com.murthinext.ae2pr.core.registry.ModBlocks;
import com.murthinext.ae2pr.machine.crystal_assembly_line.CrystalAssemblyLineUnitBlock;
import com.murthinext.ae2pr.machine.naming_factory.NamingFactoryBlock;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ConfiguredModel;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.client.model.generators.VariantBlockStateBuilder;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.RegistryObject;

/**
 * 方块状态：只做“状态属性 → 现有模型文件”的映射，不生成任何模型。
 * filter_press_plate 的连接变体仍由手写文件负责。
 */
public final class ModBlockStateProvider extends BlockStateProvider {

    private static final Direction[] HORIZONTAL = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

    private static final boolean[] BOOLEANS = {false, true};

    public ModBlockStateProvider(PackOutput output, String modId, ExistingFileHelper existingFileHelper) {
        super(output, modId, existingFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        for (RegistryObject<Block> entry : ModBlocks.BLOCKS.getEntries()) {
            Block block = entry.get();
            String path = entry.getId().getPath();
            if (block instanceof FilterPressPlateBlock) {
                continue;
            }
            if (block instanceof MachineBlock<?>) {
                machine(block, path);
            } else if (block instanceof MachinePartBlock) {
                facing6(block, existing("block/" + path));
            } else if (block instanceof MENetworkAccessHatchBlock) {
                formed6(
                        block,
                        existing("block/" + path + "/" + path),
                        existing("block/" + path + "/" + path + "_formed"));
            } else if (block instanceof MEPatternBufferBlock) {
                formed6(block, existing("block/" + path), existing("block/" + path + "_formed"));
            } else if (block instanceof ComputerCasingBlock) {
                horizontal4(block, existing("block/" + path), true);
            } else if (block instanceof NamingFactoryBlock) {
                horizontal4(block, existing("block/naming_factory/naming_factory_base"), false);
            } else if (block instanceof MeteorSteelPipeBlock) {
                pipe(block, path);
            } else if (block instanceof CrystalAssemblyLineUnitBlock) {
                active(block, path);
            } else if (block instanceof HighEnergyCoreBlock) {
                lit(block, existing("block/" + path));
            } else {
                simpleBlock(block, existing(modelPath(path)));
            }
        }
    }

    private void machine(Block block, String path) {
        boolean flat = path.equals("integrated_mass_fabricator");
        String base = flat ? "block/" + path : "block/" + path + "/" + path;
        ModelFile idle = existing(base);
        ModelFile active = existing(base + "_active");
        ModelFile paused = existing(base + "_paused");
        VariantBlockStateBuilder builder = getVariantBuilder(block);
        for (Direction facing : HORIZONTAL) {
            for (boolean running : BOOLEANS) {
                // 运行中不区分 paused，键里不写 paused 属性
                if (flat && running) {
                    builder.partialState()
                            .with(MachineBlock.FACING, facing)
                            .with(MachineBlock.RUNNING, true)
                            .setModels(rotated(active, facing));
                    continue;
                }
                for (boolean isPaused : BOOLEANS) {
                    ModelFile model = running ? active : isPaused ? paused : idle;
                    builder.partialState()
                            .with(MachineBlock.FACING, facing)
                            .with(MachineBlock.RUNNING, running)
                            .with(MachineBlock.PAUSED, isPaused)
                            .setModels(rotated(model, facing));
                }
            }
        }
    }

    private void facing6(Block block, ModelFile model) {
        VariantBlockStateBuilder builder = getVariantBuilder(block);
        for (Direction facing : Direction.values()) {
            builder.partialState().with(MachinePartBlock.FACING, facing).setModels(rotated(model, facing));
        }
    }

    private void formed6(Block block, ModelFile plain, ModelFile formed) {
        VariantBlockStateBuilder builder = getVariantBuilder(block);
        for (Direction facing : Direction.values()) {
            for (boolean isFormed : BOOLEANS) {
                builder.partialState()
                        .with(MachinePartBlock.FACING, facing)
                        .with(MachinePartBlock.FORMED, isFormed)
                        .setModels(rotated(isFormed ? formed : plain, facing));
            }
        }
    }

    private void horizontal4(Block block, ModelFile model, boolean uvLock) {
        VariantBlockStateBuilder builder = getVariantBuilder(block);
        for (Direction facing : HORIZONTAL) {
            ConfiguredModel.Builder<?> entry =
                    ConfiguredModel.builder().modelFile(model).uvLock(uvLock);
            switch (facing) {
                case EAST -> entry.rotationY(90);
                case SOUTH -> entry.rotationY(180);
                case WEST -> entry.rotationY(270);
                default -> {}
            }
            builder.partialState()
                    .with(BlockStateProperties.HORIZONTAL_FACING, facing)
                    .setModels(entry.build());
        }
    }

    private void pipe(Block block, String path) {
        String base = "block/" + path + "/" + path + "_axis_";
        ModelFile x = existing(base + "x");
        ModelFile y = existing(base + "y");
        ModelFile z = existing(base + "z");
        VariantBlockStateBuilder builder = getVariantBuilder(block);
        for (Direction facing : Direction.values()) {
            ModelFile model = switch (facing.getAxis()) {
                case X -> x;
                case Y -> y;
                case Z -> z;
            };
            builder.partialState().with(MeteorSteelPipeBlock.FACING, facing).setModels(new ConfiguredModel(model));
        }
    }

    private void active(Block block, String path) {
        String base = "block/crystal_assembly_line/" + path;
        ModelFile idle = existing(base);
        ModelFile active = existing(base + "_active");
        VariantBlockStateBuilder builder = getVariantBuilder(block);
        for (boolean isActive : BOOLEANS) {
            builder.partialState()
                    .with(CrystalAssemblyLineUnitBlock.ACTIVE, isActive)
                    .setModels(new ConfiguredModel(isActive ? active : idle));
        }
    }

    private void lit(Block block, ModelFile model) {
        VariantBlockStateBuilder builder = getVariantBuilder(block);
        for (boolean lit : BOOLEANS) {
            builder.partialState().with(HighEnergyCoreBlock.LIT, lit).setModels(new ConfiguredModel(model));
        }
    }

    private ModelFile existing(String path) {
        return models().getExistingFile(Ids.of(path));
    }

    private static String modelPath(String path) {
        return path.startsWith("crystal_assembly_line_") ? "block/crystal_assembly_line/" + path : "block/" + path;
    }

    private static ConfiguredModel[] rotated(ModelFile model, Direction facing) {
        ConfiguredModel.Builder<?> builder = ConfiguredModel.builder().modelFile(model);
        switch (facing) {
            case EAST -> builder.rotationY(90).uvLock(true);
            case SOUTH -> builder.rotationY(180).uvLock(true);
            case WEST -> builder.rotationY(270).uvLock(true);
            case UP -> builder.rotationX(270).uvLock(true);
            case DOWN -> builder.rotationX(90).uvLock(true);
            default -> {}
        }
        return builder.build();
    }
}
