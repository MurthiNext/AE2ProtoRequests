package com.murthinext.ae2pr.api.machine.part;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandlerModifiable;

import com.murthinext.ae2pr.api.machine.ControllerHost;

public interface MachinePart {

    BlockPos getPartPos();

    @Nullable
    ItemPart items();

    @Nullable
    FluidPart fluids();

    @Nullable
    NetworkPart network();

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

        int MAX_CHANNELS = 8;

        void updateController(ControllerHost controller);

        void detachController(ControllerHost controller);

        int getAllocatedChannels();

        boolean isNetworkConnected(ControllerHost controller);

        double extractAEPower(double amount, boolean simulate);

        double getAvailableAEPower();

        boolean isHatchRemoved();
    }
}
