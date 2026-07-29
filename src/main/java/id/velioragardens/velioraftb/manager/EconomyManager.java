package id.velioragardens.velioraftb.manager;

import id.velioragardens.velioraftb.VelioraFTB;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

public final class EconomyManager {
    private final VelioraFTB plugin;
    private Economy economy;

    public EconomyManager(VelioraFTB plugin) {
        this.plugin = plugin;
        setupEconomy();
    }

    private void setupEconomy() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            return;
        }

        RegisteredServiceProvider<Economy> registration =
                plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (registration != null) {
            economy = registration.getProvider();
        }
    }

    public boolean isEnabled() {
        return economy != null;
    }

    public boolean has(OfflinePlayer player, double amount) {
        return economy != null && economy.has(player, amount);
    }

    public boolean withdraw(OfflinePlayer player, double amount) {
        if (economy == null) {
            return false;
        }
        EconomyResponse response = economy.withdrawPlayer(player, amount);
        if (!response.transactionSuccess()) {
            plugin.getLogger().warning(
                    "Transaksi Vault gagal untuk " + player.getName() + ": " + response.errorMessage
            );
        }
        return response.transactionSuccess();
    }
}
