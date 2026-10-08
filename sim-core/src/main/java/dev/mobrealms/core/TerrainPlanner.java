package dev.mobrealms.core;

import dev.mobrealms.core.Development.*;
import java.util.*;

/** Plans cut/fill and a stepped entrance against a read-only terrain snapshot. No world effects. */
public final class TerrainPlanner {
    public enum Kind { OPEN, PLANT, GROUND, WOOD, BLOCKED, FLUID, UNAVAILABLE }
    public record Cell(Kind kind,String block) { public Cell {Objects.requireNonNull(kind);Objects.requireNonNull(block);} }
    public interface Terrain {
        /** First air above natural soil/stone, ignoring vegetation. */
        int surface(int x,int z);
        Cell at(int x,int y,int z);
    }
    public record Result(List<Tile> tiles,String obstacle) {public boolean accepted(){return !tiles.isEmpty();}}
    private record Pos(int x,int y,int z) {}
    private static final int CUT=4,FILL=6,ACCESS_LENGTH=5;
    private TerrainPlanner(){}
    public static Result plan(List<Tile> structure,Terrain terrain){
        if(structure.isEmpty())return new Result(List.of(),"terrain");
        int minX=structure.stream().mapToInt(Tile::x).min().orElseThrow(),maxX=structure.stream().mapToInt(Tile::x).max().orElseThrow();
        int minZ=structure.stream().mapToInt(Tile::z).min().orElseThrow(),maxZ=structure.stream().mapToInt(Tile::z).max().orElseThrow();
        int floor=structure.stream().mapToInt(Tile::y).min().orElseThrow(),roof=structure.stream().mapToInt(Tile::y).max().orElseThrow();
        if(maxX-minX>10||maxZ-minZ>10||roof-floor>10)return new Result(List.of(),"terrain");
        var clear=new LinkedHashMap<Pos,Tile>();var fill=new LinkedHashMap<Pos,Tile>();var access=new LinkedHashMap<Pos,Tile>();
        try{
            for(int x=minX;x<=maxX;x++)for(int z=minZ;z<=maxZ;z++){
                column(terrain,x,z,floor-1,roof+2,WorkPhase.FOUNDATION,clear,fill);
            }
            int middle=(minX+maxX)/2;
            int end=terrain.surface(middle,minZ-ACCESS_LENGTH);
            if(Math.abs(end-floor)>ACCESS_LENGTH)throw new Rejected("terrain_relief");
            for(int step=1;step<=ACCESS_LENGTH;step++){
                int feet=floor+Integer.signum(end-floor)*Math.min(step,Math.abs(end-floor));
                for(int dx=-1;dx<=1;dx++)column(terrain,middle+dx,minZ-step,feet-1,feet+2,WorkPhase.ACCESS,clear,access);
            }
            var tiles=new ArrayList<Tile>();
            // Top down removal prevents gravel/sand falling into uncleared lower steps.
            clear.values().stream().sorted(Comparator.comparingInt(Tile::y).reversed()).forEach(tiles::add);
            fill.values().stream().sorted(Comparator.comparingInt(Tile::y)).forEach(tiles::add);
            access.values().stream().sorted(Comparator.comparingInt(Tile::y)).forEach(tiles::add);
            tiles.addAll(structure);
            if(tiles.size()>4096)throw new Rejected("terrain_relief");
            return new Result(List.copyOf(tiles),"");
        }catch(Rejected e){return new Result(List.of(),e.reason);}
    }
    private static void column(Terrain terrain,int x,int z,int top,int ceiling,WorkPhase phase,Map<Pos,Tile> clear,Map<Pos,Tile> fill){
        int surface=terrain.surface(x,z);
        if(surface-(top+1)>CUT||(top+1)-surface>FILL)throw new Rejected("terrain_relief");
        // A continuous foundation must reach actual natural ground, never a hidden cave or water.
        int ground=top;
        for(;ground>=top-FILL;ground--){
            Cell c=checked(terrain.at(x,ground,z));
            if(c.kind()==Kind.GROUND)break;
            if(c.kind()!=Kind.OPEN)addClear(clear,x,ground,z,c);
        }
        if(ground<top-FILL)throw new Rejected("terrain_support");
        for(int y=ceiling;y>top;y--){Cell c=checked(terrain.at(x,y,z));if(c.kind()!=Kind.OPEN)addClear(clear,x,y,z,c);}
        for(int y=ground+1;y<=top;y++)fill.put(new Pos(x,y,z),new Tile(x,y,z,"minecraft:cobblestone","minecraft:cobblestone",phase,""));
    }
    private static Cell checked(Cell cell){
        if(cell.kind()==Kind.UNAVAILABLE)throw new Rejected("land");
        if(cell.kind()==Kind.BLOCKED)throw new Rejected("terrain_obstacle");
        if(cell.kind()==Kind.FLUID)throw new Rejected("terrain_water");
        return cell;
    }
    private static void addClear(Map<Pos,Tile> clear,int x,int y,int z,Cell c){
        clear.put(new Pos(x,y,z),new Tile(x,y,z,"minecraft:air","minecraft:air",WorkPhase.CLEAR,c.block()));
    }
    private static final class Rejected extends RuntimeException {
        final String reason;Rejected(String reason){super(reason,null,false,false);this.reason=reason;}
    }
}
