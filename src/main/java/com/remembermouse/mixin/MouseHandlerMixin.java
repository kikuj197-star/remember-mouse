package com.remembermouse.mixin;

import com.remembermouse.RememberMouse;
import com.remembermouse.RememberMouseConfig;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import net.minecraft.client.MouseHandler;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {

    @Inject(method = "releaseMouse", at = @At("RETURN"))
    private void afterReleaseMouse(CallbackInfo ci) {
        RememberMouseConfig cfg = RememberMouse.config;
        if (!cfg.enabled) {
            RememberMouse.pendingX = Double.NaN;
            return;
        }

        if (!Double.isNaN(RememberMouse.pendingX)) {
            GLFW.glfwSetCursorPos(
                RememberMouse.pendingWindow,
                RememberMouse.pendingX,
                RememberMouse.pendingY
            );
            RememberMouse.pendingX = Double.NaN;
        }
    }
}
