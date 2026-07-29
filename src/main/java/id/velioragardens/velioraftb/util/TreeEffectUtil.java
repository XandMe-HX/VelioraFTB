package id.velioragardens.velioraftb.util;

import id.velioragardens.velioraftb.VelioraFTB;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Locale;

public final class TreeEffectUtil {
    private static final String CONFIG_ROOT = "skills.tree.effects.";

    private TreeEffectUtil() {
    }

    public static void play(VelioraFTB plugin, List<Block> logs) {
        if (logs.isEmpty() || !plugin.getConfig().getBoolean(CONFIG_ROOT + "enabled", true)) {
            return;
        }

        Bounds bounds = findBounds(logs);
        World world = logs.getFirst().getWorld();
        playSound(
                plugin,
                world,
                bounds.center(world, bounds.minY + 0.5D),
                CONFIG_ROOT + "sounds.start",
                Sound.ENTITY_PLAYER_ATTACK_SWEEP
        );
        playCircle(plugin, world, bounds);
    }

    public static void playFinishSound(VelioraFTB plugin, List<Block> logs) {
        if (logs.isEmpty() || !plugin.getConfig().getBoolean(CONFIG_ROOT + "enabled", true)) {
            return;
        }

        Bounds bounds = findBounds(logs);
        playSound(
                plugin,
                logs.getFirst().getWorld(),
                bounds.center(logs.getFirst().getWorld(), bounds.minY + 0.5D),
                CONFIG_ROOT + "sounds.finish",
                Sound.BLOCK_WOOD_BREAK
        );
    }

    private static void playCircle(VelioraFTB plugin, World world, Bounds bounds) {
        String root = CONFIG_ROOT + "circle.";
        if (!plugin.getConfig().getBoolean(root + "enabled", true)) {
            return;
        }

        Particle primary = readParticle(
                plugin,
                root + "primary-particle",
                Particle.HAPPY_VILLAGER
        );
        Particle secondary = readParticle(
                plugin,
                root + "secondary-particle",
                Particle.COMPOSTER
        );
        int points = clamp(plugin.getConfig().getInt(root + "points", 28), 8, 64);
        long duration = clamp(plugin.getConfig().getLong(root + "duration-ticks", 18L), 2L, 60L);
        long interval = clamp(plugin.getConfig().getLong(root + "interval-ticks", 2L), 1L, 10L);
        double padding = clamp(
                plugin.getConfig().getDouble(root + "radius-padding", 1.25D),
                0.25D,
                5.0D
        );
        double minRadius = clamp(
                plugin.getConfig().getDouble(root + "min-radius", 2.0D),
                0.5D,
                10.0D
        );
        double maxRadius = clamp(
                plugin.getConfig().getDouble(root + "max-radius", 7.0D),
                minRadius,
                12.0D
        );
        double heightOffset = clamp(
                plugin.getConfig().getDouble(root + "height-offset", 0.5D),
                -2.0D,
                3.0D
        );

        double treeRadius = Math.max(
                (bounds.maxX - bounds.minX + 1) / 2.0D,
                (bounds.maxZ - bounds.minZ + 1) / 2.0D
        );
        double radius = clamp(treeRadius + padding, minRadius, maxRadius);
        double minY = bounds.minY + heightOffset;
        double maxY = bounds.maxY + 1.0D + heightOffset;
        int frames = Math.max(1, (int) Math.ceil((double) duration / interval));

        new BukkitRunnable() {
            private int frame;

            @Override
            public void run() {
                if (frame >= frames || !plugin.isEnabled()) {
                    cancel();
                    return;
                }

                double progress = frames == 1 ? 1.0D : (double) frame / (frames - 1);
                double y = minY + ((maxY - minY) * progress);
                double rotation = progress * Math.PI * 2.0D;
                spawnRing(world, bounds.centerX(), y, bounds.centerZ(), radius, points, rotation, primary, secondary);
                frame++;
            }
        }.runTaskTimer(plugin, 0L, interval);
    }

    private static void spawnRing(
            World world,
            double centerX,
            double y,
            double centerZ,
            double radius,
            int points,
            double rotation,
            Particle primary,
            Particle secondary
    ) {
        for (int point = 0; point < points; point++) {
            double angle = rotation + (Math.PI * 2.0D * point / points);
            double x = centerX + Math.cos(angle) * radius;
            double z = centerZ + Math.sin(angle) * radius;
            Particle particle = point % 4 == 0 ? secondary : primary;
            world.spawnParticle(particle, x, y, z, 1, 0.03D, 0.03D, 0.03D, 0.0D);
        }
    }

    private static void playSound(
            VelioraFTB plugin,
            World world,
            Location location,
            String path,
            Sound fallback
    ) {
        if (!plugin.getConfig().getBoolean(path + ".enabled", true)) {
            return;
        }

        Sound sound = readSound(plugin, path + ".sound", fallback);
        float volume = (float) clamp(plugin.getConfig().getDouble(path + ".volume", 0.9D), 0.0D, 4.0D);
        float pitch = (float) clamp(plugin.getConfig().getDouble(path + ".pitch", 1.0D), 0.5D, 2.0D);
        world.playSound(location, sound, volume, pitch);
    }

    private static Sound readSound(VelioraFTB plugin, String path, Sound fallback) {
        String configured = plugin.getConfig().getString(path, fallback.name());
        try {
            return Sound.valueOf(configured.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Sound tidak valid pada " + path + ": " + configured);
            return fallback;
        }
    }

    private static Particle readParticle(VelioraFTB plugin, String path, Particle fallback) {
        String configured = plugin.getConfig().getString(path, fallback.name());
        try {
            Particle particle = Particle.valueOf(configured.toUpperCase(Locale.ROOT));
            if (particle.getDataType() != Void.class) {
                plugin.getLogger().warning(
                        "Particle " + configured + " membutuhkan data tambahan; memakai " + fallback.name() + "."
                );
                return fallback;
            }
            return particle;
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Particle tidak valid pada " + path + ": " + configured);
            return fallback;
        }
    }

    private static Bounds findBounds(List<Block> logs) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;

        for (Block log : logs) {
            minX = Math.min(minX, log.getX());
            minY = Math.min(minY, log.getY());
            minZ = Math.min(minZ, log.getZ());
            maxX = Math.max(maxX, log.getX());
            maxY = Math.max(maxY, log.getY());
            maxZ = Math.max(maxZ, log.getZ());
        }
        return new Bounds(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    private static long clamp(long value, long min, long max) {
        return Math.max(min, Math.min(value, max));
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(value, max));
    }

    private record Bounds(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        private double centerX() {
            return (minX + maxX + 1) / 2.0D;
        }

        private double centerZ() {
            return (minZ + maxZ + 1) / 2.0D;
        }

        private Location center(World world, double y) {
            return new Location(world, centerX(), y, centerZ());
        }
    }
}
