package dev.mobrealms.core;

import java.util.Objects;

public record ChunkKey(String dimension, int x, int z) {
    public ChunkKey { Objects.requireNonNull(dimension); if (dimension.isBlank()) throw new IllegalArgumentException("dimension"); }
    public static ChunkKey fromBlock(String dimension, int x, int z) {
        return new ChunkKey(dimension, Math.floorDiv(x, 16), Math.floorDiv(z, 16));
    }
}
