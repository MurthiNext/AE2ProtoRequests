package com.murthinext.ae2pr.api.machine.port;

import appeng.api.stacks.AEKey;
import com.murthinext.ae2pr.api.machine.ControllerHost;
import com.murthinext.ae2pr.api.recipe.MultiblockRecipe;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.jetbrains.annotations.Nullable;

public interface MachinePart {

    BlockPos getPartPos();

    @Nullable
    ItemPart items();

    @Nullable
    FluidPart fluids();

    @Nullable
    NetworkPart network();

    @Nullable
    default ChannelRequest channelRequest() {
        return null;
    }

    @Nullable
    default PatternPart patternBuffer() {
        return null;
    }

    boolean isRecipeIsolated();

    interface ItemPart {

        BlockPos getPartPos();

        IItemHandlerModifiable storage();

        int slotCount();

        boolean isOutput();

        boolean isRecipeIsolated();
    }

    interface FluidPart {

        BlockPos getPartPos();

        int tankCount();

        FluidTank tank(int index);

        boolean isOutput();

        boolean isRecipeIsolated();

        void markChanged();
    }

    interface NetworkPart {

        void updateController(ControllerHost controller);

        void detachController(ControllerHost controller);

        ChannelStatus getChannelStatus(ControllerHost controller);

        boolean isNetworkConnected(ControllerHost controller);

        double extractAEPower(double amount, boolean simulate);

        double getAvailableAEPower();

        long insertProduct(AEKey key, long amount);

        boolean isHatchRemoved();
    }

    interface PatternPart {

        List<BufferedPattern> bufferedPatterns(ControllerHost host);
    }

    interface BufferedPattern {

        boolean matchesOutputs(MultiblockRecipe recipe);

        boolean consume(MultiblockRecipe recipe, int parallel, boolean simulate);
    }
}
