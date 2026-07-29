package id.velioragardens.velioraftb.listener;

import id.velioragardens.velioraftb.VelioraFTB;
import id.velioragardens.velioraftb.util.BlockScanner;
import id.velioragardens.velioraftb.util.MaterialUtil;
import id.velioragardens.velioraftb.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class FarmerListener implements Listener {
    private final VelioraFTB plugin;
    private final Set<String> processing = new HashSet<>();
    private final Set<String> pending = new HashSet<>();
    private final Map<UUID, Long> sneakNotice = new HashMap<>();

    public FarmerListener(VelioraFTB plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block start = event.getBlock();
        String startKey = blockKey(start);
        if (processing.contains(startKey)) {
            return;
        }
        if (pending.contains(startKey)) {
            event.setCancelled(true);
            return;
        }

        Player player = event.getPlayer();
        if (!plugin.getSkillManager().canUseSkill(player, "farmer")) {
            return;
        }

        Set<Material> cropsAllowed = MaterialUtil.readSet(plugin, "skills.farmer.allowed-crops");
        Set<Material> toolsAllowed = MaterialUtil.readSet(plugin, "skills.farmer.allowed-tools");
        if (!cropsAllowed.contains(start.getType())
                || !toolsAllowed.contains(player.getInventory().getItemInMainHand().getType())
                || !BlockScanner.isFullyGrown(start)) {
            return;
        }

        if (plugin.getConfig().getBoolean("skills.farmer.require-sneak", true)
                && !player.isSneaking()) {
            notifySneak(player);
            return;
        }

        int maxCrops = scanLimit(plugin.getConfig().getInt("skills.farmer.max-crops", 64));
        boolean diagonal = plugin.getConfig().getBoolean("skills.farmer.connect-diagonally", true);
        List<Block> crops = BlockScanner.scanCrops(start, maxCrops, cropsAllowed, diagonal);
        if (crops.isEmpty()) {
            return;
        }

        event.setCancelled(true);
        pending.add(startKey);
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            try {
                if (!player.isOnline() || !plugin.getSkillManager().canUseSkill(player, "farmer")) {
                    return;
                }
                boolean replant = plugin.getConfig().getBoolean("skills.farmer.replant", true);
                for (Block crop : crops) {
                    if (!cropsAllowed.contains(crop.getType())
                            || !BlockScanner.isFullyGrown(crop)
                            || !toolsAllowed.contains(player.getInventory().getItemInMainHand().getType())) {
                        continue;
                    }
                    Material cropType = crop.getType();
                    if (breakAsPlayer(player, crop) && replant) {
                        replant(crop, cropType);
                    }
                }
            } finally {
                pending.remove(startKey);
            }
        });
    }

    private boolean breakAsPlayer(Player player, Block block) {
        String key = blockKey(block);
        processing.add(key);
        try {
            return player.breakBlock(block);
        } finally {
            processing.remove(key);
        }
    }

    private void replant(Block block, Material cropType) {
        block.setType(cropType, false);
        if (block.getBlockData() instanceof Ageable cropData) {
            cropData.setAge(0);
            block.setBlockData(cropData, false);
        }
    }

    private int scanLimit(int configured) {
        int absolute = Math.max(1, plugin.getConfig().getInt("settings.absolute-scan-limit", 2048));
        return Math.max(1, Math.min(configured, absolute));
    }

    private void notifySneak(Player player) {
        long now = System.currentTimeMillis();
        if (now - sneakNotice.getOrDefault(player.getUniqueId(), 0L) < 3_000L) {
            return;
        }
        sneakNotice.put(player.getUniqueId(), now);
        TextUtil.send(
                plugin,
                player,
                "messages.sneak-required",
                Map.of("skill", plugin.getSkillManager().getDisplayName("farmer"))
        );
    }

    private String blockKey(Block block) {
        return block.getWorld().getUID() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ();
    }
}
