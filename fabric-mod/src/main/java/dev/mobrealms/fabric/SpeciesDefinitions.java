package dev.mobrealms.fabric;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.mobrealms.core.SpeciesProfile;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import java.io.*;
import java.util.*;

public final class SpeciesDefinitions {
    private Map<String, SpeciesProfile> profiles = Map.of();
    private Map<String,Set<String>> dimensions = Map.of();
    public boolean allows(String id,String dimension) { return dimensions.getOrDefault(id,Set.of("minecraft:overworld")).contains(dimension); }
    public SpeciesProfile get(String id) { return Objects.requireNonNull(profiles.get(id), "Unknown species: " + id); }
    public List<String> ids() { return profiles.keySet().stream().sorted().toList(); }
    public void reload(ResourceManager manager) throws IOException {
        Map<String, SpeciesProfile> next = new TreeMap<>();
        Map<String,Set<String>> places=new HashMap<>();
        var resources = manager.listResources("mobrealms/species", id -> id.getPath().endsWith(".json"));
        if (resources.size() > 256) throw new IOException("Too many species definitions");
        for (var entry : resources.entrySet()) {
            try (Reader reader = entry.getValue().openAsReader()) {
                JsonObject j = JsonParser.parseReader(reader).getAsJsonObject();
                if (j.get("schema").getAsInt() != 1) throw new IllegalArgumentException("Unsupported species schema");
                String path = entry.getKey().getPath();
                String id = entry.getKey().getNamespace() + ":" + path.substring("mobrealms/species/".length(), path.length() - 5);
                SpeciesProfile profile = new SpeciesProfile(id, j.get("entity_type").getAsString(),
                        j.get("gather_weight").getAsDouble(), j.get("regroup_weight").getAsDouble(),
                        j.get("carrying_capacity").getAsInt(), j.get("avoids_sun").getAsBoolean());
                if (!BuiltInRegistries.ENTITY_TYPE.containsKey(Identifier.parse(profile.entityType())))
                    throw new IllegalArgumentException("Unknown entity type " + profile.entityType());
                Set<String> allowed=new HashSet<>();
                if(j.has("dimensions"))for(var d:j.getAsJsonArray("dimensions"))allowed.add(d.getAsString());else allowed.add("minecraft:overworld");
                if(allowed.isEmpty()||allowed.size()>16)throw new IllegalArgumentException("dimensions");places.put(id,Set.copyOf(allowed));
                next.put(id, profile);
            } catch (RuntimeException ex) { throw new IOException("Invalid species " + entry.getKey(), ex); }
        }
        if (next.isEmpty()) throw new IOException("No Mob Realms species definitions");
        dimensions=Map.copyOf(places);
        profiles = Map.copyOf(next); // atomic replacement only after every definition passed
    }
}
