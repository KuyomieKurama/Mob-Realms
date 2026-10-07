package dev.mobrealms.core;

/** Stateless utility scoring. Safety and existing cargo outrank new work. */
public final class UtilityBrain {
    public enum Goal { SHELTER, DELIVER, GATHER, REGROUP, IDLE }
    public record Observation(boolean sunExposed, boolean threatened, boolean hasCargo,
                              boolean resourceAvailable, double distanceFromCamp, double diligence) {
        public Observation {
            if (!Double.isFinite(distanceFromCamp) || distanceFromCamp < 0
                    || !Double.isFinite(diligence) || diligence < 0 || diligence > 1)
                throw new IllegalArgumentException("Invalid observation");
        }
    }
    public Goal choose(SpeciesProfile profile, Observation o) {
        if (o.threatened() || (profile.avoidsSun() && o.sunExposed())) return Goal.SHELTER;
        if (o.hasCargo()) return Goal.DELIVER;
        double gather = o.resourceAvailable() ? profile.gatherWeight() * (0.5 + o.diligence()) : 0;
        double regroup = profile.regroupWeight() * Math.max(0, o.distanceFromCamp() - 8) / 16;
        if (regroup > gather && regroup > 0) return Goal.REGROUP;
        return gather > 0 ? Goal.GATHER : Goal.IDLE;
    }
}
