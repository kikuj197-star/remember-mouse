package com.remembermouse;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RememberMouse implements ClientModInitializer {
    public static final String MOD_ID = "remember-mouse";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /** Loaded config — accessible from all mixins. */
    public static RememberMouseConfig config;

    /**
     * Single global saved cursor position.
     * Value is double[3]: {x, y, timestampMs}.
     * timestampMs is {@code System.currentTimeMillis()} at save time,
     * used to enforce {@link RememberMouseConfig#memoryWindowSeconds}.
     */
    public static final Map<String, double[]> SAVED_POSITIONS = new ConcurrentHashMap<>();

    /** True while inside Minecraft.setScreen() — gates Screen.init() restore vs resize. */
    public static boolean insideSetScreen;

    /** Pending cursor target for MouseHandler releaseMouse RETURN fallback. */
    public static double pendingX = Double.NaN;
    public static double pendingY = Double.NaN;
    public static long pendingWindow;

    @Override
    public void onInitializeClient() {
        config = RememberMouseConfig.load();
        LOGGER.info("[RememberMouse] Ready — global cursor position remembered across containers.");
    }

    /**
     * Returns true if a saved position is still within the configured memory window.
     * Always returns true when memoryWindowSeconds is 0 (permanent).
     */
    public static boolean isWithinWindow(double[] saved) {
        if (config == null || config.memoryWindowSeconds == 0) return true;
        if (saved.length < 3) return true; // legacy data without timestamp
        long savedAt = (long) saved[2];
        long elapsed = (System.currentTimeMillis() - savedAt) / 1000;
        return elapsed < config.memoryWindowSeconds;
    }
}
