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

public final class VeinListener implements Listener {
    private final VelioraFTB plugin;
    private final Set<String> processing = new HashSet<>();
    private final Set<String> pending = new HashSet<>();
    private final Map<UUID, Long> sneakNotice = new HashMap<>();

    public VeinListener(VelioraFTB plugin) {
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
        if (!plugin.getSkillManager().canUseSkill(player, "vein")) {
            return;
        }

        Set<Material> ores = MaterialUtil.readSet(plugin, "skills.vein.allowed-blocks");
        Set<Material> tools = MaterialUtil.readSet(plugin, "skills.vein.allowed-tools");
        if (!ores.contains(start.getType())
                || !tools.contains(player.getInventory().getItemInMainHand().getType())) {
            return;
        }

        if (plugin.getConfig().getBoolean("skills.vein.require-sneak", true)
                && !player.isSneaking()) {
            notifySneak(player);
            return;
        }

        int maxBlocks = scanLimit(plugin.getConfig().getInt("skills.vein.max-blocks", 64));
        boolean diagonal = plugin.getConfig().getBoolean("skills.vein.connect-diagonally", true);
        List<Block> vein = BlockScanner.scanVein(start, maxBlocks, ores, diagonal);
        if (vein.size() <= 1) {
            return;
        }

        event.setCancelled(true);
        pending.add(startKey);
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            try {
                if (!player.isOnline() || !plugin.getSkillManager().canUseSkill(player, "vein")) {
                    return;
                }
                for (Block ore : vein) {
                    if (!ores.contains(ore.getType())
                            || !tools.contains(player.getInventory().getItemInMainHand().getType())) {
                        break;
                    }
                    breakAsPlayer(player, ore);
                }
            } finally {
                pending.remove(startKey);
            }
        });
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
                Map.of("skill", plugin.getSkillManager().getDisplayName("vein"))
        );
    }

    private String blockKey(Block block) {
        return block.getWorld().getUID() + ":" + block.getX() + ":" + block.getY() + ":" + block.getZ();
    }
}
