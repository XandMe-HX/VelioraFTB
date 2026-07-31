package id.velioragardens.velioraftb;

import id.velioragardens.velioraftb.command.FTBCommand;
import id.velioragardens.velioraftb.listener.FarmlandProtectionListener;
import id.velioragardens.velioraftb.gui.SkillGUI;
import id.velioragardens.velioraftb.listener.FarmerListener;
import id.velioragardens.velioraftb.listener.TreeListener;
import id.velioragardens.velioraftb.listener.VeinListener;
import id.velioragardens.velioraftb.manager.DataManager;
import id.velioragardens.velioraftb.manager.EconomyManager;
import id.velioragardens.velioraftb.manager.ReminderManager;
import id.velioragardens.velioraftb.manager.SkillManager;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class VelioraFTB extends JavaPlugin {

    private DataManager dataManager;
    private EconomyManager economyManager;
    private SkillManager skillManager;
    private ReminderManager reminderManager;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        dataManager = new DataManager(this);
        economyManager = new EconomyManager(this);
        skillManager = new SkillManager(this);
        reminderManager = new ReminderManager(this);

        if (getConfig().getBoolean("settings.require-vault-economy", true)
                && !economyManager.isEnabled()) {
            getLogger().severe("Vault ditemukan, tetapi provider Economy tidak tersedia.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        FTBCommand commandHandler = new FTBCommand(this);
        PluginCommand command = getCommand("velioraftb");
        if (command == null) {
            getLogger().severe("Command velioraftb tidak ditemukan di plugin.yml.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        command.setExecutor(commandHandler);
        command.setTabCompleter(commandHandler);

        getServer().getPluginManager().registerEvents(new SkillGUI(this), this);
        getServer().getPluginManager().registerEvents(new VeinListener(this), this);
        getServer().getPluginManager().registerEvents(new TreeListener(this), this);
        getServer().getPluginManager().registerEvents(new FarmerListener(this), this);
        getServer().getPluginManager().registerEvents(new FarmlandProtectionListener(), this);

        dataManager.startAutoSave();
        reminderManager.start();
        getLogger().info("VelioraFTB v" + getPluginMeta().getVersion() + " aktif.");
    }

    @Override
    public void onDisable() {
        if (reminderManager != null) {
            reminderManager.stop();
        }
        if (dataManager != null) {
            dataManager.shutdown();
        }
        getLogger().info("VelioraFTB nonaktif. Data pemain telah disimpan.");
    }

    public void reloadPlugin() {
        reloadConfig();
        dataManager.restartAutoSave();
        reminderManager.restart();
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
