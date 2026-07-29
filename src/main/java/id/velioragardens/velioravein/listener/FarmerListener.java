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

public class FarmerListener implements Listener {
    private final VelioraVein plugin;
    private final Set<Block> breaking = new HashSet<>();

    public FarmerListener(VelioraVein plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (breaking.contains(block)) return;

        Player player = event.getPlayer();
        if (!plugin.getSkillManager().isSkillActive(player.getUniqueId(), "farmer")) {
            return;
        }

        boolean requireSneak = plugin.getConfig().getBoolean("require-sneak", true);
        if (requireSneak && !player.isSneaking()) {
            return;
        }

        ItemStack tool = player.getInventory().getItemInMainHand();
        if (!isHoe(tool.getType())) {
            return;
        }

        Material type = block.getType();
        if (!isCrop(type)) {
            return;
        }

        if (!BlockScanner.isFullyGrown(block)) {
            return;
        }

        int maxBlocks = plugin.getConfig().getInt("max-blocks", 64);
        List<Block> crops = BlockScanner.scanCrops(block, maxBlocks);
        if (crops.isEmpty()) return;

        breaking.add(block);

        int cropsBroken = 0;
        for (Block crop : crops) {
            Material cropType = crop.getType();
            breaking.add(crop);
            
            crop.breakNaturally(tool);
            
            crop.setType(cropType);
            if (crop.getBlockData() instanceof org.bukkit.block.data.Ageable ageable) {
                ageable.setAge(0);
                crop.setBlockData(ageable);
            }
            
            breaking.remove(crop);
            cropsBroken++;
        }

        breaking.remove(block);

        if (player.getGameMode() != GameMode.CREATIVE && cropsBroken > 0) {
            damageTool(player, tool, cropsBroken);
        }
    }

    private boolean isHoe(Material material) {
        return material.name().endsWith("_HOE");
    }

    private boolean isCrop(Material material) {
        return switch (material) {
            case WHEAT, POTATOES, CARROTS, BEETROOTS, NETHER_WART -> true;
            default -> false;
        };
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
