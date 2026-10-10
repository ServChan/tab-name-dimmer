package org.lts.tabnamedimmer;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.lts.tabnamedimmer.OnlinePlayerTracker.TrackedPlayer;
import org.lts.tabnamedimmer.OnlinePlayerTracker.PresenceChange;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OnlinePlayerTrackerTest {
    private OnlinePlayerTracker tracker;
    private TabNameDimmerConfig config;
    private List<Component> sentMessages;
    private static final String SCOPE = "multiplayer:play.example.com";

    private final UUID trackedUuid = UUID.randomUUID();
    private final UUID untrackedUuid = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        tracker = new OnlinePlayerTracker();
        config = new TabNameDimmerConfig();
        config.enabled = true;
        config.notificationsEnabled = true;
        config.globalProfile.groups.getFirst().members.add("TrackedUser");
        sentMessages = new ArrayList<>();
    }

    private static TrackedPlayer player(UUID uuid, String name) {
        return new TrackedPlayer(uuid, name, null);
    }

    private static TrackedPlayer player(UUID uuid, String name, Component serverDisplayName) {
        return new TrackedPlayer(uuid, name, serverDisplayName);
    }

    private void runChecks(int count, Map<UUID, TrackedPlayer> players, Consumer<PresenceChange> sounds) {
        for (int i = 0; i < count; i++) {
            tracker.processTick(SCOPE, players, config, sentMessages::add, sounds);
        }
    }

    @Test
    void initialTickSetsBaselineWithoutNotifications() {
        Map<UUID, TrackedPlayer> players = Map.of(trackedUuid, player(trackedUuid, "TrackedUser"));
        tracker.processTick(SCOPE, players, config, sentMessages::add);

        assertTrue(sentMessages.isEmpty(), "Initial tick should not send join notifications for existing players");
    }

    @Test
    void notifiesWhenTrackedPlayerJoinsWithIconAndStyledName() {
        Map<UUID, TrackedPlayer> players1 = Map.of(untrackedUuid, player(untrackedUuid, "UntrackedUser"));
        tracker.processTick(SCOPE, players1, config, sentMessages::add);
        sentMessages.clear();

        Map<UUID, TrackedPlayer> players2 = new LinkedHashMap<>();
        players2.put(untrackedUuid, player(untrackedUuid, "UntrackedUser"));
        players2.put(trackedUuid, player(trackedUuid, "TrackedUser"));

        tracker.processTick(SCOPE, players2, config, sentMessages::add);

        assertEquals(1, sentMessages.size());
        Component message = sentMessages.getFirst();
        List<Component> siblings = message.getSiblings();

        assertEquals(2, siblings.size());
        assertEquals("[+] ", siblings.getFirst().getString());

        TranslatableContents contents = (TranslatableContents) siblings.get(1).getContents();
        assertEquals("tabnamedimmer.notification.joined", contents.getKey());

        Component styledArg = (Component) contents.getArgs()[0];
        assertNotNull(styledArg);
    }

    @Test
    void doesNotNotifyWhenUntrackedPlayerJoins() {
        Map<UUID, TrackedPlayer> players1 = Map.of(trackedUuid, player(trackedUuid, "TrackedUser"));
        tracker.processTick(SCOPE, players1, config, sentMessages::add);
        sentMessages.clear();

        Map<UUID, TrackedPlayer> players2 = new LinkedHashMap<>();
        players2.put(trackedUuid, player(trackedUuid, "TrackedUser"));
        players2.put(untrackedUuid, player(untrackedUuid, "UntrackedUser"));

        tracker.processTick(SCOPE, players2, config, sentMessages::add);

        assertTrue(sentMessages.isEmpty(), "Untracked player joining should not fire notification");
    }

    @Test
    void notifiesWhenTrackedPlayerLeavesWithLeaveIcon() {
        Map<UUID, TrackedPlayer> players1 = new LinkedHashMap<>();
        players1.put(untrackedUuid, player(untrackedUuid, "UntrackedUser"));
        players1.put(trackedUuid, player(trackedUuid, "TrackedUser"));

        tracker.processTick(SCOPE, players1, config, sentMessages::add);
        sentMessages.clear();

        Map<UUID, TrackedPlayer> players2 = Map.of(untrackedUuid, player(untrackedUuid, "UntrackedUser"));
        tracker.processTick(SCOPE, players2, config, sentMessages::add);
        assertTrue(sentMessages.isEmpty(), "Leave notification waits for the grace period");

        runChecks(OnlinePlayerTracker.LEAVE_GRACE_CHECKS, players2, sounds -> {});

        assertEquals(1, sentMessages.size());
        Component message = sentMessages.getFirst();
        List<Component> siblings = message.getSiblings();

        assertEquals(2, siblings.size());
        assertEquals("[-] ", siblings.getFirst().getString());

        TranslatableContents contents = (TranslatableContents) siblings.get(1).getContents();
        assertEquals("tabnamedimmer.notification.left", contents.getKey());
    }

    @Test
    void compactNotificationContainsOnlyIconAndName() {
        config.compactNotifications = true;
        tracker.processTick(SCOPE, Map.of(untrackedUuid, player(untrackedUuid, "UntrackedUser")),
                config, sentMessages::add);

        Map<UUID, TrackedPlayer> players = new LinkedHashMap<>();
        players.put(untrackedUuid, player(untrackedUuid, "UntrackedUser"));
        players.put(trackedUuid, player(trackedUuid, "TrackedUser"));
        tracker.processTick(SCOPE, players, config, sentMessages::add);

        assertEquals(1, sentMessages.size());
        assertEquals("[+] TrackedUser", sentMessages.getFirst().getString());
        assertEquals(2, sentMessages.getFirst().getSiblings().size());
    }

    @Test
    void playsDifferentSoundEventsForTrackedJoinAndLeaveWhenEnabled() {
        config.notificationSoundsEnabled = true;
        List<PresenceChange> sounds = new ArrayList<>();
        Map<UUID, TrackedPlayer> baseline = Map.of(untrackedUuid, player(untrackedUuid, "UntrackedUser"));
        tracker.processTick(SCOPE, baseline, config, sentMessages::add, sounds::add);

        Map<UUID, TrackedPlayer> joined = new LinkedHashMap<>(baseline);
        joined.put(trackedUuid, player(trackedUuid, "TrackedUser"));
        tracker.processTick(SCOPE, joined, config, sentMessages::add, sounds::add);
        tracker.processTick(SCOPE, baseline, config, sentMessages::add, sounds::add);
        runChecks(OnlinePlayerTracker.LEAVE_GRACE_CHECKS, baseline, sounds::add);

        assertEquals(List.of(PresenceChange.JOINED, PresenceChange.LEFT), sounds);
    }

    @Test
    void doesNotPlayNotificationSoundWhenSoundOptionIsDisabled() {
        List<PresenceChange> sounds = new ArrayList<>();
        tracker.processTick(SCOPE, Map.of(), config, sentMessages::add, sounds::add);
        tracker.processTick(SCOPE, Map.of(trackedUuid, player(trackedUuid, "TrackedUser")),
                config, sentMessages::add, sounds::add);

        assertTrue(sounds.isEmpty());
    }

    @Test
    void respectsGroupColorizationToggle() {

        TabNameDimmerConfig.PlayerGroup group = config.globalProfile.groups.getFirst();
        group.colorizeNames = true;
        group.color = 0xFF5555;

        Component styledWithModColor = OnlinePlayerTracker.formatPlayerName("TrackedUser", null, config, SCOPE);
        assertNotNull(styledWithModColor.getStyle().getColor());

        group.colorizeNames = false;
        Component serverCustomName = Component.literal("[VIP] TrackedUser");
        Component styledWithServerColor = OnlinePlayerTracker.formatPlayerName("TrackedUser", serverCustomName, config, SCOPE);

        assertEquals(serverCustomName, styledWithServerColor);
    }

    @Test
    void respectsDisabledNotifications() {
        config.notificationsEnabled = false;

        Map<UUID, TrackedPlayer> players1 = Map.of(untrackedUuid, player(untrackedUuid, "UntrackedUser"));
        tracker.processTick(SCOPE, players1, config, sentMessages::add);

        Map<UUID, TrackedPlayer> players2 = Map.of(untrackedUuid, player(untrackedUuid, "UntrackedUser"), trackedUuid, player(trackedUuid, "TrackedUser"));
        tracker.processTick(SCOPE, players2, config, sentMessages::add);

        assertTrue(sentMessages.isEmpty(), "No notifications when notificationsEnabled is false");
    }

    @Test
    void scopeChangeResetsBaselineWithoutNotification() {
        Map<UUID, TrackedPlayer> players1 = Map.of(untrackedUuid, player(untrackedUuid, "UntrackedUser"));
        tracker.processTick(SCOPE, players1, config, sentMessages::add);

        Map<UUID, TrackedPlayer> players2 = Map.of(trackedUuid, player(trackedUuid, "TrackedUser"));
        tracker.processTick("multiplayer:other.server.com", players2, config, sentMessages::add);

        assertTrue(sentMessages.isEmpty(), "Changing server scope should reset baseline without join notifications");
    }

    @Test
    void worldSwitchWithinGracePeriodIsSilent() {
        Map<UUID, TrackedPlayer> online = new LinkedHashMap<>();
        online.put(untrackedUuid, player(untrackedUuid, "UntrackedUser"));
        online.put(trackedUuid, player(trackedUuid, "TrackedUser"));
        Map<UUID, TrackedPlayer> switching = Map.of(untrackedUuid, player(untrackedUuid, "UntrackedUser"));

        tracker.processTick(SCOPE, online, config, sentMessages::add);
        tracker.processTick(SCOPE, switching, config, sentMessages::add);
        runChecks(OnlinePlayerTracker.LEAVE_GRACE_CHECKS - 1, switching, ignored -> {});
        tracker.processTick(SCOPE, online, config, sentMessages::add);
        runChecks(OnlinePlayerTracker.LEAVE_GRACE_CHECKS * 2, online, ignored -> {});

        assertTrue(sentMessages.isEmpty(), "Leaving and returning within the grace period is a world switch");
    }

    @Test
    void leaveIsReportedOnceAfterGracePeriod() {
        Map<UUID, TrackedPlayer> online = Map.of(trackedUuid, player(trackedUuid, "TrackedUser"));
        tracker.processTick(SCOPE, online, config, sentMessages::add);
        tracker.processTick(SCOPE, Map.of(), config, sentMessages::add);

        runChecks(OnlinePlayerTracker.LEAVE_GRACE_CHECKS - 1, Map.of(), ignored -> {});
        assertTrue(sentMessages.isEmpty());

        runChecks(OnlinePlayerTracker.LEAVE_GRACE_CHECKS * 2, Map.of(), ignored -> {});
        assertEquals(1, sentMessages.size());
    }

    @Test
    void scopeChangeDropsPendingLeave() {
        tracker.processTick(SCOPE, Map.of(trackedUuid, player(trackedUuid, "TrackedUser")), config, sentMessages::add);
        tracker.processTick(SCOPE, Map.of(), config, sentMessages::add);
        tracker.processTick("multiplayer:other.server.com", Map.of(), config, sentMessages::add);
        for (int i = 0; i < OnlinePlayerTracker.LEAVE_GRACE_CHECKS * 2; i++) {
            tracker.processTick("multiplayer:other.server.com", Map.of(), config, sentMessages::add);
        }

        assertTrue(sentMessages.isEmpty());
    }
}
