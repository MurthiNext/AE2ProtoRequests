package com.murthinext.ae2pr.client.tooltip;

import com.murthinext.ae2pr.api.math.LatexFormula;
import net.minecraft.world.inventory.tooltip.TooltipComponent;

public record FormulaTooltip(LatexFormula formula, int maxWidth) implements TooltipComponent {}
