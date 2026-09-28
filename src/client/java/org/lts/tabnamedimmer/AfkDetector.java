package org.lts.tabnamedimmer;

import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.criteria.ObjectiveCriteria;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class AfkDetector {

    private static final String VANILLASQUAD_SLEEP_ICON = "\uA423";
    private static final Pattern EXPLICIT_MARKER = Pattern.compile(
            "(?iu)(?:^|[^\\p{L}\\p{N}_])(?:afk|away)(?:$|[^\\p{L}\\p{N}_])");
    private static final Pattern PREFIX_MARKER = Pattern.compile(
            "(?iu)^\\s*(?:\\[|\\(|\\{)?(?:afk|away)(?:\\]|\\)|\\})?\\s*$");
    private static final Pattern SLEEP_MARKER = Pattern.compile(
            "(?iu)(?:^|[^\\p{L}\\p{N}_])([zᴢᶻ]+)(?:$|[^\\p{L}\\p{N}_])");

    private AfkDetector() {
    }

    public static boolean isAfk(PlayerInfo info) {
        return isAfk(info, playerListObjectiveValue(info));
    }

    static boolean isAfk(PlayerInfo info, Component playerListObjectiveValue) {
        if (info == null || info.getProfile() == null) {
            return false;
        }
        String playerName = info.getProfile().name();
        if (playerName == null || playerName.isBlank()) {
            return false;
        }
        if (hasUnambiguousSleepMarker(playerListObjectiveValue)) {
            return true;
        }
        Component serverName = info.getTabListDisplayName();
        if (serverName == null) {
            serverName = PlayerTeam.formatNameForTeam(info.getTeam(), Component.literal(playerName));
        }
        return isAfk(playerName, serverName);
    }

    static Component playerListObjectiveValue(PlayerInfo info) {
        if (info == null || info.getProfile() == null) {
            return null;
        }
        net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
        if (minecraft == null || minecraft.level == null) {
            return null;
        }
        Scoreboard scoreboard = minecraft.level.getScoreboard();
        Objective objective = scoreboard.getDisplayObjective(DisplaySlot.LIST);
        if (objective == null || objective.getRenderType() == ObjectiveCriteria.RenderType.HEARTS) {
            return null;
        }
        ReadOnlyScoreInfo score = scoreboard.getPlayerScoreInfo(
                ScoreHolder.fromGameProfile(info.getProfile()), objective);
        if (score == null) {
            return null;
        }
        return score.formatValue(objective.numberFormatOrDefault(StyledFormat.PLAYER_LIST_DEFAULT));
    }

    static boolean isAfk(String playerName, Component displayName) {
        if (playerName == null || playerName.isBlank() || displayName == null) {
            return false;
        }

        String visibleName = displayName.getString();

        if (visibleName.contains(VANILLASQUAD_SLEEP_ICON)) {
            return true;
        }

        String lowerVisible = visibleName.toLowerCase(Locale.ROOT);
        String lowerName = playerName.toLowerCase(Locale.ROOT);
        int accountNameStart = lowerVisible.indexOf(lowerName);
        if (accountNameStart < 0) {
            return false;
        }
        String prefix = lowerVisible.substring(0, accountNameStart);
        String suffix = lowerVisible.substring(accountNameStart + lowerName.length());
        if (EXPLICIT_MARKER.matcher(suffix).find() || PREFIX_MARKER.matcher(prefix).find()
                || suffix.contains("💤") || prefix.stripTrailing().endsWith("💤")) {
            return true;
        }
        Matcher matcher = SLEEP_MARKER.matcher(suffix);
        while (matcher.find()) {
            if (matcher.group(1).codePointCount(0, matcher.group(1).length()) >= 2) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasUnambiguousSleepMarker(Component component) {
        return component != null && (component.getString().contains(VANILLASQUAD_SLEEP_ICON)
                || component.getString().contains("💤"));
    }
}
