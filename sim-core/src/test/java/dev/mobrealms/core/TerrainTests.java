package dev.mobrealms.core;

import dev.mobrealms.core.Development.*;
import java.util.*;
import java.util.function.IntBinaryOperator;

/** Terrain and restart regressions independent of Minecraft's rendering and world runtime. */
public final class TerrainTests {
    private static final UUID TOWN=new UUID(0,1);
    private record Pos(int x,int y,int z){}
    private static final class Ground implements TerrainPlanner.Terrain {
        final IntBinaryOperator heights;
        final Map<Pos,TerrainPlanner.Cell> overrides=new HashMap<>();
        Ground(IntBinaryOperator heights){this.heights=heights;}
        public int surface(int x,int z){return heights.applyAsInt(x,z);}
        public TerrainPlanner.Cell at(int x,int y,int z){return overrides.getOrDefault(new Pos(x,y,z),y<surface(x,z)?new TerrainPlanner.Cell(TerrainPlanner.Kind.GROUND,"minecraft:dirt"):new TerrainPlanner.Cell(TerrainPlanner.Kind.OPEN,"minecraft:air"));}
        void set(int x,int y,int z,TerrainPlanner.Kind kind,String block){overrides.put(new Pos(x,y,z),new TerrainPlanner.Cell(kind,block));}
    }
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static List<Tile> building(){
        var tiles=new ArrayList<Tile>();
        for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)tiles.add(new Tile(x,64,z,"minecraft:cobblestone","minecraft:cobblestone"));
        for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)tiles.add(new Tile(x,70,z,"minecraft:oak_planks","minecraft:oak_planks"));
        return tiles;
    }
    private static Project planned(Ground ground){var result=TerrainPlanner.plan(building(),ground);check(result.accepted(),"normal terrain rejected: "+result.obstacle());return new Project(Building.HOUSE,result.tiles());}
    private static RealmSimulation state(Project project){
        var state=new RealmSimulation(8,1000,100);state.found(new RealmSimulation.Camp(TOWN,"mobrealms:human",new ChunkKey("minecraft:overworld",0,0),8,64,8));state.addCitizen(new UUID(0,10),TOWN,.5);
        state.development().town(TOWN).project=project;return state;
    }
    public static void main(String[] args)throws Exception{
        hills();vegetation();hazards();access();persistence();legacyV4();abstractPreparation();
        System.out.println("Passed 7 terrain scenarios.");
    }
    private static void hills(){
        var flat=planned(new Ground((x,z)->64));check(flat.preparationCount==0,"flat ground charged extra foundation");
        var ground=new Ground((x,z)->64+Math.floorDiv(x,2));var p=planned(ground);
        check(p.tiles.stream().anyMatch(t->t.phase()==WorkPhase.CLEAR),"uphill terrain not cut");
        check(p.tiles.stream().anyMatch(t->t.phase()==WorkPhase.FOUNDATION),"downhill terrain not supported");
        long foundation=p.tiles.stream().filter(t->t.phase()==WorkPhase.FOUNDATION).count();
        long stone=p.tiles.stream().filter(t->t.material().equals("minecraft:cobblestone")).count();
        check(stone>=49+foundation,"foundation material cost missing");
        // Apply each planned terrain operation: no floating floor, leftover wall, or duplicate yield.
        for(var t:p.tiles.subList(0,p.preparationCount)){
            if(t.phase()==WorkPhase.CLEAR)check(ground.at(t.x(),t.y(),t.z()).block().equals(t.expected()),"clearing expectation/order wrong");
            ground.set(t.x(),t.y(),t.z(),t.phase()==WorkPhase.CLEAR?TerrainPlanner.Kind.OPEN:TerrainPlanner.Kind.GROUND,t.block());
        }
        for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++){
            check(ground.at(x,63,z).kind()==TerrainPlanner.Kind.GROUND,"unsupported building pad");
            for(int y=64;y<=72;y++)check(ground.at(x,y,z).kind()==TerrainPlanner.Kind.OPEN,"natural hillside remains inside structure");
        }
        check(p.originY==64&&p.originX==0&&p.originZ==0,"foundation or ramp shifted building anchor");
    }
    private static void vegetation(){
        var terrain=new Ground((x,z)->64);
        for(int y=64;y<=68;y++)terrain.set(0,y,0,TerrainPlanner.Kind.WOOD,"minecraft:cherry_log");
        terrain.set(1,68,0,TerrainPlanner.Kind.PLANT,"minecraft:cherry_leaves");
        var p=planned(terrain);check(p.tiles.stream().filter(t->t.phase()==WorkPhase.CLEAR).count()==6,"vegetation not included in work plan");
        int last=Integer.MAX_VALUE;for(var t:p.tiles)if(t.phase()==WorkPhase.CLEAR){check(t.y()<=last,"removal must be top down");last=t.y();}
        check(terrain.at(0,64,0).block().equals("minecraft:cherry_log"),"planner mutated live terrain");
    }
    private static void hazards(){
        var steep=new Ground((x,z)->x==2?50:64);check(TerrainPlanner.plan(building(),steep).obstacle().equals("terrain_relief"),"cliff accepted without support limit");
        for(var kind:List.of(TerrainPlanner.Kind.FLUID,TerrainPlanner.Kind.BLOCKED,TerrainPlanner.Kind.UNAVAILABLE)){
            var terrain=new Ground((x,z)->64);terrain.set(0,65,0,kind,kind==TerrainPlanner.Kind.FLUID?"minecraft:water":"minecraft:chest");
            check(!TerrainPlanner.plan(building(),terrain).accepted(),"unsafe/unavailable cell accepted: "+kind);
        }
        var cave=new Ground((x,z)->63);for(int y=57;y<=63;y++)cave.set(0,y,0,TerrainPlanner.Kind.OPEN,"minecraft:air");
        check(TerrainPlanner.plan(building(),cave).obstacle().equals("terrain_support"),"hollow foundation accepted");
    }
    private static void access(){
        var terrain=new Ground((x,z)->z< -4?61:64);var p=planned(terrain);
        check(p.tiles.stream().anyMatch(t->t.phase()==WorkPhase.ACCESS),"raised pad has no stepped entrance");
        for(var t:p.tiles.subList(0,p.preparationCount))terrain.set(t.x(),t.y(),t.z(),t.phase()==WorkPhase.CLEAR?TerrainPlanner.Kind.OPEN:TerrainPlanner.Kind.GROUND,t.block());
        int previous=64;
        for(int step=1;step<=6;step++){
            int feet=80;while(terrain.at(0,feet-1,-4-step).kind()!=TerrainPlanner.Kind.GROUND)feet--;
            check(Math.abs(feet-previous)<=1,"entrance contains unclimbable step");
            check(terrain.at(0,feet,-4-step).kind()==TerrainPlanner.Kind.OPEN&&terrain.at(0,feet+1,-4-step).kind()==TerrainPlanner.Kind.OPEN,"entrance has no headroom");previous=feet;
        }
        check(previous==61,"entrance fails to meet surrounding ground");
    }
    private static void persistence()throws Exception{
        var p=planned(new Ground((x,z)->64+Math.floorDiv(x,2)));p.progress=2;p.paid=2;
        var restored=RealmStore.decode(RealmStore.encode(state(p))).development().town(TOWN).project;
        check(restored.tiles.equals(p.tiles)&&restored.progress==2&&restored.paid==2,"earthworks lost across restart");
        check(restored.originY==64&&restored.preparationCount==p.preparationCount,"anchor/phase changed across restart");
        var town=state(p).development().town(TOWN);town.complete(p);check(town.sites.getFirst().y()==64,"completed house anchored at foundation bottom");
        var invalid=planned(new Ground((x,z)->64+Math.floorDiv(x,2)));invalid.paid=1;
        boolean rejected=false;try{RealmStore.decode(RealmStore.encode(state(invalid)));}catch(java.io.IOException expected){rejected=true;}
        check(rejected,"save accepted abstract funding of unfinished earthworks");
    }
    private static void legacyV4()throws Exception{
        byte[] bytes=Base64.getDecoder().decode("TVJMTQAAAAQAAAAIAAAD6AAAAGQAAAAAAAAAAP//////////AAAAAAAAAAABAAAAAAAAAAAAAAAAAAAAAQAPbW9icmVhbG1zOmh1bWFuABNtaW5lY3JhZnQ6b3ZlcndvcmxkAAAACAAAAEAAAAAIAAAAAQAVbWluZWNyYWZ0OmNvYmJsZXN0b25lAAAAAAAAAAwAAAABAAAAAAAAAAAAAAAAAAAACgAAAAAAAAAAAAAAAAAAAAEAAAAAAAAAAD/gAAAAAAAAAAAAAAAAAAAAAAABAAAAAAAAAAAAAAAAAAAAAQABAAAAAAAAAAAAAAAAAAAACgAAAAAAAAABABNtaW5lY3JhZnQ6b3ZlcndvcmxkAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAClBST1NQRVJJVFkAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAP//////////AAAAAAAAAAAABnN1cnZleQEABUhPVVNFAAAAAQAAAAEAAAAAAgAAABQAAABAAAAACAAVbWluZWNyYWZ0OmNvYmJsZXN0b25lABVtaW5lY3JhZnQ6Y29iYmxlc3RvbmUAAAAVAAAAQAAAAAgAFG1pbmVjcmFmdDpvYWtfcGxhbmtzABRtaW5lY3JhZnQ6b2FrX3BsYW5rcwAAAAEAAAAAAAAAAAAAAAAAAAAKAA9tb2JyZWFsbXM6aHVtYW4ADEFyaSBWYWxkb3JpbgAIR0FUSEVSRVIAAAAAAAAAAAAAAAAAAAAAAAAAAAIAAAAAt2gH/Q==");
        var migrated=RealmStore.decode(bytes);var p=migrated.development().town(TOWN).project;
        check(p.progress==1&&p.paid==1&&p.preparationCount==0&&p.originY==64,"legacy project progress changed");
        check(p.tiles.stream().allMatch(t->t.phase()==WorkPhase.BUILD&&t.expected().isEmpty()),"legacy build interpreted as demolition");
        check(migrated.stock(TOWN).get("minecraft:cobblestone")==12,"migration lost inventory");
        var copy=RealmStore.decode(RealmStore.encode(migrated));check(copy.development().town(TOWN).project.tiles.equals(p.tiles),"migrated project not restartable");
    }
    private static void abstractPreparation(){
        var p=planned(new Ground((x,z)->64+Math.floorDiv(x,2)));var state=state(p);state.credit(TOWN,"minecraft:cobblestone",1000);state.credit(TOWN,"minecraft:oak_planks",1000);
        state.advanceDay();state.development().daily(state,TOWN,.2,1,2);
        check(p.paid==0&&p.progress==0&&state.stock(TOWN).get("minecraft:cobblestone")==1000,"abstract simulation fabricated terrain work");
        p.progress=p.paid=p.preparationCount;state.advanceDay();state.development().daily(state,TOWN,.2,1,2);
        check(p.paid>p.preparationCount&&p.progress==p.preparationCount,"prepared structure cannot resume abstract funding");
        check(state.development().town(TOWN).housing()==3,"unfinished structure supplied housing");
    }
}
