package dev.mobrealms.core;

import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/** Dependency-free executable tests; any failed expectation fails Gradle/CI. */
public final class CoreTests {
    private static int passed;
    private static final UUID CAMP = new UUID(0, 1), CITIZEN = new UUID(0, 2);
    public static void main(String[] args) throws Exception {
        test("conservation across random transfers", CoreTests::conservation);
        test("stock overflow is atomic", CoreTests::overflow);
        test("stale leases cannot duplicate cargo", CoreTests::handoff);
        test("detailed limit and protected claims", CoreTests::limits);
        test("save roundtrip invalidates old leases", CoreTests::roundtrip);
        test("corruption rejected; backup retained", CoreTests::persistence);
        test("daily abstraction creates no goods", CoreTests::days);
        test("clock ignores backward jumps and bounds catch-up", CoreTests::clock);
        test("budget resumes fairly", CoreTests::budget);
        test("utility responds to danger and species", CoreTests::utility);
        System.out.println("Passed " + passed + " core scenarios.");
    }
    private static RealmSimulation state() {
        var s = new RealmSimulation(8, 1000, 100);
        s.found(new RealmSimulation.Camp(CAMP, "mobrealms:zombie", ChunkKey.fromBlock("minecraft:overworld", -1, -1), -1, 64, -1));
        s.addCitizen(CITIZEN, CAMP, 0.7); return s;
    }
    private static void conservation() {
        Stockpile a = new Stockpile(), b = new Stockpile(); a.add("minecraft:stone", 1000);
        Random rng = new Random(827);
        for (int i = 0; i < 10_000; i++) {
            if (rng.nextBoolean()) a.transferTo(b, "minecraft:stone", rng.nextInt(100) + 1);
            else b.transferTo(a, "minecraft:stone", rng.nextInt(100) + 1);
            check(a.count("minecraft:stone") + b.count("minecraft:stone") == 1000, "created/lost goods");
        }
        expect(IllegalArgumentException.class, () -> a.take("minecraft:stone", -1));
    }
    private static void overflow() {
        Stockpile a = new Stockpile(), b = new Stockpile(); a.add("minecraft:stone", 1); b.add("minecraft:stone", Long.MAX_VALUE);
        expect(ArithmeticException.class, () -> a.transferTo(b, "minecraft:stone", 1));
        check(a.count("minecraft:stone") == 1 && b.count("minecraft:stone") == Long.MAX_VALUE, "overflow changed stock");
    }
    private static void handoff() {
        var s = state(); var old = s.activate(CITIZEN);
        s.collect(old, "minecraft:bone", 4, 16); s.deactivate(old); s.advanceDay();
        var current = s.activate(CITIZEN);
        expect(IllegalStateException.class, () -> s.collect(old, "minecraft:bone", 4, 16));
        expect(IllegalStateException.class, () -> s.deactivate(old));
        s.deliver(current); s.deliver(current);
        check(s.stock(CAMP).get("minecraft:bone") == 4, "duplicate delivery");
    }
    private static void limits() {
        var s = new RealmSimulation(1, 2, 1); var chunk = new ChunkKey("minecraft:overworld", 0, 0);
        s.protect(chunk); check(!s.canFound(chunk), "protected claim allowed"); s.unprotect(chunk);
        s.found(new RealmSimulation.Camp(CAMP, "mobrealms:zombie", chunk, 0, 64, 0));
        s.addCitizen(CITIZEN, CAMP, .5); var other = new UUID(0, 3); s.addCitizen(other, CAMP, .5);
        s.activate(CITIZEN); expect(IllegalStateException.class, () -> s.activate(other));
        check(ChunkKey.fromBlock("minecraft:overworld", -1, -17).equals(new ChunkKey("minecraft:overworld", -1, -2)), "negative coordinates");
    }
    private static void roundtrip() throws Exception {
        var s = state(); var lease = s.activate(CITIZEN); s.collect(lease, "minecraft:bone", 3, 16);
        s.protect(new ChunkKey("minecraft:the_nether", 4, -5));
        var copy = RealmStore.decode(RealmStore.encode(s));
        check(copy.citizen(CITIZEN).mode() == RealmSimulation.Mode.ABSTRACT, "persisted detailed ownership");
        var renewed = copy.activate(CITIZEN);
        expect(IllegalStateException.class, () -> copy.deliver(lease)); copy.deliver(renewed);
        check(copy.stock(CAMP).get("minecraft:bone") == 3, "lost cargo");
        check(copy.protectedChunks().equals(s.protectedChunks()), "lost protection");
        check(Arrays.equals(RealmStore.encode(copy), RealmStore.encode(RealmStore.decode(RealmStore.encode(copy)))), "noncanonical serialization");
    }
    private static void persistence() throws Exception {
        Path dir = Files.createTempDirectory("mobrealms-tests"); Path file = dir.resolve("realms.dat");
        try {
            var s = state(); RealmStore.save(file, s); s.advanceDay(); RealmStore.save(file, s);
            check(RealmStore.load(file).day() == 1, "save missing");
            check(RealmStore.load(dir.resolve("realms.dat.bak")).day() == 0, "backup missing");
            byte[] bytes = Files.readAllBytes(file); bytes[12] ^= 1;
            expect(java.io.IOException.class, () -> RealmStore.decode(bytes));
            Files.write(file, bytes); expect(java.io.IOException.class, () -> RealmStore.save(file, s));
            check(Arrays.equals(bytes, Files.readAllBytes(file)), "corrupt input overwritten");
        } finally {
            try (var paths = Files.walk(dir)) { for (var p : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(p); }
        }
    }
    private static void days() {
        var s = state(); var l = s.activate(CITIZEN); s.collect(l, "minecraft:bone", 2, 16);
        s.advanceDay(); check(s.stock(CAMP).isEmpty(), "abstract update touched detailed citizen");
        s.deactivate(l); for (int i = 0; i < 100; i++) s.advanceDay();
        check(s.stock(CAMP).get("minecraft:bone") == 2, "abstract update invented resources");
    }
    private static void clock() {
        var s = state();
        check(s.observeWorldDay(0, 7) == 0, "initial clock");
        check(s.observeWorldDay(1, 7) == 1, "sunrise");
        check(s.observeWorldDay(1, 7) == 0, "duplicate sunrise");
        check(s.observeWorldDay(0, 7) == 0, "backward clock");
        check(s.observeWorldDay(1000, 7) == 7, "unbounded catch-up");
        check(s.observeWorldDay(1000, 7) == 0, "repeated catch-up");
    }
    private static void budget() {
        AtomicLong clock = new AtomicLong(); List<Integer> order = new ArrayList<>();
        var q = new TickScheduler(3, clock::get);
        for (int i = 0; i < 3; i++) { final int n = i; check(q.submit(() -> { order.add(n); clock.addAndGet(6); }), "submit"); }
        check(!q.submit(() -> {}), "unbounded queue");
        check(q.run(10, 10) == 2 && q.pending() == 1, "budget not enforced");
        q.run(10, 10); check(order.equals(List.of(0, 1, 2)), "lost/reordered tasks");
    }
    private static void utility() {
        var zombie = new SpeciesProfile("mobrealms:zombie", "minecraft:zombie", 1.2, 1, 16, true);
        var skeleton = new SpeciesProfile("mobrealms:skeleton", "minecraft:skeleton", .8, 2, 16, true);
        var brain = new UtilityBrain();
        var o = new UtilityBrain.Observation(false, false, false, true, 20, .5);
        check(brain.choose(zombie, o) == UtilityBrain.Goal.GATHER, "zombie profile");
        check(brain.choose(skeleton, o) == UtilityBrain.Goal.REGROUP, "skeleton profile");
        check(brain.choose(zombie, new UtilityBrain.Observation(true, false, true, true, 1, .5)) == UtilityBrain.Goal.SHELTER, "sun priority");
        expect(IllegalArgumentException.class, () -> new SpeciesProfile("a:b", "a:c", Double.NaN, 1, 1, true));
    }
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    private static void expect(Class<? extends Throwable> type, Checked work) {
        try { work.run(); } catch (Throwable ex) { if (type.isInstance(ex)) return; throw new AssertionError("Wrong exception", ex); }
        throw new AssertionError("Expected " + type.getSimpleName());
    }
    private static void test(String name, Checked work) throws Exception { work.run(); passed++; System.out.println("PASS " + name); }
    @FunctionalInterface private interface Checked { void run() throws Exception; }
}
