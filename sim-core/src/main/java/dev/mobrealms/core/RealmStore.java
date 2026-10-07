package dev.mobrealms.core;

import java.io.*;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.util.*;
import java.util.zip.CRC32;

/** Versioned bounded binary format. No Java object deserialization. */
public final class RealmStore {
    private static final int MAGIC = 0x4D524C4D, VERSION = 1, MAX_BYTES = 32 * 1024 * 1024;
    private RealmStore() {}
    public static byte[] encode(RealmSimulation state) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeInt(MAGIC); out.writeInt(VERSION);
            out.writeInt(state.maxCamps()); out.writeInt(state.maxPopulation()); out.writeInt(state.maxDetailed());
            out.writeLong(state.day());
            out.writeInt(state.camps().size());
            for (var c : state.camps()) {
                uuid(out, c.id()); out.writeUTF(c.species()); out.writeUTF(c.territory().dimension());
                out.writeInt(c.x()); out.writeInt(c.y()); out.writeInt(c.z()); inventory(out, state.stock(c.id()));
            }
            out.writeInt(state.citizens().size());
            for (var c : state.citizens()) {
                uuid(out, c.id()); uuid(out, c.camp()); out.writeLong(c.generation());
                out.writeDouble(c.diligence()); inventory(out, c.cargo());
            }
            var protectedChunks = state.protectedChunks().stream().sorted(Comparator.comparing(ChunkKey::dimension)
                    .thenComparingInt(ChunkKey::x).thenComparingInt(ChunkKey::z)).toList();
            out.writeInt(protectedChunks.size());
            for (var c : protectedChunks) { out.writeUTF(c.dimension()); out.writeInt(c.x()); out.writeInt(c.z()); }
        }
        byte[] payload = bytes.toByteArray();
        if (payload.length > MAX_BYTES - 8) throw new IOException("Realm save too large");
        CRC32 crc = new CRC32(); crc.update(payload);
        try (DataOutputStream out = new DataOutputStream(bytes)) { out.writeLong(crc.getValue()); }
        return bytes.toByteArray();
    }
    public static RealmSimulation decode(byte[] bytes) throws IOException {
        if (bytes.length < 36 || bytes.length > MAX_BYTES) throw new IOException("Invalid realm save length");
        CRC32 crc = new CRC32(); crc.update(bytes, 0, bytes.length - 8);
        try (DataInputStream checksum = new DataInputStream(new ByteArrayInputStream(bytes, bytes.length - 8, 8))) {
            if (crc.getValue() != checksum.readLong()) throw new IOException("Realm checksum mismatch");
        }
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes, 0, bytes.length - 8))) {
            if (in.readInt() != MAGIC || in.readInt() != VERSION) throw new IOException("Unsupported realm save format");
            RealmSimulation state = new RealmSimulation(in.readInt(), in.readInt(), in.readInt());
            state.restoreDay(in.readLong());
            int camps = count(in, state.maxCamps());
            for (int i = 0; i < camps; i++) {
                UUID id = uuid(in); String species = in.readUTF(), dimension = in.readUTF();
                int x = in.readInt(), y = in.readInt(), z = in.readInt();
                state.found(new RealmSimulation.Camp(id, species, ChunkKey.fromBlock(dimension, x, z), x, y, z));
                inventory(in).forEach((item, amount) -> state.restoreStock(id, item, amount));
            }
            int citizens = count(in, state.maxPopulation());
            for (int i = 0; i < citizens; i++) {
                UUID id = uuid(in), camp = uuid(in); long generation = in.readLong();
                state.addCitizen(id, camp, in.readDouble()); state.restoreGeneration(id, generation);
                Map<String, Long> cargo = inventory(in);
                long held = 0;
                for (long amount : cargo.values()) held = Math.addExact(held, amount);
                if (held > 64) throw new IOException("Invalid saved cargo");
                cargo.forEach((item, amount) -> state.restoreCargo(id, item, amount));
            }
            int protections = count(in, 100_000);
            for (int i = 0; i < protections; i++) state.protect(new ChunkKey(in.readUTF(), in.readInt(), in.readInt()));
            if (in.available() != 0) throw new IOException("Trailing save data");
            return state; // all citizens start ABSTRACT; adapters must acquire fresh leases
        } catch (IllegalArgumentException | IllegalStateException | NullPointerException | ArithmeticException ex) {
            throw new IOException("Invalid realm state", ex);
        }
    }
    public static RealmSimulation load(Path path) throws IOException {
        if (Files.size(path) > MAX_BYTES) throw new IOException("Realm save too large");
        return decode(Files.readAllBytes(path));
    }
    public static void save(Path path, RealmSimulation state) throws IOException {
        byte[] bytes = encode(state);
        Path absolute = path.toAbsolutePath(); Files.createDirectories(absolute.getParent());
        Path temporary = Files.createTempFile(absolute.getParent(), "mobrealms-", ".tmp");
        try {
            Files.write(temporary, bytes);
            try (FileChannel file = FileChannel.open(temporary, StandardOpenOption.WRITE)) { file.force(true); }
            if (Files.exists(absolute)) {
                // Refuse to replace a corrupt previous save silently.
                load(absolute);
                Files.copy(absolute, absolute.resolveSibling(absolute.getFileName() + ".bak"), StandardCopyOption.REPLACE_EXISTING);
            }
            // Same-filesystem atomic rename. Unsupported filesystems fail safely, preserving the old save.
            Files.move(temporary, absolute, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } finally { Files.deleteIfExists(temporary); }
    }
    private static int count(DataInputStream in, int maximum) throws IOException {
        int value = in.readInt(); if (value < 0 || value > maximum) throw new IOException("Invalid collection size"); return value;
    }
    private static void inventory(DataOutputStream out, Map<String, Long> inventory) throws IOException {
        if (inventory.size() > 16_384) throw new IOException("Inventory too large");
        out.writeInt(inventory.size());
        for (var e : new TreeMap<>(inventory).entrySet()) { out.writeUTF(e.getKey()); out.writeLong(e.getValue()); }
    }
    private static Map<String, Long> inventory(DataInputStream in) throws IOException {
        int size = count(in, 16_384); Map<String, Long> result = new TreeMap<>();
        for (int i = 0; i < size; i++) {
            String item = in.readUTF(); long amount = in.readLong();
            if (amount <= 0 || result.putIfAbsent(item, amount) != null) throw new IOException("Invalid inventory");
        }
        return result;
    }
    private static UUID uuid(DataInputStream in) throws IOException { return new UUID(in.readLong(), in.readLong()); }
    private static void uuid(DataOutputStream out, UUID id) throws IOException { out.writeLong(id.getMostSignificantBits()); out.writeLong(id.getLeastSignificantBits()); }
}
