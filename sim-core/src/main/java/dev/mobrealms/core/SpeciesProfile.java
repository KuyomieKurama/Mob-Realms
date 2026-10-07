package dev.mobrealms.core;

import java.util.Objects;

public record SpeciesProfile(String id, String entityType, double gatherWeight,
                             double regroupWeight, int carryingCapacity, boolean avoidsSun) {
    public SpeciesProfile {
        Objects.requireNonNull(id); Objects.requireNonNull(entityType);
        if (!id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+") || !entityType.matches("[a-z0-9_.-]+:[a-z0-9_./-]+"))
            throw new IllegalArgumentException("Invalid identifier");
        if (!Double.isFinite(gatherWeight) || !Double.isFinite(regroupWeight)
                || gatherWeight < 0 || regroupWeight < 0 || carryingCapacity < 1 || carryingCapacity > 64)
            throw new IllegalArgumentException("Invalid species weights/capacity");
    }
}
