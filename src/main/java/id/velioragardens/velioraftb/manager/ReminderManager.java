package id.velioragardens.velioraftb.manager;

import id.velioragardens.velioraftb.VelioraFTB;
import id.velioragardens.velioraftb.util.TextUtil;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class ReminderManager {
    private final VelioraFTB plugin;
    private final Map<String, Long> lastReminder = new HashMap<>();
    private final Map<String, Set<Integer>> sentWarnings = new HashMap<>();
    private BukkitTask task;

    public ReminderManager(VelioraFTB plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        if (!plugin.getConfig().getBoolean("reminders.enabled", true)) {
            return;
        }

        long seconds = Math.max(
                10L,
                plugin.getConfig().getLong("reminders.check-interval-seconds", 30L)
        );
        task = plugin.getServer().getScheduler().runTaskTimer(
                plugin,
                this::checkPlayers,
                20L,
                seconds * 20L
        );
    }

    public void restart() {
        lastReminder.clear();
        sentWarnings.clear();
        start();
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public void reset(UUID uuid, String skill) {
        String key = key(uuid, skill);
        lastReminder.put(key, System.currentTimeMillis());
        sentWarnings.remove(key);
    }

    private void checkPlayers() {
        long now = System.currentTimeMillis();
        long reminderInterval = Math.max(
                1L,
                plugin.getConfig().getLong("reminders.interval-minutes", 30L)
        ) * 60_000L;

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            for (String skill : plugin.getSkillManager().getSkillKeys()) {
                long expireAt = plugin.getSkillManager().getSkillExpire(uuid, skill);
                if (expireAt <= 0L) {
                    continue;
                }

                String key = key(uuid, skill);
                long remaining = expireAt - now;
                if (remaining <= 0L) {
                    TextUtil.send(
                            plugin,
                            player,
                            "messages.expired",
                            Map.of("skill", plugin.getSkillManager().getDisplayName(skill))
                    );
                    plugin.getSkillManager().expireSkill(uuid, skill);
                    lastReminder.remove(key);
                    sentWarnings.remove(key);
                    continue;
                }

                long last = lastReminder.computeIfAbsent(key, ignored -> now);
                if (now - last >= reminderInterval) {
                    TextUtil.send(
                            plugin,
                            player,
                            "messages.reminder",
                            Map.of(
                                    "skill", plugin.getSkillManager().getDisplayName(skill),
                                    "time", TextUtil.formatDuration(remaining)
                            )
                    );
                    lastReminder.put(key, now);
                }

                checkWarnings(player, skill, key, remaining);
            }
        }
    }

    private void checkWarnings(Player player, String skill, String key, long remaining) {
        Set<Integer> sent = sentWarnings.computeIfAbsent(key, ignored -> new HashSet<>());
        for (int minutes : plugin.getConfig().getIntegerList("reminders.warning-minutes")) {
            if (minutes <= 0 || sent.contains(minutes)) {
                continue;
            }
            long threshold = minutes * 60_000L;
            if (remaining <= threshold) {
                TextUtil.send(
                        plugin,
                        player,
                        "messages.warning",
                        Map.of(
                                "skill", plugin.getSkillManager().getDisplayName(skill),
                                "time", TextUtil.formatDuration(threshold)
                        )
                );
                sent.add(minutes);
            }
        }
    }

    private String key(UUID uuid, String skill) {
        return uuid + ":" + skill;
    }
}
