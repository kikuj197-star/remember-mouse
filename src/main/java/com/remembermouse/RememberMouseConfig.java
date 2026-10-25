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
        FabricLoader.getInstance().getConfigDir().resolve("menuflow").resolve("menuflow.json");
    private static final Path LEGACY_CONFIG_PATH =
        FabricLoader.getInstance().getConfigDir().resolve("remember-mouse").resolve("remember-mouse.json");

    /** Master toggle for cursor memory and safe SkyBlock UI click replacement. */
    public boolean enabled = true;

    /**
     * How long a saved cursor position stays valid, in seconds.
     * 0 = permanent (positions never expire). Must be ≥ 0.
     */
    public int memoryWindowSeconds = 0;

    public static RememberMouseConfig load() {
        RememberMouseConfig config = new RememberMouseConfig();
        Path loadPath = Files.exists(CONFIG_PATH) ? CONFIG_PATH : LEGACY_CONFIG_PATH;
        if (Files.exists(loadPath)) {
            try {
                String json = Files.readString(loadPath);
                RememberMouseConfig loaded = GSON.fromJson(json, RememberMouseConfig.class);
                if (loaded != null) {
                    config = loaded;
                    // Validate: memoryWindowSeconds cannot be negative
                    if (config.memoryWindowSeconds < 0) {
                        config.memoryWindowSeconds = 0;
                    }
                    RememberMouse.LOGGER.info("[MenuFlow] Config loaded from {}", loadPath);
                    if (loadPath.equals(LEGACY_CONFIG_PATH)) {
                        config.save();
                        RememberMouse.LOGGER.info("[MenuFlow] Legacy Remember Mouse config migrated to {}", CONFIG_PATH);
                    }
                }
            } catch (IOException e) {
                RememberMouse.LOGGER.warn("[MenuFlow] Failed to read config, using defaults: {}", e.getMessage());
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
            RememberMouse.LOGGER.info("[MenuFlow] Config saved to {}", CONFIG_PATH);
        } catch (IOException e) {
            RememberMouse.LOGGER.warn("[MenuFlow] Failed to save config: {}", e.getMessage());
        }
    }
}
