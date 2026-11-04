package com.murthinext.ae2pr.integration.jei.plugin;

import net.minecraft.resources.ResourceLocation;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;

import com.murthinext.ae2pr.api.Ids;
import com.murthinext.ae2pr.api.machine.client.AbstractMachineScreen;
import com.murthinext.ae2pr.integration.jei.gui.MachineGuiHandler;

@JeiPlugin
public class MachineGuiPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return Ids.of("machine_gui");
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGenericGuiContainerHandler(AbstractMachineScreen.class, new MachineGuiHandler());
    }
}
