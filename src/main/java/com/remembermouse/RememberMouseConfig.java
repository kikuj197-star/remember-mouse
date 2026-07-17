package com.remembermouse;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class RememberMouseConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH =
        FabricLoader.getInstance().getConfigDir().resolve("remember-mouse").resolve("remember-mouse.json");

    /** Master toggle — when false, cursor positions are neither saved nor restored. */
    public boolean enabled = true;

    /**
     * How long a saved cursor position stays valid, in seconds.
     * 0 = permanent (positions never expire). Must be ≥ 0.
     */
    public int memoryWindowSeconds = 0;

    public static RememberMouseConfig load() {
        RememberMouseConfig config = new RememberMouseConfig();
        if (Files.exists(CONFIG_PATH)) {
            try {
                String json = Files.readString(CONFIG_PATH);
                RememberMouseConfig loaded = GSON.fromJson(json, RememberMouseConfig.class);
                if (loaded != null) {
                    config = loaded;
                    // Validate: memoryWindowSeconds cannot be negative
                    if (config.memoryWindowSeconds < 0) {
                        config.memoryWindowSeconds = 0;
                    }
                    RememberMouse.LOGGER.info("[RememberMouse] Config loaded from {}", CONFIG_PATH);
                }
            } catch (IOException e) {
                RememberMouse.LOGGER.warn("[RememberMouse] Failed to read config, using defaults: {}", e.getMessage());
            }
        } else {
            config.save();
        }
        return config;
    }

    public void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(this));
            RememberMouse.LOGGER.info("[RememberMouse] Config saved to {}", CONFIG_PATH);
        } catch (IOException e) {
            RememberMouse.LOGGER.warn("[RememberMouse] Failed to save config: {}", e.getMessage());
        }
    }
}
