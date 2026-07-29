package id.velioragardens.velioraftb.manager;

import id.velioragardens.velioraftb.VelioraFTB;
import id.velioragardens.velioraftb.model.PlayerSkillData;
import id.velioragardens.velioraftb.util.TextUtil;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class SkillManager {
    private static final List<String> SKILL_KEYS = List.of("vein", "tree", "farmer");
    private final VelioraFTB plugin;

    public SkillManager(VelioraFTB plugin) {
        this.plugin = plugin;
    }

    public boolean purchaseSkill(Player player, String rawSkill) {
        String skill = normalize(rawSkill);
        if (!SKILL_KEYS.contains(skill)) {
            return false;
        }

        String displayName = getDisplayName(skill);
        if (!isEnabled(skill)) {
            TextUtil.send(plugin, player, "messages.skill-disabled", Map.of("skill", displayName));
            return false;
        }

        if (!hasSkillPermission(player, skill)) {
            TextUtil.send(plugin, player, "messages.no-permission");
            return false;
        }

        if (isSkillActive(player.getUniqueId(), skill)) {
            TextUtil.send(
                    plugin,
                    player,
                    "messages.already-active",
                    Map.of(
                            "skill", displayName,
                            "time", getRemainingFormatted(player.getUniqueId(), skill)
                    )
            );
            return false;
        }

        if (!plugin.getEconomyManager().isEnabled()) {
            TextUtil.send(plugin, player, "messages.vault-unavailable");
            return false;
        }

        double price = getPrice(skill);
        if (!plugin.getEconomyManager().has(player, price)) {
            TextUtil.send(
                    plugin,
                    player,
                    "messages.insufficient-funds",
                    Map.of("price", TextUtil.formatMoney(price))
            );
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0F, 1.0F);
            return false;
        }

        if (!plugin.getEconomyManager().withdraw(player, price)) {
            TextUtil.send(plugin, player, "messages.vault-unavailable");
            return false;
        }

        long duration = getDurationMillis(skill);
        PlayerSkillData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        setSkillExpire(data, skill, System.currentTimeMillis() + duration);
        plugin.getDataManager().savePlayerData(data);
        plugin.getReminderManager().reset(player.getUniqueId(), skill);

        TextUtil.send(
                plugin,
                player,
                "messages.purchased",
                Map.of(
                        "skill", displayName,
                        "duration", TextUtil.formatDuration(duration),
                        "time", TextUtil.formatDuration(duration),
                        "price", TextUtil.formatMoney(price)
                )
        );
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
        return true;
    }

    public boolean canUseSkill(Player player, String skill) {
        return isEnabled(skill)
                && hasSkillPermission(player, skill)
                && isWorldEnabled(player.getWorld())
                && isSkillActive(player.getUniqueId(), skill);
    }

    public boolean isEnabled(String skill) {
        return plugin.getConfig().getBoolean("skills." + normalize(skill) + ".enabled", true);
    }

    public boolean hasSkillPermission(Player player, String skill) {
        String permission = plugin.getConfig().getString(
                "skills." + normalize(skill) + ".permission",
                "velioraftb.skill." + normalize(skill)
        );
        return permission == null || permission.isBlank() || player.hasPermission(permission);
    }

    public boolean isWorldEnabled(World world) {
        String current = world.getName().toLowerCase(Locale.ROOT);
        Set<String> disabled = plugin.getConfig().getStringList("worlds.disabled-worlds")
                .stream()
                .map(value -> value.toLowerCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toSet());
        if (disabled.contains(current)) {
            return false;
        }

        List<String> enabled = plugin.getConfig().getStringList("worlds.enabled-worlds");
        return enabled.isEmpty() || enabled.stream().anyMatch(value -> value.equalsIgnoreCase(current));
    }

    public boolean isSkillActive(UUID uuid, String skill) {
        return getSkillExpire(uuid, skill) > System.currentTimeMillis();
    }

    public long getSkillExpire(UUID uuid, String rawSkill) {
        PlayerSkillData data = plugin.getDataManager().getPlayerData(uuid);
        return switch (normalize(rawSkill)) {
            case "vein" -> data.getVeinExpire();
            case "tree" -> data.getTreeExpire();
            case "farmer" -> data.getFarmerExpire();
            default -> 0L;
        };
    }

    public void expireSkill(UUID uuid, String rawSkill) {
        PlayerSkillData data = plugin.getDataManager().getPlayerData(uuid);
        setSkillExpire(data, rawSkill, 0L);
        plugin.getDataManager().savePlayerData(data);
    }

    private void setSkillExpire(PlayerSkillData data, String rawSkill, long expireAt) {
        switch (normalize(rawSkill)) {
            case "vein" -> data.setVeinExpire(expireAt);
            case "tree" -> data.setTreeExpire(expireAt);
            case "farmer" -> data.setFarmerExpire(expireAt);
            default -> {
            }
        }
    }

    public long getDurationMillis(String skill) {
        long minutes = Math.max(
                1L,
                plugin.getConfig().getLong("skills." + normalize(skill) + ".duration-minutes", 180L)
        );
        return minutes * 60_000L;
    }

    public double getPrice(String skill) {
        return Math.max(
                0D,
                plugin.getConfig().getDouble("skills." + normalize(skill) + ".price", 10_000D)
        );
    }

    public String getDisplayName(String skill) {
        String normalized = normalize(skill);
        return plugin.getConfig().getString(
                "skills." + normalized + ".display-name",
                switch (normalized) {
                    case "vein" -> "Vein Miner";
                    case "tree" -> "Tree Feller";
                    case "farmer" -> "Farmer";
                    default -> normalized;
                }
        );
    }

    public String getRemainingFormatted(UUID uuid, String skill) {
        return TextUtil.formatDuration(getSkillExpire(uuid, skill) - System.currentTimeMillis());
    }

    public List<String> getSkillKeys() {
        return SKILL_KEYS;
    }

    private String normalize(String skill) {
        return skill == null ? "" : skill.toLowerCase(Locale.ROOT);
    }
}
