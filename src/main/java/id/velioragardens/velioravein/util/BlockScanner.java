package id.velioragardens.velioravein.util;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import java.util.*;

public class BlockScanner {

    public static List<Block> scanVein(Block startBlock, int maxBlocks) {
        List<Block> vein = new ArrayList<>();
        Queue<Block> queue = new LinkedList<>();
        Set<Block> visited = new HashSet<>();

        Material startType = startBlock.getType();
        if (!isOre(startType)) return vein;

        queue.add(startBlock);
        visited.add(startBlock);

        while (!queue.isEmpty() && vein.size() < maxBlocks) {
            Block current = queue.poll();
            vein.add(current);

            for (BlockFace face : BlockFace.values()) {
                if (face == BlockFace.SELF) continue;
                Block relative = current.getRelative(face);
                if (!visited.contains(relative) && isMatchingOre(startType, relative.getType())) {
                    visited.add(relative);
                    queue.add(relative);
                }
            }
        }
        return vein;
    }

    public static List<Block> scanTreeLogs(Block startBlock, int maxBlocks) {
        List<Block> logs = new ArrayList<>();
        Queue<Block> queue = new LinkedList<>();
        Set<Block> visited = new HashSet<>();

        Material startType = startBlock.getType();
        if (!isLog(startType)) return logs;

        queue.add(startBlock);
        visited.add(startBlock);

        while (!queue.isEmpty() && logs.size() < maxBlocks) {
            Block current = queue.poll();
            logs.add(current);

            for (BlockFace face : BlockFace.values()) {
                if (face == BlockFace.SELF) continue;
                Block relative = current.getRelative(face);
                if (!visited.contains(relative) && relative.getType() == startType) {
                    visited.add(relative);
                    queue.add(relative);
                }
            }
        }
        return logs;
    }

    public static List<Block> scanLeaves(List<Block> logs, int radius) {
        List<Block> leaves = new ArrayList<>();
        Set<Block> visited = new HashSet<>();

        for (Block log : logs) {
            for (int x = -radius; x <= radius; x++) {
                for (int y = -radius; y <= radius; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        Block relative = log.getRelative(x, y, z);
                        if (isLeaf(relative.getType()) && !visited.contains(relative)) {
                            visited.add(relative);
                            leaves.add(relative);
                        }
                    }
                }
            }
        }
        return leaves;
    }

    public static List<Block> scanCrops(Block startBlock, int maxBlocks) {
        List<Block> crops = new ArrayList<>();
        Queue<Block> queue = new LinkedList<>();
        Set<Block> visited = new HashSet<>();

        Material startType = startBlock.getType();
        if (!isCrop(startType)) return crops;

        queue.add(startBlock);
        visited.add(startBlock);

        while (!queue.isEmpty() && crops.size() < maxBlocks) {
            Block current = queue.poll();
            if (isFullyGrown(current)) {
                crops.add(current);
            }

            BlockFace[] horizontalFaces = {
                BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST,
                BlockFace.NORTH_EAST, BlockFace.NORTH_WEST, BlockFace.SOUTH_EAST, BlockFace.SOUTH_WEST
            };

            for (BlockFace face : horizontalFaces) {
                Block relative = current.getRelative(face);
                if (!visited.contains(relative) && relative.getType() == startType) {
                    visited.add(relative);
                    queue.add(relative);
                }
            }
        }
        return crops;
    }

    private static boolean isOre(Material material) {
        String name = material.name();
        return name.contains("ORE") || name.equals("ANCIENT_DEBRIS");
    }

    private static boolean isMatchingOre(Material start, Material candidate) {
        if (start == candidate) return true;
        String sName = start.name().replace("DEEPSLATE_", "");
        String cName = candidate.name().replace("DEEPSLATE_", "");
        return sName.equals(cName);
    }

    private static boolean isLog(Material material) {
        String name = material.name();
        return name.endsWith("_LOG") || name.endsWith("_WOOD") || name.endsWith("_STEM") || name.equals("MANGROVE_ROOTS");
    }

    private static boolean isLeaf(Material material) {
        String name = material.name();
        return name.endsWith("_LEAVES") || name.endsWith("_WART_BLOCK") || name.equals("SHROOM_LIGHT");
    }

    private static boolean isCrop(Material material) {
        return switch (material) {
            case WHEAT, POTATOES, CARROTS, BEETROOTS, NETHER_WART -> true;
            default -> false;
        };
    }

    public static boolean isFullyGrown(Block block) {
        if (block.getBlockData() instanceof org.bukkit.block.data.Ageable ageable) {
            return ageable.getAge() == ageable.getMaximumAge();
        }
        return false;
    }
}
