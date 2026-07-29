package id.velioragardens.velioravein.manager;

import id.velioragardens.velioravein.VelioraVein;
import id.velioragardens.velioravein.model.PlayerSkillData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class DataManager {
    private final VelioraVein plugin;
    private File file;
    private FileConfiguration config;
    private final Map<UUID, PlayerSkillData> cache = new HashMap<>();

    public DataManager(VelioraVein plugin) {
        this.plugin = plugin;
        init();
    }

    private void init() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }
        file = new File(plugin.getDataFolder(), "players.yml");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().log(Level.SEVERE, "Could not create players.yml", e);
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public PlayerSkillData getPlayerData(UUID uuid) {
        if (cache.containsKey(uuid)) {
            return cache.get(uuid);
        }

        String path = "players." + uuid.toString();
        if (config.contains(path)) {
            long vein = config.getLong(path + ".vein", 0);
            long tree = config.getLong(path + ".tree", 0);
            long farmer = config.getLong(path + ".farmer", 0);
            PlayerSkillData data = new PlayerSkillData(uuid, vein, tree, farmer);
            cache.put(uuid, data);
            return data;
        }

        PlayerSkillData data = new PlayerSkillData(uuid);
        cache.put(uuid, data);
        return data;
    }

    public void savePlayerData(PlayerSkillData data) {
        String path = "players." + data.getUuid().toString();
        config.set(path + ".vein", data.getVeinExpire());
        config.set(path + ".tree", data.getTreeExpire());
        config.set(path + ".farmer", data.getFarmerExpire());
        saveFile();
    }

    public void saveAll() {
        for (PlayerSkillData data : cache.values()) {
            String path = "players." + data.getUuid().toString();
            config.set(path + ".vein", data.getVeinExpire());
            config.set(path + ".tree", data.getTreeExpire());
            config.set(path + ".farmer", data.getFarmerExpire());
        }
        saveFile();
    }

    private void saveFile() {
        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save players.yml", e);
        }
    }
}
