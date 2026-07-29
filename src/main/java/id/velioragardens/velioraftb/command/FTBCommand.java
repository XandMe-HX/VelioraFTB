package id.velioragardens.velioraftb.command;

import id.velioragardens.velioraftb.VelioraFTB;
import id.velioragardens.velioraftb.gui.SkillGUI;
import id.velioragardens.velioraftb.util.TextUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class FTBCommand implements CommandExecutor, TabCompleter {
    private final VelioraFTB plugin;

    public FTBCommand(VelioraFTB plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String label,
            @NotNull String[] args
    ) {
        if (args.length == 0 || args[0].equalsIgnoreCase("menu")) {
            if (!(sender instanceof Player player)) {
                TextUtil.send(plugin, sender, "messages.player-only");
                return true;
            }
            SkillGUI.open(plugin, player);
            return true;
        }

        if (args[0].equalsIgnoreCase("status")) {
            if (!(sender instanceof Player player)) {
                TextUtil.send(plugin, sender, "messages.player-only");
                return true;
            }
            showStatus(player);
            return true;
        }

        if (args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("velioraftb.admin")) {
                TextUtil.send(plugin, sender, "messages.no-permission");
                return true;
            }
            plugin.reloadPlugin();
            TextUtil.send(plugin, sender, "messages.reloaded");
            return true;
        }

        TextUtil.send(plugin, sender, "messages.invalid-command");
        return true;
    }

    private void showStatus(Player player) {
        TextUtil.send(plugin, player, "messages.status-header");
        for (String skill : plugin.getSkillManager().getSkillKeys()) {
            boolean active = plugin.getSkillManager().isSkillActive(player.getUniqueId(), skill);
            String status = active
                    ? TextUtil.replace(
                            plugin.getConfig().getString("messages.status-active", "&aaktif, tersisa {time}"),
                            Map.of("time", plugin.getSkillManager().getRemainingFormatted(player.getUniqueId(), skill)))
                    : plugin.getConfig().getString("messages.status-inactive", "&ctidak aktif");

            TextUtil.send(
                    plugin,
                    player,
                    "messages.status-line",
                    Map.of(
                            "skill", plugin.getSkillManager().getDisplayName(skill),
                            "status", status
                    )
            );
        }
    }

    @Override
    public List<String> onTabComplete(
            @NotNull CommandSender sender,
            @NotNull Command command,
            @NotNull String alias,
            @NotNull String[] args
    ) {
        if (args.length != 1) {
            return List.of();
        }

        List<String> suggestions = new ArrayList<>(List.of("menu", "status"));
        if (sender.hasPermission("velioraftb.admin")) {
            suggestions.add("reload");
        }
        String typed = args[0].toLowerCase();
        return suggestions.stream().filter(value -> value.startsWith(typed)).toList();
    }
}
