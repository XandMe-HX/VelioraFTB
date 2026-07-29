package id.velioragardens.velioravein.manager;

import id.velioragardens.velioravein.VelioraVein;
import id.velioragardens.velioravein.model.PlayerSkillData;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

public class SkillManager {
    private final VelioraVein plugin;

    public SkillManager(VelioraVein plugin) {
        this.plugin = plugin;
    }

    public boolean purchaseSkill(Player player, String skillType) {
        double price = plugin.getConfig().getDouble("prices." + skillType, 10000.0);
        long durationMs = plugin.getConfig().getLong("duration.hours", 3) * 3600000L;

        if (!plugin.getEconomyManager().has(player, price)) {
            String msg = plugin.getMessagesConfig().getString("insufficient-funds", "&cUang tidak cukup! Butuh &e$%price%")
                    .replace("%price%", String.valueOf((int) price));
            
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(getPrefix() + msg));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return false;
        }

        if (plugin.getEconomyManager().withdraw(player, price)) {
            PlayerSkillData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
            if (data == null) return false; // NPE Prevention
            
            long currentExpire = getSkillExpire(player.getUniqueId(), skillType);
            long baseTime = Math.max(currentExpire, System.currentTimeMillis());
            long newExpire = baseTime + durationMs;

            setSkillExpire(data, skillType, newExpire);
            plugin.getDataManager().savePlayerData(data);

            SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH:mm");
            String dateStr = sdf.format(new Date(newExpire));
            
            String msg = plugin.getMessagesConfig().getString("shop.purchased", "&aBerhasil membeli skill %skill% sampai %expire%!")
                    .replace("%skill%", formatSkillName(skillType))
                    .replace("%expire%", dateStr);

            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(getPrefix() + msg));
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
            return true;
        }

        return false;
    }

    public long getSkillExpire(UUID uuid, String skillType) {
        PlayerSkillData data = plugin.getDataManager().getPlayerData(uuid);
        if (data == null) return 0L; // NPE Prevention
        
        return switch (skillType.toLowerCase()) {
            case "vein" -> data.getVeinExpire();
            case "tree" -> data.getTreeExpire();
            case "farmer" -> data.getFarmerExpire();
            default -> 0L;
        };
    }

    public void setSkillExpire(PlayerSkillData data, String skillType, long expire) {
        if (data == null) return;
        
        switch (skillType.toLowerCase()) {
            case "vein" -> data.setVeinExpire(expire);
            case "tree" -> data.setTreeExpire(expire);
            case "farmer" -> data.setFarmerExpire(expire);
        }
    }

    public boolean isSkillActive(UUID uuid, String skillType) {
        long expire = getSkillExpire(uuid, skillType);
        // Jika expire 0 atau di bawah current time = skill tidak aktif
        return expire > 0 && expire > System.currentTimeMillis();
    }

    public String formatSkillName(String skillType) {
        return switch (skillType.toLowerCase()) {
            case "vein" -> "Vein Miner";
            case "tree" -> "Tree Feller";
            case "farmer" -> "Farmer";
            default -> skillType;
        };
    }

    private String getPrefix() {
        return plugin.getMessagesConfig().getString("prefix", "&e[&bVelioraVein&e] &r");
    }
}