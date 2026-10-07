package dev.mobrealms.fabric;

import java.io.*;
import java.nio.file.*;
import java.util.Properties;

public record RealmConfig(int maxCamps, int maxPopulation, int maxDetailed, int aiInterval,
                          int workPerTick, long budgetNanos, int graceDays, boolean naturalCamps, int naturalIntervalTicks, int naturalAttempts, int growthDays, double learningRate, double learningCeiling, boolean protectPlayerBuilds, double aggression) {
    public static RealmConfig load(Path path) throws IOException {
        Properties p = new Properties();
        p.setProperty("maxCamps", "8"); p.setProperty("maxPopulation", "1000"); p.setProperty("maxDetailed", "100");
        p.setProperty("aiInterval", "20"); p.setProperty("workPerTick", "8"); p.setProperty("budgetMicros", "2000");
        p.setProperty("graceDays", "3"); p.setProperty("naturalCamps", "true");
        p.setProperty("naturalIntervalSeconds", "30"); p.setProperty("naturalAttempts", "8");
        p.setProperty("aggression", "0.25"); p.setProperty("growthDays", "2"); p.setProperty("learningRate", "0.15"); p.setProperty("learningCeiling", "1.0"); p.setProperty("protectPlayerBuilds", "true");
        if (Files.exists(path)) { try (Reader reader = Files.newBufferedReader(path)) { p.load(reader); } }
        else { Files.createDirectories(path.getParent()); try (Writer writer = Files.newBufferedWriter(path)) { p.store(writer, "Mob Realms M1; restart server to apply. Limits are fixed in existing realm saves."); } }
        int population = integer(p, "maxPopulation", 1, 100_000);
        String natural = p.getProperty("naturalCamps");
        if (!natural.equals("true") && !natural.equals("false")) throw new IOException("naturalCamps must be true or false");
        return new RealmConfig(integer(p, "maxCamps", 1, 1024), population, integer(p, "maxDetailed", 1, population),
                integer(p, "aiInterval", 5, 200), integer(p, "workPerTick", 1, 100),
                integer(p, "budgetMicros", 100, 10_000) * 1000L, integer(p, "graceDays", 0, 100), Boolean.parseBoolean(natural),
                integer(p, "naturalIntervalSeconds", 5, 3600) * 20, integer(p, "naturalAttempts", 1, 64), integer(p,"growthDays",1,100),
                decimal(p,"learningRate",0,1), decimal(p,"learningCeiling",0,10), bool(p,"protectPlayerBuilds"), decimal(p,"aggression",0,1));
    }
    private static boolean bool(Properties p,String key)throws IOException{String v=p.getProperty(key);if(!v.equals("true")&&!v.equals("false"))throw new IOException("Invalid "+key);return Boolean.parseBoolean(v);}
    private static double decimal(Properties p,String key,double min,double max)throws IOException{try{double v=Double.parseDouble(p.getProperty(key));if(Double.isFinite(v)&&v>=min&&v<=max)return v;}catch(NumberFormatException ignored){}throw new IOException("Invalid "+key);}
    private static int integer(Properties p, String key, int min, int max) throws IOException {
        try { int v = Integer.parseInt(p.getProperty(key)); if (v >= min && v <= max) return v; }
        catch (NumberFormatException ignored) { }
        throw new IOException("Invalid config: " + key + " must be in [" + min + ", " + max + "]");
    }
}
