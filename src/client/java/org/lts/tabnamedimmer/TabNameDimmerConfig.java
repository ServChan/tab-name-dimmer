package org.lts.tabnamedimmer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
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

    public boolean enabled = true;
    public boolean caseSensitive = false;
    public int dimColor = 0x555555;
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
        }

        return instance;
    }

    public static TabNameDimmerConfig loadIfChanged() {
        try {
            long modified = Files.exists(CONFIG_PATH) ? Files.getLastModifiedTime(CONFIG_PATH).toMillis() : -1L;
            if (modified != lastModified) {
                return load();
            }
        } catch (IOException exception) {
            TabNameDimmerClient.LOGGER.warn("Failed to check {}", CONFIG_PATH, exception);
        }

        return instance;
    }

    public static TabNameDimmerConfig currentCopy() {
        return instance.copy();
    }

    public static void save(TabNameDimmerConfig config) {
        TabNameDimmerConfig sanitized = sanitize(config);

        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(sanitized, writer);
            }
            instance = sanitized;
            lastModified = Files.getLastModifiedTime(CONFIG_PATH).toMillis();
        } catch (IOException exception) {
            TabNameDimmerClient.LOGGER.warn("Failed to save {}", CONFIG_PATH, exception);
        }
    }

    public boolean shouldDim(String playerName) {
        if (!enabled || playerName == null || playerName.isBlank()) {
            return false;
        }

        String checkedName = normalize(playerName);
        for (String allowedName : allowedNames) {
            if (checkedName.equals(normalize(allowedName))) {
                return false;
            }
        }

        return true;
    }

    public String allowedNamesText() {
        return String.join(", ", allowedNames);
    }

    public void setAllowedNamesFromText(String text) {
        allowedNames = parseNames(text);
    }

    private String normalize(String name) {
        String trimmed = name == null ? "" : name.trim();
        return caseSensitive ? trimmed : trimmed.toLowerCase(Locale.ROOT);
    }

    private static void ensureConfigExists() {
        if (Files.exists(CONFIG_PATH)) {
            return;
        }

        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(defaults(), writer);
            }
        } catch (IOException exception) {
            TabNameDimmerClient.LOGGER.warn("Failed to create {}", CONFIG_PATH, exception);
        }
    }

    private static TabNameDimmerConfig defaults() {
        return new TabNameDimmerConfig();
    }

    private TabNameDimmerConfig copy() {
        TabNameDimmerConfig copy = new TabNameDimmerConfig();
        copy.enabled = enabled;
        copy.caseSensitive = caseSensitive;
        copy.dimColor = dimColor;
        copy.allowedNames = new ArrayList<>(allowedNames);
        return copy;
    }

    private static TabNameDimmerConfig sanitize(TabNameDimmerConfig config) {
        if (config.allowedNames == null) {
            config.allowedNames = new ArrayList<>();
        }
        config.allowedNames = parseNames(String.join(",", config.allowedNames));
        config.dimColor = config.dimColor & 0xFFFFFF;
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
