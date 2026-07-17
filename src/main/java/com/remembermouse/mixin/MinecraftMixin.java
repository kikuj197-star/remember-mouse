package com.remembermouse.mixin;

import com.remembermouse.RememberMouse;
import com.remembermouse.RememberMouseConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {

    @Inject(method = "setScreen", at = @At("HEAD"))
    private void onBeforeSetScreen(Screen newScreen, CallbackInfo ci) {
        RememberMouseConfig cfg = RememberMouse.config;
        RememberMouse.insideSetScreen = true;
        Minecraft self = (Minecraft) (Object) this;
        Screen current = self.screen;
        long window = self.getWindow().handle();

        // 1) Save cursor when any container screen is being closed — single global key
        if (cfg.enabled && current instanceof AbstractContainerScreen) {
            double[] x = new double[1];
            double[] y = new double[1];
            GLFW.glfwGetCursorPos(window, x, y);
            RememberMouse.SAVED_POSITIONS.put("universal_container_cursor",
                new double[]{x[0], y[0], (double) System.currentTimeMillis()});
        }

        // 2) Pre-compute pending target for MouseHandler
        if (cfg.enabled && newScreen instanceof AbstractContainerScreen) {
            double[] saved = RememberMouse.SAVED_POSITIONS.get("universal_container_cursor");
            if (saved != null && RememberMouse.isWithinWindow(saved)) {
                int maxX = self.getWindow().getWidth() - 1;
                int maxY = self.getWindow().getHeight() - 1;
                RememberMouse.pendingX = Math.max(0, Math.min(saved[0], maxX));
                RememberMouse.pendingY = Math.max(0, Math.min(saved[1], maxY));
                RememberMouse.pendingWindow = window;
            }
        }
    }

    @Inject(method = "setScreen", at = @At("RETURN"))
    private void onAfterSetScreen(Screen newScreen, CallbackInfo ci) {
        RememberMouse.insideSetScreen = false;
    }
}
