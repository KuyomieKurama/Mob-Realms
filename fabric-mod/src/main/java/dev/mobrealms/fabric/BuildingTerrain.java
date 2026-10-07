package dev.mobrealms.fabric;

import dev.mobrealms.core.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.levelgen.Heightmap;
import java.util.*;

/** Read-only, bounded plot snapshot. Unknown blocks are obstacles, not demolition permission. */
final class BuildingTerrain implements TerrainPlanner.Terrain {
    private static final Set<String> GROUND=Set.of("dirt","grass_block","coarse_dirt","rooted_dirt","podzol","mycelium","mud",
        "stone","deepslate","granite","diorite","andesite","tuff","calcite","sand","red_sand","sandstone","red_sandstone","gravel",
        "netherrack","blackstone","basalt","end_stone","crimson_nylium","warped_nylium","snow_block","clay","terracotta");
    private final ServerLevel level;
    private final RealmSimulation state;
    private final ChunkKey plot;
    private final int referenceY;
    private final Map<BlockPos,TerrainPlanner.Cell> cells=new HashMap<>();
    private final Map<Long,Integer> surfaces=new HashMap<>();
    BuildingTerrain(ServerLevel level,RealmSimulation state,ChunkKey plot,int referenceY){this.level=level;this.state=state;this.plot=plot;this.referenceY=referenceY;}
    @Override public int surface(int x,int z){
        long key=((long)x<<32)^(z&0xffffffffL);
        return surfaces.computeIfAbsent(key,k->{
            var pos=new BlockPos(x,referenceY,z);
            if(!level.hasChunkAt(pos))return referenceY;
            int top=level.dimension().equals(net.minecraft.world.level.Level.NETHER)?referenceY+9:level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,pos).getY();
            for(int y=top-1;y>=top-32;y--)if(at(x,y,z).kind()==TerrainPlanner.Kind.GROUND)return y+1;
            return top;
        });
    }
    @Override public TerrainPlanner.Cell at(int x,int y,int z){
        BlockPos pos=new BlockPos(x,y,z);
        if(!plot.equals(ChunkKey.fromBlock(plot.dimension(),x,z))||!level.hasChunkAt(pos)||level.isOutsideBuildHeight(pos)||!level.getWorldBorder().isWithinBounds(pos))return new TerrainPlanner.Cell(TerrainPlanner.Kind.UNAVAILABLE,"minecraft:air");
        if(state.protectedAt(plot))return new TerrainPlanner.Cell(TerrainPlanner.Kind.BLOCKED,"minecraft:air");
        // 16 x 16 plot, bounded height. Refuse exceptionally large snapshots.
        if(cells.size()>=8192&&!cells.containsKey(pos))return new TerrainPlanner.Cell(TerrainPlanner.Kind.UNAVAILABLE,"minecraft:air");
        return cells.computeIfAbsent(pos,p->classify(level,p));
    }
    static TerrainPlanner.Cell classify(ServerLevel level,BlockPos pos){
        var block=level.getBlockState(pos);String id=BuiltInRegistries.BLOCK.getKey(block.getBlock()).toString();
        TerrainPlanner.Kind kind;
        if(level.getBlockEntity(pos)!=null)kind=TerrainPlanner.Kind.BLOCKED;
        else if(!block.getFluidState().isEmpty())kind=TerrainPlanner.Kind.FLUID;
        else if(block.isAir())kind=TerrainPlanner.Kind.OPEN;
        else if(ResourceMaterials.wood(id))kind=TerrainPlanner.Kind.WOOD;
        else if(block.is(BlockTags.LEAVES)||block.canBeReplaced())kind=TerrainPlanner.Kind.PLANT;
        else if(id.startsWith("minecraft:")&&GROUND.contains(id.substring(10)))kind=TerrainPlanner.Kind.GROUND;
        else kind=TerrainPlanner.Kind.BLOCKED;
        return new TerrainPlanner.Cell(kind,id);
    }
    static String salvage(String block){
        if(ResourceMaterials.wood(block))return block;
        for(String material:List.of("minecraft:dirt","minecraft:cobblestone","minecraft:wheat_seeds")){
            String item=ResourceMaterials.drop(block,material);if(item!=null)return item;
        }
        // Other terrain is cleared without invented production recipes.
        return null;
    }
}
