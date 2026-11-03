package com.murthinext.ae2pr.api.machine.part;

import java.util.ArrayList;
import java.util.List;

public final class InputGroup {

    private final List<MachinePart.ItemPart> buses = new ArrayList<>();
    private final List<MachinePart.FluidPart> hatches = new ArrayList<>();

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
