package com.murthinext.ae2pr.requester.crafting;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import javax.annotation.Nullable;

import net.minecraft.world.level.Level;

import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.CalculationStrategy;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.crafting.ICraftingSimulationRequester;
import appeng.api.networking.security.IActionSource;

import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.requester.Requests;

public final class RequestCrafting {

    private final IActionSource actionSource;
    private final ICraftingSimulationRequester simulationRequester;
    private final List<Future<ICraftingPlan>> pending = new ArrayList<>();

    public RequestCrafting(IActionSource actionSource) {
        this.actionSource = actionSource;
        simulationRequester = () -> actionSource;
    }

    public void submit(Level level, IGrid grid, Requests requests) {
        var service = grid.getCraftingService();
        for (var i = 0; i < requests.size(); i++) {
            var request = requests.get(i);
            if (!request.isRequesting() || request.getAmount() <= 0) {
                continue;
            }
            try {
                pending.add(service.beginCraftingCalculation(level, simulationRequester, request.getKey(),
                                                             request.getAmount(), CalculationStrategy.CRAFT_LESS));
            } catch (Exception exception) {
                ae2pr.LOGGER.warn("Failed to start crafting calculation for {}x{}", request.getAmount(),
                                  request.getKey(), exception);
            }
        }
    }

    public void submitCompleted(@Nullable IGrid grid) {
        if (pending.isEmpty()) {
            return;
        }
        if (grid == null) {
            pending.clear();
            return;
        }
        var service = grid.getCraftingService();
        var iterator = pending.iterator();
        while (iterator.hasNext()) {
            var future = iterator.next();
            if (!future.isDone()) {
                continue;
            }
            iterator.remove();
            try {
                var plan = future.get();
                if (plan != null && !plan.simulation()) {
                    service.submitJob(plan, null, null, true, actionSource);
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                ae2pr.LOGGER.warn("Interrupted while retrieving redstone requester crafting plan", exception);
                return;
            } catch (Exception exception) {
                ae2pr.LOGGER.warn("Failed to submit redstone requester crafting job", exception);
            }
        }
    }

    public boolean hasPendingJobs() {
        return !pending.isEmpty();
    }
}
