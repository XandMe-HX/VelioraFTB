package id.velioragardens.velioraftb.gui;

import id.velioragardens.velioraftb.VelioraFTB;
import id.velioragardens.velioraftb.util.MaterialUtil;
import id.velioragardens.velioraftb.util.TextUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SkillGUI implements Listener {
    private final VelioraFTB plugin;

    public SkillGUI(VelioraFTB plugin) {
        this.plugin = plugin;
    }

    public static void open(VelioraFTB plugin, Player player) {
        int size = normalizeSize(plugin.getConfig().getInt("gui.size", 9));
        SkillMenuHolder holder = new SkillMenuHolder();
        Inventory inventory = Bukkit.createInventory(
                holder,
                size,
                TextUtil.color(plugin.getConfig().getString("gui.title", "&8VelioraFTB Skill Shop"))
        );
        holder.setInventory(inventory);

        fillBackground(plugin, inventory);
        for (String skill : plugin.getSkillManager().getSkillKeys()) {
            int slot = plugin.getConfig().getInt("gui.slots." + skill, defaultSlot(skill));
            if (slot < 0 || slot >= size) {
                plugin.getLogger().warning("Slot GUI " + skill + " berada di luar ukuran inventory: " + slot);
                continue;
            }
            inventory.setItem(slot, createSkillItem(plugin, player, skill));
            holder.register(slot, skill);
        }
        player.openInventory(inventory);
    }

    private static void fillBackground(VelioraFTB plugin, Inventory inventory) {
        if (!plugin.getConfig().getBoolean("gui.filler.enabled", true)) {
            return;
        }

        Material material = MaterialUtil.read(
                plugin,
                "gui.filler.material",
                Material.BLACK_STAINED_GLASS_PANE
        );
        ItemStack filler = new ItemStack(material);
        ItemMeta meta = filler.getItemMeta();
        if (meta != null) {
            meta.displayName(TextUtil.color(plugin.getConfig().getString("gui.filler.name", " ")));
            filler.setItemMeta(meta);
        }

        for (int slot = 0; slot < inventory.getSize(); slot++) {
            inventory.setItem(slot, filler);
        }
    }

    private static ItemStack createSkillItem(VelioraFTB plugin, Player player, String skill) {
        Material fallback = switch (skill) {
            case "tree" -> Material.DIAMOND_AXE;
            case "farmer" -> Material.DIAMOND_HOE;
            default -> Material.DIAMOND_PICKAXE;
        };
        Material material = MaterialUtil.read(plugin, "gui.items." + skill + ".material", fallback);
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        meta.displayName(TextUtil.color(plugin.getConfig().getString(
                "gui.items." + skill + ".name",
                "&b" + plugin.getSkillManager().getDisplayName(skill)
        )));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);

        UUID uuid = player.getUniqueId();
        boolean enabled = plugin.getSkillManager().isEnabled(skill);
        boolean permitted = plugin.getSkillManager().hasSkillPermission(player, skill);
        boolean active = plugin.getSkillManager().isSkillActive(uuid, skill);

        String status;
        String finalLine;
        if (!enabled) {
            status = plugin.getConfig().getString("gui.status.disabled", "&cDinonaktifkan");
            finalLine = "";
        } else if (!permitted) {
            status = plugin.getConfig().getString("gui.status.no-permission", "&cTidak memiliki izin");
            finalLine = "";
        } else if (active) {
            status = plugin.getConfig().getString("gui.status.active", "&aAktif");
            finalLine = TextUtil.replace(
                    plugin.getConfig().getString(
                            "gui.status.remaining-line",
                            "&fSisa waktu: &a{time}"
                    ),
                    Map.of("time", plugin.getSkillManager().getRemainingFormatted(uuid, skill))
            );
        } else {
            status = plugin.getConfig().getString("gui.status.inactive", "&eKlik untuk membeli");
            finalLine = plugin.getConfig().getString(
                    "gui.status.click-line",
                    "&eKlik untuk langsung mengaktifkan"
            );
        }

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("price", TextUtil.formatMoney(plugin.getSkillManager().getPrice(skill)));
        placeholders.put(
                "duration",
                TextUtil.formatDuration(plugin.getSkillManager().getDurationMillis(skill))
        );
        placeholders.put("status", status);
        placeholders.put("remaining_line", finalLine);
        placeholders.put("max_blocks", String.valueOf(getMaxBlocks(plugin, skill)));

        List<Component> lore = new ArrayList<>();
        for (String line : plugin.getConfig().getStringList("gui.items." + skill + ".lore")) {
            String parsed = TextUtil.replace(line, placeholders);
            lore.add(parsed.isEmpty() ? Component.empty() : TextUtil.color(parsed));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof SkillMenuHolder holder)) {
            return;
        }
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
            return;
        }

        String skill = holder.getSkill(event.getRawSlot());
        if (skill == null) {
            return;
        }
        if (plugin.getSkillManager().purchaseSkill(player, skill)) {
            open(plugin, player);
        } else {
            open(plugin, player);
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof SkillMenuHolder) {
            event.setCancelled(true);
        }
    }

    private static int normalizeSize(int configured) {
        int clamped = Math.max(9, Math.min(54, configured));
        return ((clamped + 8) / 9) * 9;
    }

    private static int defaultSlot(String skill) {
        return switch (skill) {
            case "tree" -> 4;
            case "farmer" -> 5;
            default -> 3;
        };
    }

    private static int getMaxBlocks(VelioraFTB plugin, String skill) {
        return switch (skill) {
            case "tree" -> plugin.getConfig().getInt("skills.tree.max-logs", 128);
            case "farmer" -> plugin.getConfig().getInt("skills.farmer.max-crops", 64);
            default -> plugin.getConfig().getInt("skills.vein.max-blocks", 64);
        };
    }

    private static final class SkillMenuHolder implements InventoryHolder {
        private final Map<Integer, String> skillsBySlot = new HashMap<>();
        private Inventory inventory;

        private void setInventory(Inventory inventory) {
            this.inventory = inventory;
        }

        private void register(int slot, String skill) {
            skillsBySlot.put(slot, skill);
        }

        private String getSkill(int slot) {
            return skillsBySlot.get(slot);
        }

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }
}
