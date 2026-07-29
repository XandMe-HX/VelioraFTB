package id.velioragardens.velioraftb.util;

import id.velioragardens.velioraftb.VelioraFTB;
import org.bukkit.Material;

import java.util.EnumSet;
import java.util.Set;

public final class MaterialUtil {
    private MaterialUtil() {
    }

    public static Set<Material> readSet(VelioraFTB plugin, String path) {
        Set<Material> result = EnumSet.noneOf(Material.class);
        for (String value : plugin.getConfig().getStringList(path)) {
            Material material = Material.matchMaterial(value);
            if (material == null) {
                plugin.getLogger().warning("Material tidak valid pada " + path + ": " + value);
                continue;
            }
            result.add(material);
        }
        return result;
    }

    public static Material read(
            VelioraFTB plugin,
            String path,
            Material fallback
    ) {
        String value = plugin.getConfig().getString(path);
        Material material = value == null ? null : Material.matchMaterial(value);
        if (material == null) {
            if (value != null) {
                plugin.getLogger().warning("Material tidak valid pada " + path + ": " + value);
            }
            return fallback;
        }
        return material;
    }
}
