package com.murthinext.ae2pr.client.tooltip;

import net.minecraft.world.inventory.tooltip.TooltipComponent;

import com.murthinext.ae2pr.api.math.LatexFormula;

public record FormulaTooltip(LatexFormula formula, int maxWidth) implements TooltipComponent {}
