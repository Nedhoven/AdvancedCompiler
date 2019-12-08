
package main.graph;

import main.backend.BlockType;

import java.util.Map;
import java.util.HashMap;

public enum PhiFunctionUpdateType {

    LEFT,
    RIGHT;

    private static Map<BlockType, PhiFunctionUpdateType> BlockPhiMap;

    public static void setBlockPhiMap() {
        BlockPhiMap = new HashMap<>();
        BlockPhiMap.put(BlockType.IF, LEFT);
        BlockPhiMap.put(BlockType.ELSE, RIGHT);
        BlockPhiMap.put(BlockType.DO, RIGHT);
    }

    public static Map<BlockType, PhiFunctionUpdateType> getBlockPhiMap() {
        if (BlockPhiMap.size() == 0) {
            setBlockPhiMap();
        }
        return BlockPhiMap;
    }

}
