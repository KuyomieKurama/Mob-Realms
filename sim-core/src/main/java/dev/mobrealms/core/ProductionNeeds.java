package dev.mobrealms.core;

import java.util.LinkedHashMap;
import java.util.Map;

/** Outstanding construction demand, excluding already funded abstract work. */
public final class ProductionNeeds {
    private ProductionNeeds() {}
    public static String next(Development.Project project, Map<String,Long> stock) {
        Map<String,Long> needed = new LinkedHashMap<>();
        if (project != null) {
            for (int i = project.paid; i < project.tiles.size(); i++) {
                String material = project.tiles.get(i).material();
                if (!material.equals("minecraft:air")) needed.merge(material, 1L, Long::sum);
            }
        } else {
            needed.put("minecraft:oak_planks", 32L);
            needed.put("minecraft:cobblestone", 32L);
        }
        for (var entry : needed.entrySet())
            if (stock.getOrDefault(entry.getKey(), 0L) < entry.getValue()) return entry.getKey();
        return null;
    }
}
