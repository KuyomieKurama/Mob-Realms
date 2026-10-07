package dev.mobrealms.core;

import dev.mobrealms.core.Development.*;
import java.util.*;

public final class DevelopmentTests {
    private static final UUID A=new UUID(0,1),B=new UUID(0,2),P=new UUID(0,99);
    private static RealmSimulation fixture(){
        var s=new RealmSimulation(8,1000,100);s.found(new RealmSimulation.Camp(A,"mobrealms:human",new ChunkKey("minecraft:overworld",0,0),8,64,8));
        s.found(new RealmSimulation.Camp(B,"mobrealms:skeleton",new ChunkKey("minecraft:overworld",4,0),72,64,8));
        for(int i=0;i<3;i++)s.addCitizen(new UUID(0,10+i),A,.5);return s;
    }
    private static void check(boolean c,String why){if(!c)throw new AssertionError(why);}
    private static void next(RealmSimulation s){s.advanceDay();s.development().daily(s,A,.2,1,2);}
    public static void main(String[] args)throws Exception{
        growth();project();trade();diplomacy();learning();save();
        System.out.println("Passed 6 civilization scenarios.");
    }
    private static void growth(){
        var s=fixture();var t=s.development().town(A);s.credit(A,"minecraft:bread",20);next(s);next(s);check(s.population(A)==3,"growth without housing");
        t.buildings.put(Building.HOUSE,1);next(s);check(s.population(A)==4,"fed town did not grow");
        check(s.residents(A).stream().anyMatch(c->s.development().person(c.id()).pendingSpawn),"new resident not scheduled for materialization");
        int pop=s.population(A);s.consume(A,Map.of("minecraft:bread",s.stock(A).get("minecraft:bread")));next(s);next(s);check(s.population(A)==pop,"unfed growth");
        t.buildings.put(Building.FARM,1);next(s);check(s.stock(A).getOrDefault("minecraft:bread",0L)>0,"farm not staffed");
    }
    private static void project(){
        var s=fixture();var t=s.development().town(A);var tiles=new ArrayList<Tile>();for(int i=0;i<30;i++)tiles.add(new Tile(i,64,0,"minecraft:oak_planks","minecraft:oak_planks"));t.project=new Project(Building.HOUSE,tiles);
        s.credit(A,"minecraft:oak_planks",20);next(s);next(s);check(t.project.paid==20&&t.project.progress==0,"unloaded work overspent or touched world");
        check(t.housing()==3,"unfinished house supplied beds");s.credit(A,"minecraft:oak_planks",10);next(s);check(t.housing()==7,"paid construction missing housing");next(s);check(t.housing()==7,"duplicate completion");
    }
    private static void trade(){
        var s=fixture();s.credit(A,"minecraft:oak_planks",10);s.credit(B,"minecraft:cobblestone",12);check(!s.development().trade(s,A,B,"minecraft:oak_planks","minecraft:cobblestone",4),"trade without treaty");
        var r=s.development().relation(A,B);r.adjust(20);check(s.development().propose(A,B,Treaty.TRADE),"reasonable offer rejected");check(s.development().trade(s,A,B,"minecraft:oak_planks","minecraft:cobblestone",4),"trade failed");
        check(s.stock(A).get("minecraft:oak_planks")==6&&s.stock(B).get("minecraft:oak_planks")==4,"goods duplicated");check(!s.development().trade(s,A,B,"minecraft:oak_planks","minecraft:cobblestone",9),"overdraft");
        check(s.stock(A).get("minecraft:oak_planks")==6,"failed trade mutated stock");
    }
    private static void diplomacy(){
        var s=fixture();s.development().town(B).owner=P;var r=s.development().relation(A,B);r.adjust(100);check(s.development().propose(A,B,Treaty.VASSAL),"offer failed");check(r.treaty==Treaty.NEUTRAL&&r.offer==Treaty.VASSAL,"player treaty accepted without consent");
        check(s.development().answer(B,A,true,0)&&r.overlord.equals(A),"vassal direction wrong");s.development().propose(B,A,Treaty.WAR);check(r.treaty==Treaty.WAR&&r.overlord==null,"war failed to end vassalage");
        s.development().propose(A,B,Treaty.ALLIANCE);s.development().propose(B,A,Treaty.WAR);check(!s.development().answer(B,A,true,0)&&r.treaty==Treaty.WAR,"stale offer ended war");
        var claim=new ChunkKey("minecraft:overworld",1,0);check(!s.development().claim(A,claim,Set.of(claim)),"protected expansion");check(s.development().claim(A,claim,Set.of()),"contiguous expansion failed");check(!s.development().claim(B,claim,Set.of()),"overlapping territory");
    }
    private static void learning(){
        var s=fixture();var t=s.development().town(A);t.buildings.put(Building.WORKSHOP,1);s.credit(A,"minecraft:cobblestone",100);s.credit(A,"minecraft:bread",500);
        for(int i=0;i<30;i++){t.labor=40;t.rangedHits=2;next(s);}check(t.rangedThreat>.9,"archery history ignored");check(t.technologies.contains(Technology.SHIELDS),"technology never advanced");
        for(double reward:t.rewards)check(Double.isFinite(reward)&&Math.abs(reward)<=1,"unbounded learning");var before=t.strategy;double threat=t.rangedThreat;s.advanceDay();t.meleeHits=30;s.development().daily(s,A,0,1,2);check(t.strategy==before&&t.rangedThreat==threat,"disabled learning changed behavior");
    }
    private static void save()throws Exception{
        var s=fixture();var t=s.development().town(A);t.owner=P;t.reputation(P,30);t.technologies.add(Technology.MASONRY);t.strategy=Strategy.SECURITY;t.project=new Project(Building.STORE,List.of(new Tile(1,64,1,"minecraft:oak_planks","minecraft:oak_planks")));t.project.paid=1;t.project.counted=true;t.buildings.put(Building.STORE,1);
        var p=s.development().person(new UUID(0,10));p.role=Role.SOLDIER;p.reward(333);s.development().town(B).owner=new UUID(0,100);s.development().propose(A,B,Treaty.ALLIANCE);
        var copy=RealmStore.decode(RealmStore.encode(s));check(copy.development().town(A).owner.equals(P)&&copy.development().town(A).project.paid==1,"lost project/owner");check(copy.development().person(new UUID(0,10)).rank()==3,"lost veteran");check(copy.development().relation(A,B).offer==Treaty.ALLIANCE,"lost offer");
        check(copy.population(A)==3&&copy.population(B)==0,"resident index lost");copy.recruit(new UUID(0,10),B);check(copy.population(A)==2&&copy.population(B)==1,"recruit index stale");check(copy.development().person(new UUID(0,10)).species.equals("mobrealms:human"),"recruitment changed species");
        check(Arrays.equals(RealmStore.encode(copy),RealmStore.encode(RealmStore.decode(RealmStore.encode(copy)))),"noncanonical save");
    }
}
