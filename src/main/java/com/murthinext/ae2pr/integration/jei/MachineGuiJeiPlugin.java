package com.murthinext.ae2pr.integration.jei;

import net.minecraft.resources.ResourceLocation;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;

import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.client.AbstractMachineScreen;
import com.murthinext.ae2pr.client.AbstractMachinePartScreen;

@JeiPlugin
public class MachineGuiJeiPlugin implements IModPlugin {

    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(ae2pr.MODID, "machine_gui");
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGenericGuiContainerHandler(AbstractMachineScreen.class, new MachineGuiHandler());
        registration.addGenericGuiContainerHandler(AbstractMachinePartScreen.class, new MachineGuiHandler());
    }
}
