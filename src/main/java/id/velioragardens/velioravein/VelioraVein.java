package id.velioragardens.velioravein;

import id.velioragardens.velioravein.command.VeinCommand;
import id.velioragardens.velioravein.gui.SkillGUI;
import id.velioragardens.velioravein.listener.FarmerListener;
import id.velioragardens.velioravein.listener.TreeListener;
import id.velioragardens.velioravein.listener.VeinListener;
import id.velioragardens.velioravein.manager.DataManager;
import id.velioragardens.velioravein.manager.EconomyManager;
import id.velioragardens.velioravein.manager.ReminderManager;
import id.velioragardens.velioravein.manager.SkillManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class VelioraVein extends JavaPlugin {

    private DataManager dataManager;
    private EconomyManager economyManager;
    private SkillManager skillManager;
    private ReminderManager reminderManager;

    private File messagesFile;
    private FileConfiguration messagesConfig;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveDefaultMessages();
        loadMessagesConfig();

        this.dataManager = new DataManager(this);
        this.economyManager = new EconomyManager(this);
        this.skillManager = new SkillManager(this);
        this.reminderManager = new ReminderManager(this);

        // --- INI BAGIAN YANG DIPERBAIKI ---
        // Sesuai dengan plugin.yml, command yang kita daftarkan adalah "vgvein"
        if (getCommand("vgvein") != null) {
            getCommand("vgvein").setExecutor(new VeinCommand(this));
        } else {
            getLogger().severe("Gagal mendaftarkan command 'vgvein'! Pastikan nama di plugin.yml sudah benar.");
        }
        // ------------------------------------

        getServer().getPluginManager().registerEvents(new SkillGUI(this), this);
        getServer().getPluginManager().registerEvents(new VeinListener(this), this);
        getServer().getPluginManager().registerEvents(new TreeListener(this), this);
        getServer().getPluginManager().registerEvents(new FarmerListener(this), this);

        getLogger().info("VelioraVein v" + getDescription().getVersion() + " has been successfully enabled!");
    }

    @Override
    public void onDisable() {
        if (reminderManager != null) {
            reminderManager.stop();
        }
        if (dataManager != null) {
            dataManager.saveAll();
        }
        getLogger().info("VelioraVein has been disabled. All player data has been saved.");
    }

    private void saveDefaultMessages() {
        messagesFile = new File(getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            saveResource("messages.yml", false);
        }
    }

    public void loadMessagesConfig() {
        messagesFile = new File(getDataFolder(), "messages.yml");
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public FileConfiguration getMessagesConfig() {
        if (messagesConfig == null) {
            loadMessagesConfig();
        }
        return messagesConfig;
    }

    public DataManager getDataManager() {
        return dataManager;
    }

    public EconomyManager getEconomyManager() {
        return economyManager;
    }

    public SkillManager getSkillManager() {
        return skillManager;
    }

    public ReminderManager getReminderManager() {
        return reminderManager;
    }
}