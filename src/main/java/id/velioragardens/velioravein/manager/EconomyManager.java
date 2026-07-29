package id.velioragardens.velioravein.manager;

import id.velioragardens.velioravein.VelioraVein;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class EconomyManager {
    private final VelioraVein plugin;
    private Economy econ = null;

    public EconomyManager(VelioraVein plugin) {
        this.plugin = plugin;
        setupEconomy();
    }

    private boolean setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }

    public boolean isEnabled() {
        return econ != null;
    }

    public double getBalance(Player player) {
        if (!isEnabled()) return 0;
        return econ.getBalance(player);
    }

    public boolean has(Player player, double amount) {
        if (!isEnabled()) return false;
        return econ.has(player, amount);
    }

    public boolean withdraw(Player player, double amount) {
        if (!isEnabled()) return false;
        return econ.withdrawPlayer(player, amount).transactionSuccess();
    }
}