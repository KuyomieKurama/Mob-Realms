package dev.mobrealms.fabric;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

public record RealmConfig(int maxCamps, int maxPopulation, int maxDetailed, int aiInterval,
                          int workPerTick, long budgetNanos, int graceDays, boolean naturalCamps) {
    public static RealmConfig load(Path path) throws IOException {
        Properties p = new Properties();
        p.setProperty("maxCamps", "8"); p.setProperty("maxPopulation", "1000"); p.setProperty("maxDetailed", "100");
        p.setProperty("aiInterval", "20"); p.setProperty("workPerTick", "8"); p.setProperty("budgetMicros", "2000");
        p.setProperty("graceDays", "3"); p.setProperty("naturalCamps", "true");
        if (Files.exists(path)) { try (Reader reader = Files.newBufferedReader(path)) { p.load(reader); } }
        else { Files.createDirectories(path.getParent()); try (Writer writer = Files.newBufferedWriter(path)) { p.store(writer, "Mob Realms M1; restart server to apply. Limits are fixed in existing realm saves."); } }
        int population = integer(p, "maxPopulation", 1, 100_000);
        String natural = p.getProperty("naturalCamps");
        if (!natural.equals("true") && !natural.equals("false")) throw new IOException("naturalCamps must be true or false");
        return new RealmConfig(integer(p, "maxCamps", 1, 1024), population, integer(p, "maxDetailed", 1, population),
                integer(p, "aiInterval", 5, 200), integer(p, "workPerTick", 1, 100),
                integer(p, "budgetMicros", 100, 10_000) * 1000L, integer(p, "graceDays", 0, 100), Boolean.parseBoolean(natural));
    }
    private static int integer(Properties p, String key, int min, int max) throws IOException {
        try { int v = Integer.parseInt(p.getProperty(key)); if (v >= min && v <= max) return v; }
        catch (NumberFormatException ignored) { }
        throw new IOException("Invalid config: " + key + " must be in [" + min + ", " + max + "]");
    }
}
