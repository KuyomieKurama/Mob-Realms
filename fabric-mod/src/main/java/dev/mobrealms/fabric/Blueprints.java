package dev.mobrealms.fabric;

import com.google.gson.*;
import dev.mobrealms.core.Development.*;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.*;
import java.io.*;

/** Small JSON blueprints are validated once, then frozen into each paid project. */
public final class Blueprints {
    private Map<Building,List<List<Tile>>> templates = Map.of();
    public void reload(ResourceManager manager) throws IOException {
        Map<Building,List<List<Tile>>> next=new EnumMap<>(Building.class);
        var resources=manager.listResources("mobrealms/buildings",id -> id.getPath().endsWith(".json"));
        for(var e:resources.entrySet().stream().sorted(Map.Entry.comparingByKey(Comparator.comparing(Identifier::toString))).toList()) {
            try(var reader=e.getValue().openAsReader()) {
                var json=JsonParser.parseReader(reader).getAsJsonObject();
                Building kind=Building.valueOf(json.get("kind").getAsString());
                List<Tile> tiles=new ArrayList<>();Set<String> positions=new HashSet<>();
                for(var element:json.getAsJsonArray("blocks")) {
                    var b=element.getAsJsonObject();int x=b.get("x").getAsInt(),y=b.get("y").getAsInt(),z=b.get("z").getAsInt();
                    String block=b.get("block").getAsString(),material=b.get("material").getAsString();
                    if(Math.abs(x)>5||Math.abs(z)>5||y<0||y>10||!positions.add(x+","+y+","+z)
                        ||!BuiltInRegistries.BLOCK.containsKey(Identifier.parse(block))||!BuiltInRegistries.ITEM.containsKey(Identifier.parse(material)))throw new IOException("Invalid blueprint block");
                    tiles.add(new Tile(x,y,z,block,material));
                }
                if(tiles.isEmpty()||tiles.size()>1024)throw new IOException("Invalid blueprint");
                next.computeIfAbsent(kind,ignored->new ArrayList<>()).add(List.copyOf(tiles));
            }catch(RuntimeException ex){throw new IOException("Invalid blueprint: "+e.getKey(),ex);}
        }
        if(next.size()!=Building.values().length)throw new IOException("Missing building blueprint");
        next.replaceAll((kind,variants)->List.copyOf(variants));templates=Map.copyOf(next);
    }
    public List<Tile> at(Building kind,int x,int y,int z,String species) {
        var variants=templates.get(kind);
        int choice=Math.floorMod(Objects.hash(x,z,species),variants.size());
        return variants.get(choice).stream().map(t -> {
            String block=t.block();
            if(block.equals("minecraft:oak_planks"))block=species.endsWith("skeleton")?"minecraft:spruce_planks":species.endsWith("piglin")?"minecraft:crimson_planks":species.endsWith("illager")?"minecraft:dark_oak_planks":"minecraft:oak_planks";
            if(species.endsWith("piglin")){
                if(block.equals("minecraft:water"))block="minecraft:shroomlight";
                if(block.equals("minecraft:farmland"))block="minecraft:crimson_nylium";
                if(block.equals("minecraft:wheat"))block="minecraft:crimson_fungus";
            }
            if(block.equals("minecraft:cobblestone")&&species.endsWith("enderman"))block="minecraft:deepslate_bricks";
            return new Tile(t.x()+x,t.y()+y,t.z()+z,block,t.material());
        }).toList();
    }
}
