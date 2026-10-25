package com.remembermouse.mixin;

import com.remembermouse.RememberMouse;
import com.remembermouse.RememberMouseConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class MinecraftMixin {

    @Inject(method = "setScreen", at = @At("HEAD"))
    private void onBeforeSetScreen(Screen newScreen, CallbackInfo ci) {
        RememberMouse.insideSetScreen = true;

        RememberMouseConfig cfg = RememberMouse.config;
        if (cfg == null || !cfg.enabled) return;

        Minecraft minecraft = Minecraft.getInstance();
        Screen current = ((Gui) (Object) this).screen();

        // MouseHandler owns the SDL-backed cursor coordinates in 26.3.
        if (current instanceof AbstractContainerScreen) {
            RememberMouse.SAVED_POSITIONS.put("universal_container_cursor",
                new double[]{
                    minecraft.mouseHandler.xpos(),
                    minecraft.mouseHandler.ypos(),
                    (double) System.currentTimeMillis()
                });
        }

        // Pre-compute the fallback target before Gui initializes the new screen.
        if (newScreen instanceof AbstractContainerScreen) {
            double[] saved = RememberMouse.SAVED_POSITIONS.get("universal_container_cursor");
            if (saved != null && RememberMouse.isWithinWindow(saved)) {
                int maxX = minecraft.getWindow().getWidth() - 1;
                int maxY = minecraft.getWindow().getHeight() - 1;
                RememberMouse.pendingX = Math.max(0, Math.min(saved[0], maxX));
                RememberMouse.pendingY = Math.max(0, Math.min(saved[1], maxY));
            }
        }
    }

    @Inject(method = "setScreen", at = @At("RETURN"))
    private void onAfterSetScreen(Screen newScreen, CallbackInfo ci) {
        RememberMouse.insideSetScreen = false;
    }
}
