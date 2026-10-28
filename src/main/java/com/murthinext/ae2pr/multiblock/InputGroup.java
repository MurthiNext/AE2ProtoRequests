package com.murthinext.ae2pr.multiblock;

import java.util.ArrayList;
import java.util.List;

import com.murthinext.ae2pr.block.machine_part.ItemBusBlockEntity;
import com.murthinext.ae2pr.block.machine_part.MachinePartFluidStorage;

// 参与同一次配方匹配的输入部件集合
public final class InputGroup {

    private final List<ItemBusBlockEntity> buses = new ArrayList<>();
    private final List<MachinePartFluidStorage> hatches = new ArrayList<>();

    public List<ItemBusBlockEntity> buses() {
        return buses;
    }

    public List<MachinePartFluidStorage> hatches() {
        return hatches;
    }

    public void addBus(ItemBusBlockEntity bus) {
        buses.add(bus);
    }

    public void addHatch(MachinePartFluidStorage hatch) {
        hatches.add(hatch);
    }

    public PartInventory inventory() {
        return new PartInventory(buses, hatches);
    }
}
