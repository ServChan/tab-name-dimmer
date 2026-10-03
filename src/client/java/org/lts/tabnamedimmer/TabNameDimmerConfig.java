package org.lts.tabnamedimmer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

public class TabNameDimmerConfig {
    public static final int CURRENT_SCHEMA_VERSION = 2;
    public static final String GLOBAL_SCOPE = "global";
    public static final long MAX_TRANSFER_BYTES = 1024L * 1024L;
    public static final int MAX_GROUPS = 32;
    public static final int MAX_MEMBERS_PER_GROUP = 4096;
    public static final int MIN_PRIORITY = -1000;
    public static final int MAX_PRIORITY = 1000;
    private static final int MAX_TOTAL_MEMBERS_PER_PROFILE = 8192;
    private static final int MAX_SERVER_PROFILES = 128;

    private static final Logger LOGGER = LoggerFactory.getLogger("tabnamedimmer");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Object IO_LOCK = new Object();
    private static volatile TabNameDimmerConfig instance = defaults();
    private static volatile long lastModified = -1L;
    private static volatile Path configDirectoryOverride;
    private static ScheduledExecutorService watcher;

    public enum DisplayMode {
        ANIMATED_SORT, FILTER, EXTRA_HUD
    }

    public enum ActivationMode {
        HOLD_SHIFT, HOLD_KEY, TOGGLE_KEY
    }

    public enum PlayerSortMode {
        ORIGINAL, NAME, PING
    }

    public enum AfkHandlingMode {
        SHOW, MOVE_TO_END, HIDE
    }

    public enum HudAnchor {
        TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT
    }

    public int schemaVersion = CURRENT_SCHEMA_VERSION;
    public boolean enabled = true;
    public boolean caseSensitive;
    public boolean notificationsEnabled = true;
    public boolean compactNotifications;
    public boolean notificationSoundsEnabled;
    public boolean playerTransparencyEnabled;
    public float dimOpacity = 0.3F;
    public int dimColor = 0x555555;
    public DisplayMode displayMode = DisplayMode.ANIMATED_SORT;
    public ActivationMode activationMode = ActivationMode.HOLD_SHIFT;
    public PlayerSortMode playerSortMode = PlayerSortMode.ORIGINAL;
    public AfkHandlingMode afkHandlingMode = AfkHandlingMode.SHOW;
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

        private transient volatile MatchIndex matchIndex;

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
            if (groups != null) {
                for (PlayerGroup group : groups) {
                    if (group != null) {
                        copy.groups.add(group.copy());
                    }
                }
            }
            return copy;
        }

        public Match findMatch(String playerName, boolean caseSensitive) {
            if (playerName == null || playerName.isBlank() || groups == null) {
                return null;
            }
            MatchIndex index = matchIndex;
            if (index == null || !index.isCurrent(groups, caseSensitive)) {
                index = MatchIndex.build(groups, caseSensitive);
                matchIndex = index;
            }
            return index.find(playerName);
        }

        public boolean sameGroupsAs(Profile other) {
            if (other == null || groups == null || other.groups == null || groups.size() != other.groups.size()) {
                return false;
            }
            for (int i = 0; i < groups.size(); i++) {
                PlayerGroup group = groups.get(i);
                if (group == null ? other.groups.get(i) != null : !group.sameAs(other.groups.get(i))) {
                    return false;
                }
            }
            return true;
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

        public static PlayerGroup defaultGroup() {
            return new PlayerGroup();
        }

        public static boolean isMask(String member) {
            return member != null && (member.indexOf('*') >= 0 || member.indexOf('?') >= 0);
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
            copy.members = members == null ? new ArrayList<>() : new ArrayList<>(members);
            return copy;
        }

        public boolean sameAs(PlayerGroup other) {
            return other != null
                    && Objects.equals(name, other.name)
                    && color == other.color
                    && priority == other.priority
                    && enabled == other.enabled
                    && colorizeNames == other.colorizeNames
                    && glowingEnabled == other.glowingEnabled
                    && transparencyEnabled == other.transparencyEnabled
                    && Objects.equals(members, other.members);
        }
    }

    public record Match(PlayerGroup group, int priority, int order) {
        public Match(PlayerGroup group, int priority) {
            this(group, priority, 0);
        }

        boolean outranks(Match other) {
            return other == null || priority > other.priority || (priority == other.priority && order < other.order);
        }
    }

    private record MaskRule(Pattern pattern, Match match) {
    }

    private static final class MatchIndex {
        private final List<PlayerGroup> source;
        private final boolean caseSensitive;
        private final PlayerGroup[] groups;
        private final boolean[] enabled;
        private final int[] priorities;
        private final List<?>[] memberLists;
        private final int[] memberSizes;
        private final Map<String, Match> exact;
        private final List<MaskRule> masks;

        private MatchIndex(List<PlayerGroup> source, boolean caseSensitive, Map<String, Match> exact,
                           List<MaskRule> masks) {
            int size = source.size();
            this.source = source;
            this.caseSensitive = caseSensitive;
            this.groups = new PlayerGroup[size];
            this.enabled = new boolean[size];
            this.priorities = new int[size];
            this.memberLists = new List<?>[size];
            this.memberSizes = new int[size];
            for (int i = 0; i < size; i++) {
                PlayerGroup group = source.get(i);
                groups[i] = group;
                if (group != null) {
                    enabled[i] = group.enabled;
                    priorities[i] = group.priority;
                    memberLists[i] = group.members;
                    memberSizes[i] = group.members == null ? 0 : group.members.size();
                }
            }
            this.exact = exact;
            this.masks = masks;
        }

        static MatchIndex build(List<PlayerGroup> source, boolean caseSensitive) {
            Map<String, Match> exact = new HashMap<>();
            List<MaskRule> masks = new ArrayList<>();
            for (int order = 0; order < source.size(); order++) {
                PlayerGroup group = source.get(order);
                if (group == null || !group.enabled || group.members == null) {
                    continue;
                }
                Match match = new Match(group, group.priority, order);
                for (String member : group.members) {
                    String normalized = normalizeName(member, caseSensitive);
                    if (normalized.isEmpty()) {
                        continue;
                    }
                    if (PlayerGroup.isMask(normalized)) {
                        masks.add(new MaskRule(compileMask(normalized), match));
                    } else {
                        exact.merge(normalized, match,
                                (current, candidate) -> candidate.outranks(current) ? candidate : current);
                    }
                }
            }
            masks.sort((left, right) -> left.match().outranks(right.match()) ? -1
                    : right.match().outranks(left.match()) ? 1 : 0);
            return new MatchIndex(source, caseSensitive, exact, List.copyOf(masks));
        }

        boolean isCurrent(List<PlayerGroup> current, boolean currentCaseSensitive) {
            if (current != source || currentCaseSensitive != caseSensitive || current.size() != groups.length) {
                return false;
            }
            for (int i = 0; i < groups.length; i++) {
                PlayerGroup group = current.get(i);
                if (group != groups[i]) {
                    return false;
                }
                if (group != null && (group.enabled != enabled[i] || group.priority != priorities[i]
                        || group.members != memberLists[i]
                        || (group.members == null ? 0 : group.members.size()) != memberSizes[i])) {
                    return false;
                }
            }
            return true;
        }

        Match find(String playerName) {
            String normalized = normalizeName(playerName, caseSensitive);
            Match best = exact.get(normalized);
            for (MaskRule rule : masks) {
                if (!rule.match().outranks(best)) {
                    break;
                }
                if (rule.pattern().matcher(normalized).matches()) {
                    return rule.match();
                }
            }
            return best;
        }
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
        synchronized (IO_LOCK) {
            Path configPath = configPath();
            if (!Files.exists(configPath)) {
                if (Files.isRegularFile(backupPath())) {
                    LOGGER.warn("The Tab Name Dimmer configuration is missing; restoring it from the backup");
                    instance = loadBackup();
                    lastModified = currentModifiedTime();
                    return instance;
                }
                if (!save(defaults())) {
                    LOGGER.warn("Failed to create the Tab Name Dimmer configuration");
                }
            }
            try (Reader reader = Files.newBufferedReader(configPath)) {
                JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
                TabNameDimmerConfig loaded = parseConfig(root);
                instance = sanitize(loaded == null ? defaults() : loaded);
                lastModified = Files.getLastModifiedTime(configPath).toMillis();
            } catch (IOException | RuntimeException exception) {
                LOGGER.warn("Failed to load the Tab Name Dimmer configuration; using recovery", exception);
                instance = loadBackup();
                lastModified = currentModifiedTime();
            }
            return instance;
        }
    }

    public static synchronized void startWatching() {
        if (watcher != null) {
            return;
        }
        watcher = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "Tab Name Dimmer config watcher");
            thread.setDaemon(true);
            return thread;
        });
        watcher.scheduleWithFixedDelay(TabNameDimmerConfig::reloadIfChanged, 1L, 1L, TimeUnit.SECONDS);
    }

    static void reloadIfChanged() {
        try {
            synchronized (IO_LOCK) {
                if (currentModifiedTime() != lastModified) {
                    load();
                }
            }
        } catch (RuntimeException exception) {
            LOGGER.warn("Failed to reload the Tab Name Dimmer configuration", exception);
        }
    }

    static void useConfigDirectoryForTests(Path directory) {
        synchronized (IO_LOCK) {
            configDirectoryOverride = directory;
            instance = defaults();
            lastModified = -1L;
        }
    }

    public static TabNameDimmerConfig current() {
        return instance;
    }

    public static TabNameDimmerConfig currentCopy() {
        return instance.copy();
    }

    public static boolean save(TabNameDimmerConfig config) {
        TabNameDimmerConfig sanitized = sanitize(config == null ? defaults() : config.copy());
        synchronized (IO_LOCK) {
            if (!writeJsonAtomically(configPath(), backupPath(), sanitized)) {
                return false;
            }
            instance = sanitized;
            lastModified = currentModifiedTime();
            return true;
        }
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
            LOGGER.warn("Failed to import a Tab Name Dimmer profile", exception);
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

    public boolean hasServerProfile(String scope) {
        return scope != null && serverProfiles.containsKey(scope);
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
        if (!enabled) {
            return null;
        }
        return activeProfile(scope).findMatch(playerName, caseSensitive);
    }

    public boolean shouldDim(String playerName) {
        return shouldDim(playerName, ServerScopeTracker.currentScope());
    }

    public boolean shouldDim(String playerName, String scope) {
        return enabled && findMatch(playerName, scope) == null;
    }

    public boolean sameSettingsAs(TabNameDimmerConfig other) {
        return other != null && GSON.toJsonTree(this).equals(GSON.toJsonTree(other));
    }

    public TabNameDimmerConfig copy() {
        TabNameDimmerConfig copy = new TabNameDimmerConfig();
        copy.schemaVersion = schemaVersion;
        copy.enabled = enabled;
        copy.caseSensitive = caseSensitive;
        copy.notificationsEnabled = notificationsEnabled;
        copy.compactNotifications = compactNotifications;
        copy.notificationSoundsEnabled = notificationSoundsEnabled;
        copy.playerTransparencyEnabled = playerTransparencyEnabled;
        copy.dimOpacity = dimOpacity;
        copy.dimColor = dimColor;
        copy.displayMode = displayMode;
        copy.activationMode = activationMode;
        copy.playerSortMode = playerSortMode;
        copy.afkHandlingMode = afkHandlingMode;
        copy.animationSpeed = animationSpeed;
        copy.hudAnchor = hudAnchor;
        copy.hudColumns = hudColumns;
        copy.hudMaxRows = hudMaxRows;
        copy.hudShowAvatars = hudShowAvatars;
        copy.hudShowPing = hudShowPing;
        copy.globalProfile = globalProfile == null ? Profile.defaultProfile() : globalProfile.copy();
        copy.serverProfiles = new LinkedHashMap<>();
        if (serverProfiles != null) {
            serverProfiles.forEach((scope, profile) -> {
                if (profile != null) {
                    copy.serverProfiles.put(scope, profile.copy());
                }
            });
        }
        return copy;
    }

    static TabNameDimmerConfig sanitizeForTests(TabNameDimmerConfig config) {
        return sanitize(config);
    }

    static TabNameDimmerConfig fromJsonForTests(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        return sanitize(parseConfig(root));
    }

    private static TabNameDimmerConfig parseConfig(JsonObject root) {
        if (!root.has("schemaVersion")) {
            return migrateLegacy(GSON.fromJson(root, LegacyConfig.class));
        }
        int schemaVersion = root.get("schemaVersion").getAsInt();
        if (schemaVersion > CURRENT_SCHEMA_VERSION) {
            throw new IllegalArgumentException("Unsupported future config schema version: " + schemaVersion);
        }
        if (schemaVersion < CURRENT_SCHEMA_VERSION) {
            return migrateLegacy(GSON.fromJson(root, LegacyConfig.class));
        }
        return GSON.fromJson(root, TabNameDimmerConfig.class);
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
        config.afkHandlingMode = config.afkHandlingMode == null ? AfkHandlingMode.SHOW : config.afkHandlingMode;
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
        group.priority = clampPriority(group.priority);
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
        return group;
    }

    public static int clampPriority(int priority) {
        return Math.max(MIN_PRIORITY, Math.min(MAX_PRIORITY, priority));
    }

    private static float clampFinite(float value, float fallback, float minimum, float maximum) {
        float finite = Float.isFinite(value) ? value : fallback;
        return Math.max(minimum, Math.min(maximum, finite));
    }

    private static String normalizeName(String name, boolean caseSensitive) {
        String trimmed = name == null ? "" : name.trim();
        return caseSensitive ? trimmed : trimmed.toLowerCase(Locale.ROOT);
    }

    private static Pattern compileMask(String mask) {
        StringBuilder regex = new StringBuilder(mask.length() + 8);
        StringBuilder literal = new StringBuilder();
        for (int i = 0; i < mask.length(); i++) {
            char character = mask.charAt(i);
            if (character == '*' || character == '?') {
                if (!literal.isEmpty()) {
                    regex.append(Pattern.quote(literal.toString()));
                    literal.setLength(0);
                }
                regex.append(character == '*' ? ".*" : ".");
            } else {
                literal.append(character);
            }
        }
        if (!literal.isEmpty()) {
            regex.append(Pattern.quote(literal.toString()));
        }
        return Pattern.compile(regex.toString(), Pattern.DOTALL);
    }

    private static String trimToLength(String value, int maximumLength) {
        String trimmed = value == null ? "" : value.trim();
        return trimmed.length() <= maximumLength ? trimmed : trimmed.substring(0, maximumLength);
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
            TabNameDimmerConfig recovered = parseConfig(root);
            recovered = sanitize(recovered);
            try {
                if (Files.isRegularFile(configPath)) {
                    Files.move(configPath, configPath.resolveSibling("tab-name-dimmer.json.corrupt"),
                            StandardCopyOption.REPLACE_EXISTING);
                }
                Files.copy(backupPath, configPath, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException exception) {
                LOGGER.warn("Failed to restore the primary Tab Name Dimmer configuration", exception);
            }
            return recovered;
        } catch (IOException | RuntimeException exception) {
            LOGGER.warn("Failed to recover the Tab Name Dimmer configuration backup", exception);
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
            LOGGER.warn("Failed to write Tab Name Dimmer JSON data", exception);
            try {
                Files.deleteIfExists(temporary);
            } catch (IOException cleanupException) {
                LOGGER.debug("Failed to clean a temporary Tab Name Dimmer file", cleanupException);
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
        Path directory = configDirectoryOverride;
        return (directory != null ? directory : FabricLoader.getInstance().getConfigDir())
                .resolve("tab-name-dimmer.json");
    }

    private static Path backupPath() {
        return configPath().resolveSibling("tab-name-dimmer.json.bak");
    }
}
