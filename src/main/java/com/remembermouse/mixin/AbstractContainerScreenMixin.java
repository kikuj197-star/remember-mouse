package com.remembermouse.mixin;

import com.remembermouse.RememberMouse;
import com.remembermouse.SkyBlockUiDetector;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {
    @ModifyArgs(
        method = "slotClicked(Lnet/minecraft/world/inventory/Slot;IILnet/minecraft/world/inventory/ContainerInput;)V",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;handleContainerInput(IIILnet/minecraft/world/inventory/ContainerInput;Lnet/minecraft/world/entity/player/Player;)V"
        )
    )
    private void rememberMouse$replaceUiClickWithMiddleClick(Args args) {
        if (RememberMouse.config == null || !RememberMouse.config.enabled) return;

        int slotId = args.get(1);
        int mouseButton = args.get(2);
        ContainerInput input = args.get(3);
        // Match Skytils' safe scope: replace ordinary left clicks only. Right
        // clicks can have a distinct action (for example changing Storage icons).
        if (slotId < 0 || mouseButton != 0 || input != ContainerInput.PICKUP) return;

        AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) (Object) this;
        if (slotId >= screen.getMenu().slots.size()) return;
        Slot slot = screen.getMenu().getSlot(slotId);
        if (!SkyBlockUiDetector.isUiSlot(screen, slot)) return;

        // Match a real middle click: button 2 with CLONE. In survival this does
        // not pick the display stack up locally, while Hypixel still receives
        // the click and can execute the GUI action.
        args.set(2, 2);
        args.set(3, ContainerInput.CLONE);
    }
}
