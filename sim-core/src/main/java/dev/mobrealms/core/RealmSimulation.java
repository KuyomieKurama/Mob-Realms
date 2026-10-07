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
        final UUID id;
        UUID camp;
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
    private final NavigableMap<UUID, Citizen> citizens = new TreeMap<>();
    private final Map<UUID,Set<UUID>> residents = new HashMap<>();
    private final Map<UUID, Stockpile> stores = new HashMap<>();
    private final Set<ChunkKey> protectedChunks = new HashSet<>();
    private final int maxCamps, maxPopulation, maxDetailed;
    public static final int MAX_PENDING_DAYS = 365;
    private int pendingDays;
    private UUID dayCursor;
    private final Development development = new Development();
    public Development development() { return development; }
    public int population(UUID camp) { return residents.getOrDefault(camp,Set.of()).size(); }
    public int citizenCount() { return citizens.size(); }
    public List<CitizenView> residents(UUID camp) { return residents.getOrDefault(camp,Set.of()).stream().map(id -> required(id).view()).toList(); }
    public void credit(UUID camp, String item, long count) { store(camp).add(item,count); }
    public boolean consume(UUID camp, Map<String,Long> recipe) {
        Stockpile stock = store(camp);
        if (recipe.values().stream().anyMatch(n -> n <= 0)) throw new IllegalArgumentException("recipe");
        if (recipe.entrySet().stream().anyMatch(e -> stock.count(e.getKey()) < e.getValue())) return false;
        recipe.forEach(stock::take); return true;
    }
    public void recruit(UUID citizen, UUID destination) {
        camp(destination); Citizen c = required(citizen); deliver(c); residents.get(c.camp).remove(citizen); c.camp = destination; residents.get(destination).add(citizen);
    }
    private long day;
    private long observedWorldDay = -1;
    public RealmSimulation(int maxCamps, int maxPopulation, int maxDetailed) {
        if (maxCamps < 1 || maxCamps > 1024 || maxPopulation < 1 || maxPopulation > 100_000
                || maxDetailed < 1 || maxDetailed > maxPopulation) throw new IllegalArgumentException("limits");
        this.maxCamps = maxCamps; this.maxPopulation = maxPopulation; this.maxDetailed = maxDetailed;
    }
    public int maxCamps() { return maxCamps; }
    public int maxPopulation() { return maxPopulation; }
    public int maxDetailed() { return maxDetailed; }
    public long day() { return day; }
    public int pendingDays() { return pendingDays; }
    UUID dayCursor() { return dayCursor; }
    public boolean enqueueDays(int days) {
        if (days < 1 || days > MAX_PENDING_DAYS - pendingDays) return false;
        pendingDays += days; return true;
    }
    /** Cancellation preserves completed transfers, but does not count an unfinished day. */
    public int cancelDays() { int old = pendingDays; pendingDays = 0; dayCursor = null; return old; }
    void restoreQueue(int pending, UUID cursor) {
        if (pending < 0 || pending > MAX_PENDING_DAYS || (pending == 0 && cursor != null))
            throw new IllegalArgumentException("Invalid day queue");
        pendingDays = pending; dayCursor = cursor;
    }
    /** One bounded slice of one day. No entity list copy; dead citizens may disappear between slices.
     * Citizens added behind the cursor join the next day. Detailed citizens retain physical ownership.
     */
    public boolean processDaySlice(int maxCitizens) {
        if (maxCitizens < 1) throw new IllegalArgumentException("slice size");
        if (pendingDays == 0) return false;
        long nextDay = Math.incrementExact(day);
        for (int i = 0; i < maxCitizens; i++) {
            var entry = dayCursor == null ? citizens.firstEntry() : citizens.higherEntry(dayCursor);
            if (entry == null) {
                day = nextDay; pendingDays--; dayCursor = null; return true;
            }
            Citizen c = entry.getValue();
            if (c.mode == Mode.ABSTRACT) deliver(c);
            dayCursor = entry.getKey();
        }
        return false;
    }
    public long observedWorldDay() { return observedWorldDay; }
    public int observeWorldDay(long worldDay, int cap) {
        if (worldDay < 0 || cap < 0) throw new IllegalArgumentException("world day/cap");
        if (observedWorldDay < 0) { observedWorldDay = worldDay; return 0; }
        if (worldDay <= observedWorldDay) return 0;
        int elapsed = (int) Math.min((long) cap, worldDay - observedWorldDay);
        observedWorldDay = worldDay;
        return elapsed;
    }
    void restoreObservedWorldDay(long value) {
        if (value < -1) throw new IllegalArgumentException("world day"); observedWorldDay = value;
    }
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
        return camps.size() < maxCamps && !protectedAt(chunk) && !development.claimed(chunk)
                && camps.values().stream().noneMatch(c -> c.territory().equals(chunk));
    }
    public void found(Camp camp) {
        if (camps.containsKey(camp.id()) || !canFound(camp.territory())) throw new IllegalStateException("Camp conflict/limit");
        camps.put(camp.id(), camp); stores.put(camp.id(), new Stockpile()); residents.put(camp.id(),new LinkedHashSet<>()); development.found(camp.id(),camp.territory());
    }
    public void addCitizen(UUID id, UUID camp, double diligence) {
        camp(camp);
        if (citizens.size() >= maxPopulation || citizens.containsKey(id)) throw new IllegalStateException("Citizen conflict/limit");
        citizens.put(id, new Citizen(id, camp, diligence)); residents.get(camp).add(id); development.person(id).species=camp(camp).species();
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
        if (pendingDays != 0) throw new IllegalStateException("Queued simulation in progress");
        long next = Math.incrementExact(day);
        for (Citizen c : citizens.values()) if (c.mode == Mode.ABSTRACT) deliver(c);
        day = next;
    }
    /** Death destroys undelivered cargo; it must not also be dropped by the adapter. */
    public void removeCitizen(UUID id) { residents.get(required(id).camp).remove(id); citizens.remove(id); development.removePerson(id); }
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
