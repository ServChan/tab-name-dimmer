package org.lts.tabnamedimmer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class TabNameDimmerConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("tab-name-dimmer.json");

    private static TabNameDimmerConfig instance = defaults();
    private static long lastModified = -1L;

public enum DisplayMode {
        ANIMATED_SORT, FILTER, EXTRA_HUD
    }

    public boolean enabled = true;
    public boolean caseSensitive = false;
    public boolean glowingEnabled = false;
    public int dimColor = 0x555555;
    public DisplayMode displayMode = DisplayMode.ANIMATED_SORT;
    public float animationSpeed = 0.05f;
    public List<String> allowedNames = new ArrayList<>();

    public static TabNameDimmerConfig load() {
        ensureConfigExists();

        try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
            TabNameDimmerConfig loaded = GSON.fromJson(reader, TabNameDimmerConfig.class);
            instance = sanitize(loaded == null ? defaults() : loaded);
            lastModified = Files.getLastModifiedTime(CONFIG_PATH).toMillis();
        } catch (IOException | RuntimeException exception) {
            TabNameDimmerClient.LOGGER.warn("Failed to load {}, using defaults", CONFIG_PATH, exception);
            instance = defaults();
            lastModified = currentModifiedTime();
        }

        return instance;
    }

    private static long nextCheckTime = 0;

    public static TabNameDimmerConfig loadIfChanged() {
        long now = System.currentTimeMillis();
        if (now > nextCheckTime) {
            nextCheckTime = now + 1000L;
            try {
                long modified = Files.exists(CONFIG_PATH) ? Files.getLastModifiedTime(CONFIG_PATH).toMillis() : -1L;
                if (modified != lastModified) {
                    return load();
                }
            } catch (IOException exception) {
                TabNameDimmerClient.LOGGER.warn("Failed to check {}", CONFIG_PATH, exception);
            }
        }
        return instance;
    }

    public static TabNameDimmerConfig currentCopy() {
        return instance.copy();
    }

    public static boolean save(TabNameDimmerConfig config) {
        TabNameDimmerConfig sanitized = sanitize(config);
        Path temporary = CONFIG_PATH.resolveSibling(CONFIG_PATH.getFileName() + ".tmp");
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(temporary)) {
                GSON.toJson(sanitized, writer);
            }
            try {
                Files.move(temporary, CONFIG_PATH, StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, CONFIG_PATH, StandardCopyOption.REPLACE_EXISTING);
            }
            instance = sanitized;
            lastModified = Files.getLastModifiedTime(CONFIG_PATH).toMillis();
            return true;
        } catch (IOException exception) {
            TabNameDimmerClient.LOGGER.warn("Failed to save {}", CONFIG_PATH, exception);
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException cleanupException) {
                TabNameDimmerClient.LOGGER.debug("Failed to clean temporary config {}", temporary, cleanupException);
            }
            return false;
        }
    }

    private transient Set<String> normalizedNamesCache = null;

    public boolean shouldDim(String playerName) {
        if (!enabled || playerName == null || playerName.isBlank()) {
            return false;
        }

        if (normalizedNamesCache == null) {
            normalizedNamesCache = new LinkedHashSet<>();
            for (String allowedName : allowedNames) {
                normalizedNamesCache.add(normalize(allowedName));
            }
        }

        return !normalizedNamesCache.contains(normalize(playerName));
    }

    public String allowedNamesText() {
        return String.join(", ", allowedNames);
    }

    public void setAllowedNamesFromText(String text) {
        allowedNames = parseNames(text);
        normalizedNamesCache = null;
    }

    private String normalize(String name) {
        String trimmed = name == null ? "" : name.trim();
        return caseSensitive ? trimmed : trimmed.toLowerCase(Locale.ROOT);
    }

    private static void ensureConfigExists() {
        if (Files.exists(CONFIG_PATH)) {
            return;
        }
        if (!save(defaults())) {
            TabNameDimmerClient.LOGGER.warn("Failed to create {}", CONFIG_PATH);
        }
    }

    private static TabNameDimmerConfig defaults() {
        return new TabNameDimmerConfig();
    }

    private static long currentModifiedTime() {
        try {
            return Files.exists(CONFIG_PATH) ? Files.getLastModifiedTime(CONFIG_PATH).toMillis() : -1L;
        } catch (IOException ignored) {
            return -1L;
        }
    }

    private TabNameDimmerConfig copy() {
        TabNameDimmerConfig copy = new TabNameDimmerConfig();
        copy.enabled = enabled;
        copy.caseSensitive = caseSensitive;
        copy.glowingEnabled = glowingEnabled;
        copy.dimColor = dimColor;
        copy.displayMode = displayMode;
        copy.animationSpeed = animationSpeed;
        copy.allowedNames = new ArrayList<>(allowedNames);
        return copy;
    }

    private static TabNameDimmerConfig sanitize(TabNameDimmerConfig config) {
        if (config.allowedNames == null) {
            config.allowedNames = new ArrayList<>();
        }
        List<String> nonNullNames = new ArrayList<>();
        for (String name : config.allowedNames) {
            if (name != null) {
                nonNullNames.add(name);
            }
        }
        config.allowedNames = parseNames(String.join(",", nonNullNames));
        config.dimColor = config.dimColor & 0xFFFFFF;
        if (config.displayMode == null) {
            config.displayMode = DisplayMode.ANIMATED_SORT;
        }
        if (!Float.isFinite(config.animationSpeed)) {
            config.animationSpeed = 0.05F;
        }
        config.animationSpeed = Math.max(0.001F, Math.min(1.0F, config.animationSpeed));
        config.normalizedNamesCache = null;
        return config;
    }

    private static List<String> parseNames(String text) {
        Set<String> names = new LinkedHashSet<>();
        if (text != null) {
            for (String rawName : text.split("[,;\\r\\n]+")) {
                String name = rawName.trim();
                if (!name.isBlank()) {
                    names.add(name);
                }
            }
        }
        return new ArrayList<>(names);
    }
}
