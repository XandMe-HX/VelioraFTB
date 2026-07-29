package id.velioragardens.velioravein.command;

import id.velioragardens.velioravein.VelioraVein;
import id.velioragardens.velioravein.gui.SkillGUI;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class VeinCommand implements CommandExecutor {
    private final VelioraVein plugin;

    public VeinCommand(VelioraVein plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            plugin.reloadConfig();
            // Panggil method reload untuk messages.yml kalau kamu punya
            // plugin.reloadMessagesConfig(); 
            String prefix = plugin.getMessagesConfig().getString("prefix", "&e[&bVelioraVein&e] &r");
            sender.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(prefix + "&aConfig berhasil direload!"));
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage("Command ini hanya bisa dijalankan oleh Player di dalam game.");
            return true;
        }

        // Langsung buka GUI (akses tanpa permission) untuk command /vgvein atau /vgvein gui
        SkillGUI.open(plugin, player);
        return true;
    }
}