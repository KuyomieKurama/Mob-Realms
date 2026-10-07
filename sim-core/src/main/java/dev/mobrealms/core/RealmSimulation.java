package dev.mobrealms.core;

import java.util.*;

/** Single-thread-owned domain state. Minecraft objects must never enter this class. */
public final class RealmSimulation {
    public enum Mode { ABSTRACT, DETAILED }
    public record Camp(UUID id, String species, ChunkKey territory, int x, int y, int z) {
        public Camp {
            Objects.requireNonNull(id); Objects.requireNonNull(species); Objects.requireNonNull(territory);
            if (!territory.equals(ChunkKey.fromBlock(territory.dimension(), x, z)))
                throw new IllegalArgumentException("Camp outside territory");
        }
    }
    public record CitizenView(UUID id, UUID camp, Mode mode, long generation, double diligence,
                              Map<String, Long> cargo) {}
    public record Lease(UUID citizen, long generation) {}
    private static final class Citizen {
        final UUID id, camp;
        final double diligence;
        final Stockpile cargo = new Stockpile();
        Mode mode = Mode.ABSTRACT;
        long generation;
        Citizen(UUID id, UUID camp, double diligence) {
            this.id = Objects.requireNonNull(id); this.camp = Objects.requireNonNull(camp);
            if (!Double.isFinite(diligence) || diligence < 0 || diligence > 1) throw new IllegalArgumentException("diligence");
            this.diligence = diligence;
        }
        CitizenView view() { return new CitizenView(id, camp, mode, generation, diligence, cargo.snapshot()); }
    }
    private final LinkedHashMap<UUID, Camp> camps = new LinkedHashMap<>();
    private final LinkedHashMap<UUID, Citizen> citizens = new LinkedHashMap<>();
    private final Map<UUID, Stockpile> stores = new HashMap<>();
    private final Set<ChunkKey> protectedChunks = new HashSet<>();
    private final int maxCamps, maxPopulation, maxDetailed;
    private long day;
    public RealmSimulation(int maxCamps, int maxPopulation, int maxDetailed) {
        if (maxCamps < 1 || maxCamps > 1024 || maxPopulation < 1 || maxPopulation > 100_000
                || maxDetailed < 1 || maxDetailed > maxPopulation) throw new IllegalArgumentException("limits");
        this.maxCamps = maxCamps; this.maxPopulation = maxPopulation; this.maxDetailed = maxDetailed;
    }
    public int maxCamps() { return maxCamps; }
    public int maxPopulation() { return maxPopulation; }
    public int maxDetailed() { return maxDetailed; }
    public long day() { return day; }
    public List<Camp> camps() { return List.copyOf(camps.values()); }
    public List<CitizenView> citizens() { return citizens.values().stream().map(Citizen::view).toList(); }
    public CitizenView citizen(UUID id) { return required(id).view(); }
    public boolean hasCitizen(UUID id) { return citizens.containsKey(id); }
    public Camp camp(UUID id) { return Objects.requireNonNull(camps.get(id), "Unknown camp"); }
    public Map<String, Long> stock(UUID camp) { return store(camp).snapshot(); }
    public Set<ChunkKey> protectedChunks() { return Set.copyOf(protectedChunks); }
    public void protect(ChunkKey chunk) {
        if (protectedChunks.size() >= 100_000 && !protectedChunks.contains(chunk)) throw new IllegalStateException("Protection limit");
        protectedChunks.add(Objects.requireNonNull(chunk));
    }
    public void unprotect(ChunkKey chunk) { protectedChunks.remove(chunk); }
    public boolean protectedAt(ChunkKey chunk) { return protectedChunks.contains(chunk); }
    public boolean canFound(ChunkKey chunk) {
        return camps.size() < maxCamps && !protectedAt(chunk)
                && camps.values().stream().noneMatch(c -> c.territory().equals(chunk));
    }
    public void found(Camp camp) {
        if (camps.containsKey(camp.id()) || !canFound(camp.territory())) throw new IllegalStateException("Camp conflict/limit");
        camps.put(camp.id(), camp); stores.put(camp.id(), new Stockpile());
    }
    public void addCitizen(UUID id, UUID camp, double diligence) {
        camp(camp);
        if (citizens.size() >= maxPopulation || citizens.containsKey(id)) throw new IllegalStateException("Citizen conflict/limit");
        citizens.put(id, new Citizen(id, camp, diligence));
    }
    public Lease activate(UUID id) {
        Citizen c = required(id);
        if (c.mode == Mode.DETAILED) throw new IllegalStateException("Already active");
        if (citizens.values().stream().filter(v -> v.mode == Mode.DETAILED).count() >= maxDetailed)
            throw new IllegalStateException("Detailed limit");
        long next = Math.incrementExact(c.generation);
        c.mode = Mode.DETAILED; c.generation = next;
        return new Lease(id, next);
    }
    public void deactivate(Lease lease) {
        Citizen c = leased(lease); c.mode = Mode.ABSTRACT;
    }
    /** Adapter calls only after the source item is successfully removed from the world. */
    public void collect(Lease lease, String item, int amount, int capacity) {
        Citizen c = leased(lease);
        long held = c.cargo.snapshot().values().stream().mapToLong(Long::longValue).sum();
        if (amount <= 0 || capacity < 1 || capacity > 64 || held + amount > capacity)
            throw new IllegalArgumentException("Cargo capacity");
        c.cargo.add(item, amount);
    }
    public void deliver(Lease lease) { deliver(requiredLeased(lease)); }
    private Citizen requiredLeased(Lease lease) { return leased(lease); }
    private void deliver(Citizen c) {
        Stockpile destination = store(c.camp);
        // Preflight all items so an overflow cannot partially deliver cargo.
        c.cargo.snapshot().forEach((item, amount) -> Math.addExact(destination.count(item), amount));
        c.cargo.snapshot().forEach((item, amount) -> c.cargo.transferTo(destination, item, amount));
    }
    /** Only absent citizens deliver existing cargo; no new resources are created. */
    public void advanceDay() {
        long next = Math.incrementExact(day);
        for (Citizen c : citizens.values()) if (c.mode == Mode.ABSTRACT) deliver(c);
        day = next;
    }
    /** Death destroys undelivered cargo; it must not also be dropped by the adapter. */
    public void removeCitizen(UUID id) { required(id); citizens.remove(id); }
    void restoreDay(long day) { if (day < 0) throw new IllegalArgumentException("day"); this.day = day; }
    void restoreStock(UUID id, String item, long count) { store(id).add(item, count); }
    void restoreCargo(UUID id, String item, long count) { required(id).cargo.add(item, count); }
    void restoreGeneration(UUID id, long generation) {
        if (generation < 0) throw new IllegalArgumentException("generation"); required(id).generation = generation;
    }
    private Stockpile store(UUID camp) { return Objects.requireNonNull(stores.get(camp), "Unknown camp"); }
    private Citizen required(UUID id) { return Objects.requireNonNull(citizens.get(id), "Unknown citizen"); }
    private Citizen leased(Lease lease) {
        Citizen c = required(lease.citizen());
        if (c.mode != Mode.DETAILED || c.generation != lease.generation()) throw new IllegalStateException("Stale lease");
        return c;
    }
}
