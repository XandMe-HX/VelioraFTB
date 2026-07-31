package id.velioragardens.velioraftb.listener;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * Keeps tilled farmland safe when a player lands or walks on it.
 */
public final class FarmlandProtectionListener implements Listener {

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onFarmlandTrample(PlayerInteractEvent event) {
        if (event.getAction() != Action.PHYSICAL
                || event.getClickedBlock() == null
                || event.getClickedBlock().getType() != Material.FARMLAND) {
            return;
        }

        event.setCancelled(true);
    }
}
