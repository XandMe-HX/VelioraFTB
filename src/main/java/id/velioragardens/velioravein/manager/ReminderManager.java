package id.velioragardens.velioravein.manager;

import id.velioragardens.velioravein.VelioraVein;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

public class ReminderManager {
    private final VelioraVein plugin;
    private BukkitTask task;

    private final Map<UUID, Set<String>> warned10 = new HashMap<>();
    private final Map<UUID, Set<String>> warned5 = new HashMap<>();
    private final Map<UUID, Set<String>> warned1 = new HashMap<>();
    private final Map<UUID, Set<String>> warnedExpired = new HashMap<>();

    private long lastIntervalCheck = System.currentTimeMillis();

    public ReminderManager(VelioraVein plugin) {
        this.plugin = plugin;
        start();
    }

    public void start() {
        long intervalTicks = 20L * 60L; // Check every 1 minute
        task = new BukkitRunnable() {
            @Override
            public void run() {
                checkReminders();
            }
        }.runTaskTimer(plugin, 120L, intervalTicks); // Delay 6 seconds, run every 60 seconds
    }

    public void stop() {
        if (task != null) {
            task.cancel();
        }
    }

    private void checkReminders() {
        long now = System.currentTimeMillis();
        boolean isIntervalReminderTime = false;
        long intervalConfigMs = plugin.getConfig().getLong("reminder.interval-minutes", 20) * 60000L;

        if (now - lastIntervalCheck >= intervalConfigMs) {
            isIntervalReminderTime = true;
            lastIntervalCheck = now;
        }

        String prefix = plugin.getMessagesConfig().getString("prefix", "&e[&bVelioraVein&e] &r");

        for (Player player : Bukkit.getOnlinePlayers()) {
            UUID uuid = player.getUniqueId();
            String[] skills = {"vein", "tree", "farmer"};

            for (String skill : skills) {
                // FIX: Gunakan uuid, bukan data
                long expire = plugin.getSkillManager().getSkillExpire(uuid, skill);
                if (expire <= 0) continue;

                long remainingMs = expire - now;
                String skillName = plugin.getSkillManager().formatSkillName(skill);

                if (remainingMs > 0) {
                    // Reset expired warning if skill was renewed
                    getWarnSet(warnedExpired, uuid).remove(skill);

                    // 1 minute warning
                    if (remainingMs <= 60000L) {
                        if (!getWarnSet(warned1, uuid).contains(skill)) {
                            getWarnSet(warned1, uuid).add(skill);
                            String msg = plugin.getMessagesConfig().getString("reminder-warning-1m", "&ePeringatan! Skill &b%skill% &eaktif tinggal &c1 menit &elagi!")
                                    .replace("%skill%", skillName);
                            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(prefix + msg));
                        }
                    }
                    // 5 minute warning
                    else if (remainingMs <= 300000L) {
                        if (!getWarnSet(warned5, uuid).contains(skill)) {
                            getWarnSet(warned5, uuid).add(skill);
                            String msg = plugin.getMessagesConfig().getString("reminder-warning-5m", "&ePeringatan! Skill &b%skill% &eaktif tinggal &c5 menit &elagi!")
                                    .replace("%skill%", skillName);
                            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(prefix + msg));
                        }
                    }
                    // 10 minute warning
                    else if (remainingMs <= 600000L) {
                        if (!getWarnSet(warned10, uuid).contains(skill)) {
                            getWarnSet(warned10, uuid).add(skill);
                            String msg = plugin.getMessagesConfig().getString("reminder-warning-10m", "&ePeringatan! Skill &b%skill% &eaktif tinggal &c10 menit &elagi!")
                                    .replace("%skill%", skillName);
                            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(prefix + msg));
                        }
                    }

                    // Reset warnings if skill renewed or has more time
                    if (remainingMs > 600000L) {
                        getWarnSet(warned10, uuid).remove(skill);
                        getWarnSet(warned5, uuid).remove(skill);
                        getWarnSet(warned1, uuid).remove(skill);
                    } else if (remainingMs > 300000L) {
                        getWarnSet(warned5, uuid).remove(skill);
                        getWarnSet(warned1, uuid).remove(skill);
                    } else if (remainingMs > 60000L) {
                        getWarnSet(warned1, uuid).remove(skill);
                    }

                    // General interval reminder (every 20 mins)
                    if (isIntervalReminderTime) {
                        long remMin = remainingMs / 60000L;
                        String timeStr = remMin > 60 ? (remMin / 60) + " Jam " + (remMin % 60) + " Menit" : remMin + " Menit";
                        String msg = plugin.getMessagesConfig().getString("reminder-interval", "&ePemberitahuan: Skill &b%skill% &eaktif &d%time% &elagi.")
                                .replace("%skill%", skillName)
                                .replace("%time%", timeStr);
                        player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(prefix + msg));
                    }
                } else {
                    // Just expired
                    if (!getWarnSet(warnedExpired, uuid).contains(skill)) {
                        getWarnSet(warnedExpired, uuid).add(skill);
                        // Clean up other warning flags
                        getWarnSet(warned10, uuid).remove(skill);
                        getWarnSet(warned5, uuid).remove(skill);
                        getWarnSet(warned1, uuid).remove(skill);

                        String msg = plugin.getMessagesConfig().getString("skill-expired", "&cSkill &b%skill% &ckamu telah berakhir!")
                                .replace("%skill%", skillName);
                        player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(prefix + msg));
                    }
                }
            }
        }
    }

    private Set<String> getWarnSet(Map<UUID, Set<String>> map, UUID uuid) {
        return map.computeIfAbsent(uuid, k -> new HashSet<>());
    }
}