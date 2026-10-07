package dev.mobrealms.core;

import java.util.*;

/** Bounded, repeatable coverage of wilderness around a settlement, independent of claims. */
public final class ResourceSurvey {
    public record Column(int x,int z) {}
    private final List<ChunkKey> chunks=new ArrayList<>();
    private long cursor;
    public ResourceSurvey(ChunkKey origin,int radius,int seed){
        if(radius<0||radius>4)throw new IllegalArgumentException("survey radius");
        for(int ring=0;ring<=radius;ring++)for(int x=-ring;x<=ring;x++)for(int z=-ring;z<=ring;z++)
            if(Math.max(Math.abs(x),Math.abs(z))==ring)chunks.add(new ChunkKey(origin.dimension(),origin.x()+x,origin.z()+z));
        cursor=Math.floorMod(seed,16);
    }
    public Column next(){
        long scan=cursor++;int chunk=(int)((scan/16)%chunks.size());
        int column=(int)(((scan%16)+(scan/(16*chunks.size()))*16)*73%256);
        var c=chunks.get(chunk);return new Column(c.x()*16+(column&15),c.z()*16+(column>>4));
    }
    public int columnsPerPass(){return chunks.size()*256;}
}
