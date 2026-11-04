package com.murthinext.ae2pr.api.material;

public record FluidSpec(String id, boolean lavaLike, int lightLevel, int density, int viscosity, int temperature) {

    public static FluidSpec water(String id, int lightLevel, int density, int viscosity, int temperature) {
        return new FluidSpec(id, false, lightLevel, density, viscosity, temperature);
    }

    public static FluidSpec lava(String id, int lightLevel, int density, int viscosity, int temperature) {
        return new FluidSpec(id, true, lightLevel, density, viscosity, temperature);
    }
}
