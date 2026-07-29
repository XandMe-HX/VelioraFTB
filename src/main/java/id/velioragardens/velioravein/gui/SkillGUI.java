package id.velioragardens.velioravein.gui;

import id.velioragardens.velioravein.VelioraVein;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class SkillGUI implements Listener {
    private final VelioraVein plugin;
    private static final String GUI_TITLE_KEY = "gui.title";

    public SkillGUI(VelioraVein plugin) {
        this.plugin = plugin;
    }

    public static void open(VelioraVein plugin, Player player) {
        String rawTitle = plugin.getMessagesConfig().getString(GUI_TITLE_KEY, "&8Skill Shop");
        Inventory inv = Bukkit.createInventory(null, 9, LegacyComponentSerializer.legacyAmpersand().deserialize(rawTitle));

        ItemStack pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta paneMeta = pane.getItemMeta();
        if (paneMeta != null) {
            paneMeta.displayName(Component.text(" "));
            pane.setItemMeta(paneMeta);
        }
        for (int i = 0; i < 9; i++) inv.setItem(i, pane);

        UUID uuid = player.getUniqueId();
        int duration = plugin.getConfig().getInt("duration.hours", 3);

        inv.setItem(3, createSkillItem(plugin, "vein", Material.DIAMOND_PICKAXE, plugin.getSkillManager().isSkillActive(uuid, "vein"), duration));
        inv.setItem(4, createSkillItem(plugin, "tree", Material.DIAMOND_AXE, plugin.getSkillManager().isSkillActive(uuid, "tree"), duration));
        inv.setItem(5, createSkillItem(plugin, "farmer", Material.DIAMOND_HOE, plugin.getSkillManager().isSkillActive(uuid, "farmer"), duration));

        player.openInventory(inv);
    }

    private static ItemStack createSkillItem(VelioraVein plugin, String skillKey, Material material, boolean active, int duration) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        // Ambil Nama
        String name = plugin.getMessagesConfig().getString("gui." + skillKey + ".name", "&b" + skillKey);
        meta.displayName(LegacyComponentSerializer.legacyAmpersand().deserialize(name));

        // Ambil List Deskripsi dari Config
        List<String> descList = plugin.getMessagesConfig().getStringList("gui." + skillKey + ".desc");
        List<Component> lore = new ArrayList<>();
        
        for (String line : descList) {
            lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize(line));
        }

        // Tambahkan Status Aktif/Tidak
        lore.add(Component.text(" "));
        lore.add(LegacyComponentSerializer.legacyAmpersand().deserialize(active ? "&a✔ Sedang Aktif" : "&eKlik untuk membeli"));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String guiTitle = LegacyComponentSerializer.legacyAmpersand().serialize(event.getView().title());
        String expectedTitle = plugin.getMessagesConfig().getString(GUI_TITLE_KEY, "&8Skill Shop");

        if (!guiTitle.equals(expectedTitle)) return;
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;

        int slot = event.getRawSlot();
        String skillType = switch (slot) {
            case 3 -> "vein";
            case 4 -> "tree";
            case 5 -> "farmer";
            default -> null;
        };

        if (skillType == null) return;

        if (plugin.getSkillManager().isSkillActive(player.getUniqueId(), skillType)) {
            player.sendMessage(LegacyComponentSerializer.legacyAmpersand().deserialize(
                plugin.getMessagesConfig().getString("prefix", "&6[Veliora] ") + 
                plugin.getMessagesConfig().getString("shop.already-active", "&eSkill masih aktif.")
                .replace("{skill}", plugin.getSkillManager().formatSkillName(skillType))));
            player.closeInventory();
            return;
        }

        if (plugin.getSkillManager().purchaseSkill(player, skillType)) {
            open(plugin, player);
        } else {
            player.closeInventory();
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (LegacyComponentSerializer.legacyAmpersand().serialize(event.getView().title()).equals(
            plugin.getMessagesConfig().getString(GUI_TITLE_KEY, "&8Skill Shop"))) {
            event.setCancelled(true);
        }
    }
}