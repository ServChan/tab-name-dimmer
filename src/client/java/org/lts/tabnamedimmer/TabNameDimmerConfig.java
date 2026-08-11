package org.lts.tabnamedimmer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class TabNameDimmerConfig {
    public static final int CURRENT_SCHEMA_VERSION = 2;
    public static final String GLOBAL_SCOPE = "global";
    public static final long MAX_TRANSFER_BYTES = 1024L * 1024L;
    private static final int MAX_GROUPS = 32;
    private static final int MAX_MEMBERS_PER_GROUP = 4096;
    private static final int MAX_TOTAL_MEMBERS_PER_PROFILE = 8192;
    private static final int MAX_SERVER_PROFILES = 128;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static volatile TabNameDimmerConfig instance = defaults();
    private static long lastModified = -1L;
    private static long nextCheckTime;

    public enum DisplayMode {
        ANIMATED_SORT, FILTER, EXTRA_HUD
    }

    public enum ActivationMode {
        HOLD_SHIFT, HOLD_KEY, TOGGLE_KEY
    }

    public enum PlayerSortMode {
        ORIGINAL, NAME, PING
    }

    public enum HudAnchor {
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT
    }

    public int schemaVersion = CURRENT_SCHEMA_VERSION;
    public boolean enabled = true;
    public boolean caseSensitive;
    public boolean notificationsEnabled = true;
    public boolean playerTransparencyEnabled;
    public float dimOpacity = 0.3F;
    public int dimColor = 0x555555;
    public DisplayMode displayMode = DisplayMode.ANIMATED_SORT;
    public ActivationMode activationMode = ActivationMode.HOLD_SHIFT;
    public PlayerSortMode playerSortMode = PlayerSortMode.ORIGINAL;
    public float animationSpeed = 0.05F;
    public HudAnchor hudAnchor = HudAnchor.BOTTOM_CENTER;
    public int hudColumns = 1;
    public int hudMaxRows = 20;
    public boolean hudShowAvatars = true;
    public boolean hudShowPing = true;
    public Profile globalProfile = Profile.defaultProfile();
    public Map<String, Profile> serverProfiles = new LinkedHashMap<>();

    public static final class Profile {
        public String name = "Global";
        public List<PlayerGroup> groups = new ArrayList<>();

        public Profile() {
        }

        public Profile(String name) {
            this.name = name;
        }

        public static Profile defaultProfile() {
            Profile profile = new Profile("Global");
            profile.groups.add(PlayerGroup.defaultGroup());
            return profile;
        }

        public Profile copy() {
            Profile copy = new Profile(name);
            for (PlayerGroup group : groups) {
                copy.groups.add(group.copy());
            }
            return copy;
        }
    }

    public static final class PlayerGroup {
        public String name = "Default";
        public int color = 0x55FF55;
        public int priority;
        public boolean enabled = true;
        public boolean colorizeNames;
        public boolean glowingEnabled;
        public boolean transparencyEnabled;
        public List<String> members = new ArrayList<>();

        private transient Set<String> normalizedMembers;
        private transient boolean normalizedCaseSensitive;

        public static PlayerGroup defaultGroup() {
            return new PlayerGroup();
        }

        public PlayerGroup copy() {
            PlayerGroup copy = new PlayerGroup();
            copy.name = name;
            copy.color = color;
            copy.priority = priority;
            copy.enabled = enabled;
            copy.colorizeNames = colorizeNames;
            copy.glowingEnabled = glowingEnabled;
            copy.transparencyEnabled = transparencyEnabled;
            copy.members = new ArrayList<>(members);
            return copy;
        }

        private boolean contains(String playerName, boolean caseSensitive) {
            if (normalizedMembers == null || normalizedCaseSensitive != caseSensitive) {
                normalizedMembers = new LinkedHashSet<>();
                normalizedCaseSensitive = caseSensitive;
                for (String member : members) {
                    normalizedMembers.add(normalizeName(member, caseSensitive));
                }
            }
            return normalizedMembers.contains(normalizeName(playerName, caseSensitive));
        }

        private void invalidateCache() {
            normalizedMembers = null;
        }
    }

    public record Match(PlayerGroup group, int priority) {
    }

    private static final class LegacyConfig {
        boolean enabled = true;
        boolean caseSensitive;
        boolean glowingEnabled;
        boolean playerTransparencyEnabled;
        float dimOpacity = 0.3F;
        int dimColor = 0x555555;
        DisplayMode displayMode = DisplayMode.ANIMATED_SORT;
        float animationSpeed = 0.05F;
        List<String> allowedNames = new ArrayList<>();
    }

    private static final class ProfileTransfer {
        int schemaVersion = CURRENT_SCHEMA_VERSION;
        String type = "tabnamedimmer-profile";
        Profile profile;

        ProfileTransfer(Profile profile) {
            this.profile = profile;
        }
    }

    public static TabNameDimmerConfig load() {
        ensureConfigExists();
        Path configPath = configPath();
        try (Reader reader = Files.newBufferedReader(configPath)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            TabNameDimmerConfig loaded;
            if (root.has("schemaVersion") && root.get("schemaVersion").getAsInt() >= CURRENT_SCHEMA_VERSION) {
                loaded = GSON.fromJson(root, TabNameDimmerConfig.class);
            } else {
                loaded = migrateLegacy(GSON.fromJson(root, LegacyConfig.class));
            }
            instance = sanitize(loaded == null ? defaults() : loaded);
            lastModified = Files.getLastModifiedTime(configPath).toMillis();
        } catch (IOException | RuntimeException exception) {
            TabNameDimmerClient.LOGGER.warn("Failed to load the Tab Name Dimmer configuration; using recovery", exception);
            instance = loadBackup();
            lastModified = currentModifiedTime();
        }
        return instance;
    }

    public static void pollForChanges() {
        long now = System.currentTimeMillis();
        if (now <= nextCheckTime) {
            return;
        }
        nextCheckTime = now + 1000L;
        try {
            Path configPath = configPath();
            long modified = Files.exists(configPath) ? Files.getLastModifiedTime(configPath).toMillis() : -1L;
            if (modified != lastModified) {
                load();
            }
        } catch (IOException exception) {
            TabNameDimmerClient.LOGGER.warn("Failed to check the Tab Name Dimmer configuration timestamp", exception);
        }
    }

    public static TabNameDimmerConfig current() {
        return instance;
    }

    public static TabNameDimmerConfig currentCopy() {
        return instance.copy();
    }

    public static boolean save(TabNameDimmerConfig config) {
        TabNameDimmerConfig sanitized = sanitize(config);
        if (!writeJsonAtomically(configPath(), backupPath(), sanitized)) {
            return false;
        }
        instance = sanitized;
        lastModified = currentModifiedTime();
        return true;
    }

    public static boolean exportProfile(Profile profile, Path path) {
        Profile safeProfile = sanitizeProfile(profile == null ? Profile.defaultProfile() : profile.copy(), "Exported");
        Path backup = path.resolveSibling(path.getFileName() + ".bak");
        return writeJsonAtomically(path, backup, new ProfileTransfer(safeProfile));
    }

    public static Profile importProfile(Path path) {
        try {
            if (!Files.isRegularFile(path) || Files.size(path) > MAX_TRANSFER_BYTES) {
                return null;
            }
            try (Reader reader = Files.newBufferedReader(path)) {
                ProfileTransfer transfer = GSON.fromJson(reader, ProfileTransfer.class);
                if (transfer == null || transfer.schemaVersion < 1
                        || transfer.schemaVersion > CURRENT_SCHEMA_VERSION
                        || !"tabnamedimmer-profile".equals(transfer.type) || transfer.profile == null) {
                    return null;
                }
                return sanitizeProfile(transfer.profile, "Imported");
            }
        } catch (IOException | RuntimeException exception) {
            TabNameDimmerClient.LOGGER.warn("Failed to import a Tab Name Dimmer profile", exception);
            return null;
        }
    }

    public Profile activeProfile(String scope) {
        if (scope != null && !scope.isBlank() && !GLOBAL_SCOPE.equals(scope)) {
            Profile profile = serverProfiles.get(scope);
            if (profile != null) {
                return profile;
            }
        }
        return globalProfile;
    }

    public Profile getOrCreateServerProfile(String scope) {
        if (scope == null || scope.isBlank() || GLOBAL_SCOPE.equals(scope)) {
            return globalProfile;
        }
        return serverProfiles.computeIfAbsent(scope, key -> {
            Profile copy = globalProfile.copy();
            copy.name = key;
            return copy;
        });
    }

    public boolean removeServerProfile(String scope) {
        return scope != null && serverProfiles.remove(scope) != null;
    }

    public Match findMatch(String playerName, String scope) {
        if (!enabled || playerName == null || playerName.isBlank()) {
            return null;
        }
        return activeProfile(scope).groups.stream()
                .filter(group -> group.enabled && group.contains(playerName, caseSensitive))
                .sorted(Comparator.comparingInt((PlayerGroup group) -> group.priority).reversed())
                .map(group -> new Match(group, group.priority))
                .findFirst()
                .orElse(null);
    }

    public boolean shouldDim(String playerName) {
        return shouldDim(playerName, ServerScopeTracker.currentScope());
    }

    public boolean shouldDim(String playerName, String scope) {
        return enabled && findMatch(playerName, scope) == null;
    }

    public List<PlayerGroup> orderedGroups(String scope) {
        return activeProfile(scope).groups.stream()
                .filter(group -> group.enabled)
                .sorted(Comparator.comparingInt((PlayerGroup group) -> group.priority).reversed()
                        .thenComparing(group -> group.name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    public TabNameDimmerConfig copy() {
        TabNameDimmerConfig copy = new TabNameDimmerConfig();
        copy.schemaVersion = schemaVersion;
        copy.enabled = enabled;
        copy.caseSensitive = caseSensitive;
        copy.notificationsEnabled = notificationsEnabled;
        copy.playerTransparencyEnabled = playerTransparencyEnabled;
        copy.dimOpacity = dimOpacity;
        copy.dimColor = dimColor;
        copy.displayMode = displayMode;
        copy.activationMode = activationMode;
        copy.playerSortMode = playerSortMode;
        copy.animationSpeed = animationSpeed;
        copy.hudAnchor = hudAnchor;
        copy.hudColumns = hudColumns;
        copy.hudMaxRows = hudMaxRows;
        copy.hudShowAvatars = hudShowAvatars;
        copy.hudShowPing = hudShowPing;
        copy.globalProfile = globalProfile.copy();
        copy.serverProfiles = new LinkedHashMap<>();
        serverProfiles.forEach((scope, profile) -> copy.serverProfiles.put(scope, profile.copy()));
        return copy;
    }

    static TabNameDimmerConfig sanitizeForTests(TabNameDimmerConfig config) {
        return sanitize(config);
    }

    static TabNameDimmerConfig fromJsonForTests(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        if (root.has("schemaVersion") && root.get("schemaVersion").getAsInt() >= CURRENT_SCHEMA_VERSION) {
            return sanitize(GSON.fromJson(root, TabNameDimmerConfig.class));
        }
        return sanitize(migrateLegacy(GSON.fromJson(root, LegacyConfig.class)));
    }

    private static TabNameDimmerConfig migrateLegacy(LegacyConfig legacy) {
        TabNameDimmerConfig migrated = defaults();
        if (legacy == null) {
            return migrated;
        }
        migrated.enabled = legacy.enabled;
        migrated.caseSensitive = legacy.caseSensitive;
        migrated.playerTransparencyEnabled = legacy.playerTransparencyEnabled;
        migrated.dimOpacity = legacy.dimOpacity;
        migrated.dimColor = legacy.dimColor;
        migrated.displayMode = legacy.displayMode;
        migrated.animationSpeed = legacy.animationSpeed;
        PlayerGroup group = migrated.globalProfile.groups.getFirst();
        group.glowingEnabled = legacy.glowingEnabled;
        group.members = legacy.allowedNames == null ? new ArrayList<>() : new ArrayList<>(legacy.allowedNames);
        return migrated;
    }

    private static TabNameDimmerConfig sanitize(TabNameDimmerConfig config) {
        if (config == null) {
            config = defaults();
        }
        config.schemaVersion = CURRENT_SCHEMA_VERSION;
        config.dimColor &= 0xFFFFFF;
        config.displayMode = config.displayMode == null ? DisplayMode.ANIMATED_SORT : config.displayMode;
        config.activationMode = config.activationMode == null ? ActivationMode.HOLD_SHIFT : config.activationMode;
        config.playerSortMode = config.playerSortMode == null ? PlayerSortMode.ORIGINAL : config.playerSortMode;
        config.hudAnchor = config.hudAnchor == null ? HudAnchor.BOTTOM_CENTER : config.hudAnchor;
        config.animationSpeed = clampFinite(config.animationSpeed, 0.05F, 0.001F, 1.0F);
        config.dimOpacity = clampFinite(config.dimOpacity, 0.3F, 0.05F, 1.0F);
        config.hudColumns = Math.max(1, Math.min(4, config.hudColumns));
        config.hudMaxRows = Math.max(1, Math.min(100, config.hudMaxRows));
        config.globalProfile = sanitizeProfile(config.globalProfile, "Global");
        if (config.serverProfiles == null) {
            config.serverProfiles = new LinkedHashMap<>();
        }
        Map<String, Profile> sanitizedProfiles = new LinkedHashMap<>();
        for (Map.Entry<String, Profile> entry : config.serverProfiles.entrySet()) {
            if (sanitizedProfiles.size() >= MAX_SERVER_PROFILES) {
                break;
            }
            String scope = trimToLength(entry.getKey(), 255);
            if (!scope.isBlank() && !GLOBAL_SCOPE.equals(scope) && entry.getValue() != null) {
                sanitizedProfiles.put(scope, sanitizeProfile(entry.getValue(), scope));
            }
        }
        config.serverProfiles = sanitizedProfiles;
        return config;
    }

    private static Profile sanitizeProfile(Profile profile, String fallbackName) {
        if (profile == null) {
            profile = new Profile(fallbackName);
        }
        profile.name = trimToLength(profile.name, 64);
        if (profile.name.isBlank()) {
            profile.name = trimToLength(fallbackName, 64);
        }
        if (profile.groups == null) {
            profile.groups = new ArrayList<>();
        }
        List<PlayerGroup> groups = new ArrayList<>();
        int totalMembers = 0;
        for (PlayerGroup group : profile.groups) {
            if (groups.size() >= MAX_GROUPS) {
                break;
            }
            if (group != null) {
                PlayerGroup sanitized = sanitizeGroup(group, groups.size() + 1,
                        MAX_TOTAL_MEMBERS_PER_PROFILE - totalMembers);
                totalMembers += sanitized.members.size();
                groups.add(sanitized);
            }
        }
        if (groups.isEmpty()) {
            groups.add(PlayerGroup.defaultGroup());
        }
        profile.groups = groups;
        return profile;
    }

    private static PlayerGroup sanitizeGroup(PlayerGroup group, int index, int remainingProfileMembers) {
        group.name = trimToLength(group.name, 64);
        if (group.name.isBlank()) {
            group.name = "Group " + index;
        }
        group.color &= 0xFFFFFF;
        group.priority = Math.max(-1000, Math.min(1000, group.priority));
        if (group.members == null) {
            group.members = new ArrayList<>();
        }
        Set<String> members = new LinkedHashSet<>();
        for (String member : group.members) {
            if (members.size() >= MAX_MEMBERS_PER_GROUP || members.size() >= remainingProfileMembers) {
                break;
            }
            String cleaned = trimToLength(member, 64);
            if (!cleaned.isBlank()) {
                members.add(cleaned);
            }
        }
        group.members = new ArrayList<>(members);
        group.invalidateCache();
        return group;
    }

    private static float clampFinite(float value, float fallback, float minimum, float maximum) {
        float finite = Float.isFinite(value) ? value : fallback;
        return Math.max(minimum, Math.min(maximum, finite));
    }

    private static String normalizeName(String name, boolean caseSensitive) {
        String trimmed = name == null ? "" : name.trim();
        return caseSensitive ? trimmed : trimmed.toLowerCase(Locale.ROOT);
    }

    private static String trimToLength(String value, int maximumLength) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.length() <= maximumLength ? trimmed : trimmed.substring(0, maximumLength);
    }

    private static void ensureConfigExists() {
        if (!Files.exists(configPath()) && !save(defaults())) {
            TabNameDimmerClient.LOGGER.warn("Failed to create the Tab Name Dimmer configuration");
        }
    }

    private static TabNameDimmerConfig defaults() {
        return new TabNameDimmerConfig();
    }

    private static TabNameDimmerConfig loadBackup() {
        Path configPath = configPath();
        Path backupPath = backupPath();
        if (!Files.isRegularFile(backupPath)) {
            return defaults();
        }
        try (Reader reader = Files.newBufferedReader(backupPath)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            TabNameDimmerConfig recovered = root.has("schemaVersion")
                    ? GSON.fromJson(root, TabNameDimmerConfig.class)
                    : migrateLegacy(GSON.fromJson(root, LegacyConfig.class));
            recovered = sanitize(recovered);
            try {
                if (Files.isRegularFile(configPath)) {
                    Files.move(configPath, configPath.resolveSibling("tab-name-dimmer.json.corrupt"),
                            StandardCopyOption.REPLACE_EXISTING);
                }
                Files.copy(backupPath, configPath, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException exception) {
                TabNameDimmerClient.LOGGER.warn("Failed to restore the primary Tab Name Dimmer configuration", exception);
            }
            return recovered;
        } catch (IOException | RuntimeException exception) {
            TabNameDimmerClient.LOGGER.warn("Failed to recover the Tab Name Dimmer configuration backup", exception);
            return defaults();
        }
    }

    private static boolean writeJsonAtomically(Path path, Path backup, Object value) {
        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
        try {
            Files.createDirectories(path.toAbsolutePath().getParent());
            try (Writer writer = Files.newBufferedWriter(temporary)) {
                GSON.toJson(value, writer);
            }
            if (Files.isRegularFile(path)) {
                Files.copy(path, backup, StandardCopyOption.REPLACE_EXISTING);
            }
            try {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException exception) {
            TabNameDimmerClient.LOGGER.warn("Failed to write Tab Name Dimmer JSON data", exception);
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException cleanupException) {
                TabNameDimmerClient.LOGGER.debug("Failed to clean a temporary Tab Name Dimmer file", cleanupException);
            }
            return false;
        }
    }

    private static long currentModifiedTime() {
        try {
            Path configPath = configPath();
            return Files.exists(configPath) ? Files.getLastModifiedTime(configPath).toMillis() : -1L;
        } catch (IOException ignored) {
            return -1L;
        }
    }

    private static Path configPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("tab-name-dimmer.json");
    }

    private static Path backupPath() {
        Path configPath = configPath();
        return configPath.resolveSibling("tab-name-dimmer.json.bak");
    }
}
