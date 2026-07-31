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

public final class SkillEffectUtil {
    private SkillEffectUtil() {
    }

    public static void playTreeStart(VelioraFTB plugin, List<Block> logs) {
        String root = "skills.tree.effects.";
        if (!enabled(plugin, root, logs)) {
            return;
        }

        Bounds bounds = findBounds(logs);
        World world = logs.getFirst().getWorld();
        playSound(plugin, world, bounds.center(world), root + "sounds.start", Sound.ENTITY_PLAYER_ATTACK_SWEEP);
        playTreeRing(plugin, world, bounds, root + "particles.");
    }

    public static void playTreeFinish(VelioraFTB plugin, List<Block> logs) {
        String root = "skills.tree.effects.";
        if (!enabled(plugin, root, logs)) {
            return;
        }

        Bounds bounds = findBounds(logs);
        playSound(plugin, logs.getFirst().getWorld(), bounds.center(logs.getFirst().getWorld()),
                root + "sounds.finish", Sound.BLOCK_WOOD_BREAK);
    }

    public static void playVeinStart(VelioraFTB plugin, List<Block> ores) {
        String root = "skills.vein.effects.";
        if (!enabled(plugin, root, ores)) {
            return;
        }

        Bounds bounds = findBounds(ores);
        World world = ores.getFirst().getWorld();
        playSound(plugin, world, bounds.center(world), root + "sounds.start", Sound.BLOCK_AMETHYST_BLOCK_HIT);
        playBlockTrail(plugin, ores, root + "particles.", Particle.END_ROD, Particle.CRIT);
    }

    public static void playVeinFinish(VelioraFTB plugin, List<Block> ores) {
        String root = "skills.vein.effects.";
        if (!enabled(plugin, root, ores)) {
            return;
        }

        Bounds bounds = findBounds(ores);
        playSound(plugin, ores.getFirst().getWorld(), bounds.center(ores.getFirst().getWorld()),
                root + "sounds.finish", Sound.BLOCK_STONE_BREAK);
    }

    public static void playFarmerStart(VelioraFTB plugin, List<Block> crops) {
        String root = "skills.farmer.effects.";
        if (!enabled(plugin, root, crops)) {
            return;
        }

        Bounds bounds = findBounds(crops);
        World world = crops.getFirst().getWorld();
        playSound(plugin, world, bounds.center(world), root + "sounds.start", Sound.ITEM_HOE_TILL);
        playCropWave(plugin, crops, root + "particles.");
    }

    public static void playFarmerFinish(VelioraFTB plugin, List<Block> crops) {
        String root = "skills.farmer.effects.";
        if (!enabled(plugin, root, crops)) {
            return;
        }

        Bounds bounds = findBounds(crops);
        playSound(plugin, crops.getFirst().getWorld(), bounds.center(crops.getFirst().getWorld()),
                root + "sounds.finish", Sound.BLOCK_GRASS_BREAK);
    }

    private static boolean enabled(VelioraFTB plugin, String root, List<Block> blocks) {
        return !blocks.isEmpty() && plugin.getConfig().getBoolean(root + "enabled", true);
    }

    private static void playTreeRing(VelioraFTB plugin, World world, Bounds bounds, String root) {
        if (!plugin.getConfig().getBoolean(root + "enabled", true)) {
            return;
        }

        Particle primary = readParticle(plugin, root + "primary-particle", Particle.HAPPY_VILLAGER);
        Particle secondary = readParticle(plugin, root + "secondary-particle", Particle.COMPOSTER);
        int points = clamp(plugin.getConfig().getInt(root + "points", 28), 8, 64);
        long duration = clamp(plugin.getConfig().getLong(root + "duration-ticks", 18L), 2L, 60L);
        long interval = clamp(plugin.getConfig().getLong(root + "interval-ticks", 2L), 1L, 10L);
        double padding = clamp(plugin.getConfig().getDouble(root + "radius-padding", 1.25D), 0.25D, 5.0D);
        double minRadius = clamp(plugin.getConfig().getDouble(root + "min-radius", 2.0D), 0.5D, 10.0D);
        double maxRadius = clamp(plugin.getConfig().getDouble(root + "max-radius", 7.0D), minRadius, 12.0D);
        double heightOffset = clamp(plugin.getConfig().getDouble(root + "height-offset", 0.5D), -2.0D, 3.0D);

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
                for (int point = 0; point < points; point++) {
                    double angle = rotation + (Math.PI * 2.0D * point / points);
                    double x = bounds.centerX() + Math.cos(angle) * radius;
                    double z = bounds.centerZ() + Math.sin(angle) * radius;
                    Particle particle = point % 4 == 0 ? secondary : primary;
                    world.spawnParticle(particle, x, y, z, 1, 0.03D, 0.03D, 0.03D, 0.0D);
                }
                frame++;
            }
        }.runTaskTimer(plugin, 0L, interval);
    }

    private static void playBlockTrail(
            VelioraFTB plugin,
            List<Block> blocks,
            String root,
            Particle primaryFallback,
            Particle secondaryFallback
    ) {
        if (!plugin.getConfig().getBoolean(root + "enabled", true)) {
            return;
        }

        Particle primary = readParticle(plugin, root + "primary-particle", primaryFallback);
        Particle secondary = readParticle(plugin, root + "secondary-particle", secondaryFallback);
        int amount = clamp(plugin.getConfig().getInt(root + "amount-per-block", 5), 1, 16);
        int secondaryAmount = clamp(plugin.getConfig().getInt(
                root + "secondary-amount-per-block", Math.max(1, amount / 2)
        ), 1, 16);
        int blockAmount = clamp(plugin.getConfig().getInt(root + "block-particle-amount", 0), 0, 8);
        int maxBlocks = clamp(plugin.getConfig().getInt(root + "max-effect-blocks", 48), 1, 128);
        double spread = clamp(plugin.getConfig().getDouble(root + "spread", 0.18D), 0.0D, 1.0D);

        int step = Math.max(1, (int) Math.ceil((double) blocks.size() / maxBlocks));
        for (int index = 0; index < blocks.size(); index += step) {
            Location location = blocks.get(index).getLocation().add(0.5D, 0.5D, 0.5D);
            location.getWorld().spawnParticle(primary, location, amount, spread, spread, spread, 0.01D);
            location.getWorld().spawnParticle(secondary, location, secondaryAmount,
                    spread / 2.0D, spread / 2.0D, spread / 2.0D, 0.0D);
            if (blockAmount > 0) {
                location.getWorld().spawnParticle(Particle.BLOCK, location, blockAmount,
                        spread / 2.0D, spread / 2.0D, spread / 2.0D, 0.0D,
                        blocks.get(index).getBlockData());
            }
        }
    }

    private static void playCropWave(VelioraFTB plugin, List<Block> crops, String root) {
        if (!plugin.getConfig().getBoolean(root + "enabled", true)) {
            return;
        }

        Particle primary = readParticle(plugin, root + "primary-particle", Particle.COMPOSTER);
        Particle secondary = readParticle(plugin, root + "secondary-particle", Particle.HAPPY_VILLAGER);
        int amount = clamp(plugin.getConfig().getInt(root + "amount-per-crop", 3), 1, 12);
        int secondaryAmount = clamp(plugin.getConfig().getInt(
                root + "secondary-amount-per-crop", Math.max(1, amount / 2)
        ), 1, 12);
        int maxCrops = clamp(plugin.getConfig().getInt(root + "max-effect-crops", 48), 1, 128);
        long interval = clamp(plugin.getConfig().getLong(root + "interval-ticks", 1L), 1L, 5L);
        int step = Math.max(1, (int) Math.ceil((double) crops.size() / maxCrops));

        new BukkitRunnable() {
            private int index;

            @Override
            public void run() {
                if (index >= crops.size() || !plugin.isEnabled()) {
                    cancel();
                    return;
                }

                int spawned = 0;
                while (index < crops.size() && spawned < 8) {
                    Block crop = crops.get(index);
                    Location location = crop.getLocation().add(0.5D, 0.7D, 0.5D);
                    location.getWorld().spawnParticle(primary, location, amount, 0.25D, 0.15D, 0.25D, 0.01D);
                    location.getWorld().spawnParticle(secondary, location, secondaryAmount,
                            0.16D, 0.10D, 0.16D, 0.0D);
                    index += step;
                    spawned++;
                }
            }
        }.runTaskTimer(plugin, 0L, interval);
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

    private static Bounds findBounds(List<Block> blocks) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;

        for (Block block : blocks) {
            minX = Math.min(minX, block.getX());
            minY = Math.min(minY, block.getY());
            minZ = Math.min(minZ, block.getZ());
            maxX = Math.max(maxX, block.getX());
            maxY = Math.max(maxY, block.getY());
            maxZ = Math.max(maxZ, block.getZ());
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

        private Location center(World world) {
            return new Location(world, centerX(), minY + 0.5D, centerZ());
        }
    }
}
