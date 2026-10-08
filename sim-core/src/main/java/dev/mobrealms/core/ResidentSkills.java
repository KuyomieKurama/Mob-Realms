package dev.mobrealms.core;

import java.util.UUID;
import dev.mobrealms.core.Development.Role;

/** Stable individual aptitudes: a newborn keeps the same strengths after every reload. */
public final class ResidentSkills {
    private ResidentSkills() {}
    public enum Branch { BUILDING, MINING, GATHERING, COMBAT }

    public static Branch branch(Role role) {
        return switch (role) {
            case BUILDER -> Branch.BUILDING;
            case MINER -> Branch.MINING;
            case GUARD, SOLDIER -> Branch.COMBAT;
            default -> Branch.GATHERING;
        };
    }

    public static int level(int experience) { return Math.min(5, Math.max(0, experience / 50)); }
    public static String root(Branch branch) { return branch.name().toLowerCase(java.util.Locale.ROOT) + "_root"; }
    public static String mastery(Branch branch) { return branch.name().toLowerCase(java.util.Locale.ROOT) + "_mastery"; }

    public static int aptitude(UUID id, Role role) {
        long value = id.getMostSignificantBits() ^ Long.rotateLeft(id.getLeastSignificantBits(), 23)
                ^ (0x9e3779b97f4a7c15L * (role.ordinal() + 1));
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        return 1 + Math.floorMod(value, 5);
    }

    public static int workBonus(UUID id, Role role, int experience) {
        return Math.min(4, experience / 100) + aptitude(id, role) - 1;
    }
}
