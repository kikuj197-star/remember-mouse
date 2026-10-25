package com.remembermouse.mixin;

import com.remembermouse.RememberMouse;
import com.remembermouse.RememberMouseConfig;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Primary restore: sets cursor at the end of Screen.init(), before the first frame.
 * Only triggers for new screen opens (not window resizes), gated by insideSetScreen.
 */
@Mixin(Screen.class)
public abstract class ScreenMixin {

    @Inject(method = "init()V", at = @At("TAIL"))
    private void afterInit(CallbackInfo ci) {
        RememberMouseConfig cfg = RememberMouse.config;
        if (cfg == null || !cfg.enabled) {
            return;
        }

        if (!RememberMouse.insideSetScreen) {
            return; // window resize, not a new screen open
        }

        Screen self = (Screen) (Object) this;
        if (!(self instanceof AbstractContainerScreen)) {
            return;
        }

        double[] saved = RememberMouse.SAVED_POSITIONS.get("universal_container_cursor");
        if (saved == null || !RememberMouse.isWithinWindow(saved)) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();

        int maxX = mc.getWindow().getWidth() - 1;
        int maxY = mc.getWindow().getHeight() - 1;
        double clampedX = Math.max(0, Math.min(saved[0], maxX));
        double clampedY = Math.max(0, Math.min(saved[1], maxY));

        InputConstants.releaseMouse(mc.getWindow(), clampedX, clampedY);
    }
}
