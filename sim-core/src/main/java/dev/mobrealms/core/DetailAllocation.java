package dev.mobrealms.core;

import java.util.*;

/** Rotating per-settlement service; one populous or early-loaded town cannot keep all slots. */
public final class DetailAllocation {
    private int nextTown;
    private final Map<UUID,Integer> nextResident=new HashMap<>();
    public Set<UUID> select(Map<UUID,List<UUID>> residents,int capacity){
        if(capacity<0)throw new IllegalArgumentException("capacity");
        var towns=residents.keySet().stream().filter(id->!residents.get(id).isEmpty()).sorted().toList();
        nextResident.keySet().retainAll(towns);Set<UUID> chosen=new LinkedHashSet<>();if(towns.isEmpty())return chosen;
        Map<UUID,Integer> served=new HashMap<>();int start=Math.floorMod(nextTown,towns.size());
        for(int round=0;chosen.size()<capacity;round++){
            boolean added=false;
            for(int offset=0;offset<towns.size()&&chosen.size()<capacity;offset++){
                UUID town=towns.get((start+offset)%towns.size());var people=residents.get(town);
                int count=served.getOrDefault(town,0);if(count>=people.size())continue;
                int index=Math.floorMod(nextResident.getOrDefault(town,0)+count,people.size());
                chosen.add(people.get(index));served.put(town,count+1);added=true;
            }
            if(!added)break;
        }
        for(var entry:served.entrySet())nextResident.put(entry.getKey(),Math.floorMod(nextResident.getOrDefault(entry.getKey(),0)+entry.getValue(),residents.get(entry.getKey()).size()));
        nextTown=(start+Math.max(1,capacity<towns.size()?capacity:1))%towns.size();return chosen;
    }
}
