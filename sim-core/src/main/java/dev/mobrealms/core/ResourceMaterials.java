package dev.mobrealms.core;

import java.util.*;

/** Explicit basic recipes used both by world extraction and stock accounting. */
public final class ResourceMaterials {
    private ResourceMaterials(){}
    private static final java.util.regex.Pattern WOOD=java.util.regex.Pattern.compile("minecraft:(stripped_)?((oak|birch|spruce|jungle|acacia|dark_oak|mangrove|cherry|pale_oak)_(log|wood)|(crimson|warped)_(stem|hyphae))");
    private static final Set<String> STONE=Set.of("minecraft:stone","minecraft:cobblestone","minecraft:deepslate","minecraft:cobbled_deepslate","minecraft:blackstone","minecraft:netherrack","minecraft:end_stone");
    private static final Set<String> SOIL=Set.of("minecraft:dirt","minecraft:grass_block","minecraft:coarse_dirt","minecraft:rooted_dirt","minecraft:podzol","minecraft:crimson_nylium","minecraft:warped_nylium");
    private static final Set<String> GRASS=Set.of("minecraft:short_grass","minecraft:tall_grass");
    private static final Set<String> IRON=Set.of("minecraft:iron_ore","minecraft:deepslate_iron_ore");
    private static final Set<String> COAL=Set.of("minecraft:coal_ore","minecraft:deepslate_coal_ore");
    public static boolean wood(String id){return WOOD.matcher(id).matches();}
    public static String drop(String block,String wanted){
        return switch(wanted){
            case "minecraft:oak_planks" -> wood(block)?block:null;
            case "minecraft:cobblestone" -> STONE.contains(block)?"minecraft:cobblestone":null;
            case "minecraft:dirt" -> SOIL.contains(block)?"minecraft:dirt":null;
            case "minecraft:wheat_seeds" -> GRASS.contains(block)?"minecraft:wheat_seeds":null;
            case "minecraft:iron_ingot" -> IRON.contains(block)?"minecraft:raw_iron":null;
            case "minecraft:coal" -> COAL.contains(block)?"minecraft:coal":null;
            default -> null;
        };
    }
    public static Map<String,Long> available(Map<String,Long> stock){
        var result=new HashMap<>(stock);
        for(var e:stock.entrySet())if(wood(e.getKey())){
            long extra=Math.min(e.getValue(),Long.MAX_VALUE/4)*4;
            result.compute("minecraft:oak_planks",(k,v)->{long n=v==null?0:v;return n>Long.MAX_VALUE-extra?Long.MAX_VALUE:n+extra;});
        }
        return result;
    }
}
