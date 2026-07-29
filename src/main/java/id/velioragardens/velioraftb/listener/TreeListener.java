package id.velioragardens.velioraftb.listener;

import id.velioragardens.velioraftb.VelioraFTB;
import id.velioragardens.velioraftb.util.BlockScanner;
import id.velioragardens.velioraftb.util.MaterialUtil;
import id.velioragardens.velioraftb.util.TextUtil;
import org.bukkit.Material;
import org.bukkit.block.Block;
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

public final class TreeListener implements Listener {
    private final VelioraFTB plugin;
    private final Set<String> processing = new HashSet<>();
    private final Set<String> pending = new HashSet<>();
    private final Map<UUID, Long> sneakNotice = new HashMap<>();

    public TreeListener(VelioraFTB plugin) {
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
        if (!plugin.getSkillManager().canUseSkill(player, "tree")) {
            return;
        }

        Set<Material> logsAllowed = MaterialUtil.readSet(plugin, "skills.tree.allowed-logs");
        Set<Material> leavesAllowed = MaterialUtil.readSet(plugin, "skills.tree.allowed-leaves");
        Set<Material> toolsAllowed = MaterialUtil.readSet(plugin, "skills.tree.allowed-tools");
        if (!logsAllowed.contains(start.getType())
                || !toolsAllowed.contains(player.getInventory().getItemInMainHand().getType())) {
            return;
        }

        if (plugin.getConfig().getBoolean("skills.tree.require-sneak", true)
                && !player.isSneaking()) {
            notifySneak(player);
            return;
        }

        int maxLogs = scanLimit(plugin.getConfig().getInt("skills.tree.max-logs", 128));
        int horizontal = Math.max(
                1,
                plugin.getConfig().getInt("skills.tree.max-horizontal-distance", 7)
        );
        int vertical = Math.max(
                1,
                plugin.getConfig().getInt("skills.tree.max-vertical-distance", 48)
        );
        List<Block> logs = BlockScanner.scanTreeLogs(
                start,
                maxLogs,
                horizontal,
                vertical,
                logsAllowed
        );
        if (logs.isEmpty()) {
            return;
        }

        int leafRadius = Math.max(1, plugin.getConfig().getInt("skills.tree.leaf-radius", 4));
        int maxLeaves = scanLimit(plugin.getConfig().getInt("skills.tree.max-leaves", 512));
        boolean onlyNatural = plugin.getConfig().getBoolean(
                "skills.tree.only-natural-leaves",
                true
        );
        List<Block> leaves = BlockScanner.scanLeaves(
                logs,
                leafRadius,
                maxLeaves,
                leavesAllowed,
                onlyNatural
        );

        if (plugin.getConfig().getBoolean("skills.tree.require-natural-leaves", true)
                && leaves.isEmpty()) {
            return;
        }

        event.setCancelled(true);
        pending.add(startKey);
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            try {
                if (!player.isOnline() || !plugin.getSkillManager().canUseSkill(player, "tree")) {
                    return;
                }
                for (Block log : logs) {
                    if (!logsAllowed.contains(log.getType())
                            || !toolsAllowed.contains(player.getInventory().getItemInMainHand().getType())) {
                        break;
                    }
                    breakAsPlayer(player, log);
                }
                scheduleLeaves(player, leaves, leavesAllowed);
            } finally {
                pending.remove(startKey);
            }
        });
    }

    private void scheduleLeaves(Player player, List<Block> leaves, Set<Material> leavesAllowed) {
        if (!plugin.getConfig().getBoolean("skills.tree.break-leaves", true)) {
            return;
        }

        long delay = Math.max(0L, plugin.getConfig().getLong("skills.tree.leaf-delay-ticks", 2L));
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            boolean drops = plugin.getConfig().getBoolean("skills.tree.leaf-drops", true);
            for (Block leaf : leaves) {
                if (!leavesAllowed.contains(leaf.getType())) {
                    continue;
                }

                String key = blockKey(leaf);
                processing.add(key);
                try {
                    BlockBreakEvent leafEvent = new BlockBreakEvent(leaf, player);
                    plugin.getServer().getPluginManager().callEvent(leafEvent);
                    if (leafEvent.isCancelled()) {
                        continue;
                    }
                    if (drops) {
                        leaf.breakNaturally();
                    } else {
                        leaf.setType(Material.AIR, false);
                    }
                } finally {
                    processing.remove(key);
                }
            }
        }, delay);
    }

    private void breakAsPlayer(Player player, Block block) {
        String key = blockKey(block);
        processing.add(key);
        try {
            player.breakBlock(block);
        } finally {
            processing.remove(key);
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
                Map.of("skill", plugin.getSkillManager().getDisplayName("tree"))
        );
    }

    private String blockKey(Block block) {
        return block.getWorld().getUID() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ();
    }
}
