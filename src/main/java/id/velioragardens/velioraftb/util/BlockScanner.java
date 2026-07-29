package id.velioragardens.velioraftb.util;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Leaves;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public final class BlockScanner {
    private BlockScanner() {
    }

    public static List<Block> scanVein(
            Block start,
            int maxBlocks,
            Set<Material> allowed,
            boolean diagonal
    ) {
        if (!allowed.contains(start.getType())) {
            return List.of();
        }

        List<Block> result = new ArrayList<>();
        Queue<Block> queue = new ArrayDeque<>();
        Set<Block> visited = new HashSet<>();
        Material originType = start.getType();
        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty() && result.size() < maxBlocks) {
            Block current = queue.remove();
            result.add(current);

            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        if (x == 0 && y == 0 && z == 0) {
                            continue;
                        }
                        if (!diagonal && Math.abs(x) + Math.abs(y) + Math.abs(z) != 1) {
                            continue;
                        }

                        Block next = current.getRelative(x, y, z);
                        if (visited.add(next)
                                && allowed.contains(next.getType())
                                && isMatchingOre(originType, next.getType())) {
                            queue.add(next);
                        }
                    }
                }
            }
        }
        return result;
    }

    public static List<Block> scanTreeLogs(
            Block start,
            int maxLogs,
            int maxHorizontalDistance,
            int maxVerticalDistance,
            Set<Material> allowed
    ) {
        if (!allowed.contains(start.getType())) {
            return List.of();
        }

        String family = woodFamily(start.getType());
        List<Block> result = new ArrayList<>();
        Queue<Block> queue = new ArrayDeque<>();
        Set<Block> visited = new HashSet<>();
        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty() && result.size() < maxLogs) {
            Block current = queue.remove();
            result.add(current);

            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        if (x == 0 && y == 0 && z == 0) {
                            continue;
                        }
                        Block next = current.getRelative(x, y, z);
                        if (!visited.add(next)) {
                            continue;
                        }

                        int horizontalX = Math.abs(next.getX() - start.getX());
                        int horizontalZ = Math.abs(next.getZ() - start.getZ());
                        int vertical = Math.abs(next.getY() - start.getY());
                        if (horizontalX > maxHorizontalDistance
                                || horizontalZ > maxHorizontalDistance
                                || vertical > maxVerticalDistance) {
                            continue;
                        }

                        if (allowed.contains(next.getType())
                                && family.equals(woodFamily(next.getType()))) {
                            queue.add(next);
                        }
                    }
                }
            }
        }
        return result;
    }

    public static List<Block> scanLeaves(
            List<Block> logs,
            int radius,
            int maxLeaves,
            Set<Material> allowedLeaves,
            boolean onlyNatural
    ) {
        if (logs.isEmpty() || radius < 1 || maxLeaves < 1) {
            return List.of();
        }

        String family = woodFamily(logs.getFirst().getType());
        List<Block> result = new ArrayList<>();
        Set<Block> visited = new HashSet<>();
        Set<Block> ownLogs = new HashSet<>(logs);

        for (Block log : logs) {
            for (int x = -radius; x <= radius && result.size() < maxLeaves; x++) {
                for (int y = -radius; y <= radius && result.size() < maxLeaves; y++) {
                    for (int z = -radius; z <= radius && result.size() < maxLeaves; z++) {
                        Block leaf = log.getRelative(x, y, z);
                        if (!visited.add(leaf)
                                || !allowedLeaves.contains(leaf.getType())
                                || !leafMatchesFamily(leaf.getType(), family)) {
                            continue;
                        }
                        if (onlyNatural
                                && leaf.getBlockData() instanceof Leaves leaves
                                && leaves.isPersistent()) {
                            continue;
                        }
                        if (belongsToAnotherTree(leaf, ownLogs, family, radius)) {
                            continue;
                        }
                        result.add(leaf);
                    }
                }
            }
            if (result.size() >= maxLeaves) {
                break;
            }
        }
        return result;
    }

    public static List<Block> scanCrops(
            Block start,
            int maxCrops,
            Set<Material> allowed,
            boolean diagonal
    ) {
        if (!allowed.contains(start.getType()) || !isFullyGrown(start)) {
            return List.of();
        }

        Material cropType = start.getType();
        List<Block> result = new ArrayList<>();
        Queue<Block> queue = new ArrayDeque<>();
        Set<Block> visited = new HashSet<>();
        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty() && visited.size() <= maxCrops) {
            Block current = queue.remove();
            if (current.getType() == cropType && isFullyGrown(current)) {
                result.add(current);
            }

            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    if (x == 0 && z == 0) {
                        continue;
                    }
                    if (!diagonal && Math.abs(x) + Math.abs(z) != 1) {
                        continue;
                    }
                    Block next = current.getRelative(x, 0, z);
                    if (visited.size() < maxCrops
                            && visited.add(next)
                            && next.getType() == cropType) {
                        queue.add(next);
                    }
                }
            }
        }
        return result;
    }

    public static boolean isFullyGrown(Block block) {
        if (block.getBlockData() instanceof org.bukkit.block.data.Ageable ageable) {
            return ageable.getAge() >= ageable.getMaximumAge();
        }
        return false;
    }

    private static boolean isMatchingOre(Material first, Material second) {
        if (first == second) {
            return true;
        }
        return normalizeOre(first).equals(normalizeOre(second));
    }

    private static String normalizeOre(Material material) {
        return material.name().replace("DEEPSLATE_", "");
    }

    private static String woodFamily(Material material) {
        String name = material.name();
        if (name.equals("MANGROVE_ROOTS")) {
            return "MANGROVE";
        }
        for (String suffix : List.of("_LOG", "_WOOD", "_STEM", "_HYPHAE")) {
            if (name.endsWith(suffix)) {
                return name.substring(0, name.length() - suffix.length());
            }
        }
        return name;
    }

    private static boolean leafMatchesFamily(Material leaf, String family) {
        String name = leaf.name();
        if (family.equals("CRIMSON")) {
            return name.equals("NETHER_WART_BLOCK");
        }
        if (family.equals("WARPED")) {
            return name.equals("WARPED_WART_BLOCK");
        }
        if (family.equals("OAK")
                && (name.equals("AZALEA_LEAVES") || name.equals("FLOWERING_AZALEA_LEAVES"))) {
            return true;
        }
        return name.equals(family + "_LEAVES");
    }

    private static boolean belongsToAnotherTree(
            Block leaf,
            Set<Block> ownLogs,
            String family,
            int radius
    ) {
        int nearestOwn = Integer.MAX_VALUE;
        for (Block log : ownLogs) {
            int distance = chebyshevDistance(leaf, log);
            if (distance < nearestOwn) {
                nearestOwn = distance;
            }
        }

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Block nearby = leaf.getRelative(x, y, z);
                    if (ownLogs.contains(nearby)
                            || !isWood(nearby.getType())
                            || !family.equals(woodFamily(nearby.getType()))) {
                        continue;
                    }
                    int externalDistance = Math.max(Math.abs(x), Math.max(Math.abs(y), Math.abs(z)));
                    if (externalDistance <= nearestOwn) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static int chebyshevDistance(Block first, Block second) {
        return Math.max(
                Math.abs(first.getX() - second.getX()),
                Math.max(
                        Math.abs(first.getY() - second.getY()),
                        Math.abs(first.getZ() - second.getZ())
                )
        );
    }

    private static boolean isWood(Material material) {
        String name = material.name();
        return name.endsWith("_LOG")
                || name.endsWith("_WOOD")
                || name.endsWith("_STEM")
                || name.endsWith("_HYPHAE")
                || name.equals("MANGROVE_ROOTS");
    }
}
