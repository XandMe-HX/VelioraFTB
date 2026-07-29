package id.velioragardens.velioravein.listener;

import id.velioragardens.velioravein.VelioraVein;
import id.velioragardens.velioravein.util.BlockScanner;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TreeListener implements Listener {
    private final VelioraVein plugin;
    private final Set<Block> breaking = new HashSet<>();

    public TreeListener(VelioraVein plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (breaking.contains(block)) return;

        Player player = event.getPlayer();
        if (!plugin.getSkillManager().isSkillActive(player.getUniqueId(), "tree")) {
            return;
        }

        boolean requireSneak = plugin.getConfig().getBoolean("require-sneak", true);
        if (requireSneak && !player.isSneaking()) {
            return;
        }

        ItemStack tool = player.getInventory().getItemInMainHand();
        if (!isAxe(tool.getType())) {
            return;
        }

        Material type = block.getType();
        if (!isLog(type)) {
            return;
        }

        int maxBlocks = plugin.getConfig().getInt("max-blocks", 64);
        List<Block> logs = BlockScanner.scanTreeLogs(block, maxBlocks);
        if (logs.isEmpty()) return;

        breaking.add(block);

        int logsBroken = 0;
        for (Block log : logs) {
            if (log.equals(block)) continue;
            breaking.add(log);
            log.breakNaturally(tool);
            breaking.remove(log);
            logsBroken++;
        }

        List<Block> leaves = BlockScanner.scanLeaves(logs, 4);
        for (Block leaf : leaves) {
            breaking.add(leaf);
            leaf.breakNaturally();
            breaking.remove(leaf);
        }

        breaking.remove(block);

        if (player.getGameMode() != GameMode.CREATIVE && logsBroken > 0) {
            damageTool(player, tool, logsBroken);
        }
    }

    private boolean isAxe(Material material) {
        return material.name().endsWith("_AXE");
    }

    private boolean isLog(Material material) {
        String name = material.name();
        return name.endsWith("_LOG") || name.endsWith("_WOOD") || name.endsWith("_STEM") || name.equals("MANGROVE_ROOTS");
    }

    private void damageTool(Player player, ItemStack tool, int amount) {
        if (!(tool.getItemMeta() instanceof Damageable damageable)) return;

        int level = tool.getEnchantmentLevel(Enchantment.UNBREAKING);
        int finalAmount = 0;
        for (int i = 0; i < amount; i++) {
            if (Math.random() < (1.0 / (level + 1))) {
                finalAmount++;
            }
        }

        if (finalAmount > 0) {
            int newDamage = damageable.getDamage() + finalAmount;
            if (newDamage >= tool.getType().getMaxDurability()) {
                player.getInventory().setItemInMainHand(null);
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
            } else {
                damageable.setDamage(newDamage);
                tool.setItemMeta(damageable);
            }
        }
    }
}
