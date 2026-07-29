package id.velioragardens.velioraftb.manager;

import id.velioragardens.velioraftb.VelioraFTB;
import id.velioragardens.velioraftb.model.PlayerSkillData;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public final class DataManager {
    private final VelioraFTB plugin;
    private final Map<UUID, PlayerSkillData> cache = new HashMap<>();
    private File file;
    private FileConfiguration config;
    private BukkitTask autoSaveTask;

    public DataManager(VelioraFTB plugin) {
        this.plugin = plugin;
        initialize();
    }

    private void initialize() {
        if (!plugin.getDataFolder().exists() && !plugin.getDataFolder().mkdirs()) {
            plugin.getLogger().warning("Folder data VelioraFTB tidak dapat dibuat.");
        }

        file = new File(plugin.getDataFolder(), "players.yml");
        migrateLegacyData();

        if (!file.exists()) {
            try {
                if (!file.createNewFile()) {
                    plugin.getLogger().warning("players.yml tidak dapat dibuat.");
                }
            } catch (IOException exception) {
                plugin.getLogger().log(Level.SEVERE, "Gagal membuat players.yml.", exception);
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    private void migrateLegacyData() {
        if (file.exists() || !plugin.getConfig().getBoolean("data.migrate-legacy-folder", true)) {
            return;
        }

        String legacyName = plugin.getConfig().getString("data.legacy-folder-name", "VelioraVein");
        File pluginsFolder = plugin.getDataFolder().getParentFile();
        File legacyFile = new File(new File(pluginsFolder, legacyName), "players.yml");
        if (!legacyFile.isFile()) {
            return;
        }

        try {
            Files.copy(legacyFile.toPath(), file.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
            plugin.getLogger().info("Data players.yml lama berhasil dimigrasikan dari " + legacyName + ".");
        } catch (IOException exception) {
            plugin.getLogger().log(Level.WARNING, "Data lama gagal dimigrasikan.", exception);
        }
    }

    public PlayerSkillData getPlayerData(UUID uuid) {
        return cache.computeIfAbsent(uuid, this::loadPlayerData);
    }

    private PlayerSkillData loadPlayerData(UUID uuid) {
        String path = "players." + uuid;
        return new PlayerSkillData(
                uuid,
                config.getLong(path + ".vein", 0L),
                config.getLong(path + ".tree", 0L),
                config.getLong(path + ".farmer", 0L)
        );
    }

    public void savePlayerData(PlayerSkillData data) {
        writeToConfig(data);
        saveFile();
    }

    public void saveAll() {
        for (PlayerSkillData data : cache.values()) {
            writeToConfig(data);
        }
        saveFile();
    }

    private void writeToConfig(PlayerSkillData data) {
        String path = "players." + data.getUuid();
        config.set(path + ".vein", data.getVeinExpire());
        config.set(path + ".tree", data.getTreeExpire());
        config.set(path + ".farmer", data.getFarmerExpire());
    }

    private void saveFile() {
        try {
            config.save(file);
        } catch (IOException exception) {
            plugin.getLogger().log(Level.SEVERE, "Gagal menyimpan players.yml.", exception);
        }
    }

    public void startAutoSave() {
        stopAutoSave();
        long minutes = Math.max(1L, plugin.getConfig().getLong("data.auto-save-minutes", 5L));
        long ticks = minutes * 60L * 20L;
        autoSaveTask = plugin.getServer().getScheduler()
                .runTaskTimer(plugin, this::saveAll, ticks, ticks);
    }

    public void restartAutoSave() {
        startAutoSave();
    }

    private void stopAutoSave() {
        if (autoSaveTask != null) {
            autoSaveTask.cancel();
            autoSaveTask = null;
        }
    }

    public void shutdown() {
        stopAutoSave();
        saveAll();
    }
}
