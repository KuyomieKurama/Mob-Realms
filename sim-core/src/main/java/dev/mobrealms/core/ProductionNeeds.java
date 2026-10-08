package dev.mobrealms.core;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/** Outstanding construction demand, excluding already funded abstract work. */
public final class ProductionNeeds {
    private ProductionNeeds() {}
    /** Innate survival rules, independent of species and learned strategy weights. */
    public static boolean needsWorkers(int population,int farms,int starvation){return population<8||farms==0||starvation>0;}
    public static String next(Development.Project project, Map<String,Long> stock) {
        var missing=missing(project,stock);return missing.isEmpty()?null:missing.get(0);
    }
    public static List<String> missing(Development.Project project,Map<String,Long> stock){
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
        var result=new ArrayList<String>();
        for (var entry : needed.entrySet())
            if (stock.getOrDefault(entry.getKey(), 0L) < entry.getValue()) result.add(entry.getKey());
        return result;
    }
}
