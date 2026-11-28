package com.murthinext.ae2pr.api.machine.port;

import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;

public final class InputGroup {

    private final List<MachinePart.ItemPart> buses = new ArrayList<>();
    private final List<MachinePart.FluidPart> hatches = new ArrayList<>();

    @Nullable
    private final MachinePart.BufferedPattern pattern;

    public InputGroup() {
        this(null);
    }

    public InputGroup(@Nullable MachinePart.BufferedPattern pattern) {
        this.pattern = pattern;
    }

    @Nullable
    public MachinePart.BufferedPattern pattern() {
        return pattern;
    }

    public List<MachinePart.ItemPart> buses() {
        return buses;
    }

    public List<MachinePart.FluidPart> hatches() {
        return hatches;
    }

    public void addBus(MachinePart.ItemPart bus) {
        buses.add(bus);
    }

    public void addHatch(MachinePart.FluidPart hatch) {
        hatches.add(hatch);
    }

    public PartInventory inventory() {
        return new PartInventory(buses, hatches);
    }
}
