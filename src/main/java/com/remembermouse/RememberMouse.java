package com.remembermouse;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RememberMouse implements ClientModInitializer {
    public static final String MOD_ID = "remember-mouse";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /** Single global saved cursor position — all container types share one position. */
    public static final Map<String, double[]> SAVED_POSITIONS = new ConcurrentHashMap<>();

    /** True while inside Minecraft.setScreen() — gates Screen.init() restore vs resize. */
    public static boolean insideSetScreen;

    /** Pending cursor target for MouseHandler releaseMouse RETURN fallback. */
    public static double pendingX = Double.NaN;
    public static double pendingY = Double.NaN;
    public static long pendingWindow;

    @Override
    public void onInitializeClient() {
        LOGGER.info("[RememberMouse] Ready — global cursor position remembered across containers.");
    }
}
