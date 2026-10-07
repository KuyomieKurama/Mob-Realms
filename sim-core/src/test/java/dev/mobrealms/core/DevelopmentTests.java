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
        growth();project();trade();diplomacy();learning();save();production();lifecycle();populationCap();eventLog();identities();socialRanks();legacyNames();fairDetail();waitingCargo();basicSkills();
        System.out.println("Passed 16 civilization scenarios.");
    }
    private static void fairDetail(){
        Map<UUID,List<UUID>> towns=new TreeMap<>();Map<UUID,UUID> membership=new HashMap<>();
        for(int t=0;t<8;t++){UUID town=new UUID(1,t);var residents=new ArrayList<UUID>();for(int i=0;i<96;i++){UUID id=new UUID(t+2,i);residents.add(id);membership.put(id,town);}towns.put(town,residents);}
        var allocation=new DetailAllocation();Set<UUID> seen=new HashSet<>();
        for(int window=0;window<8;window++){
            var selected=allocation.select(towns,100);check(selected.size()==100,"detailed budget exceeded or wasted");seen.addAll(selected);
            Set<UUID> served=new HashSet<>();for(UUID id:selected)served.add(membership.get(id));check(served.size()==8,"one settlement monopolized AI slots");
        }
        check(seen.size()==768,"loaded residents permanently starved");
        allocation=new DetailAllocation();Set<UUID> served=new HashSet<>();for(int w=0;w<8;w++)for(UUID id:allocation.select(towns,1))served.add(membership.get(id));check(served.size()==8,"small budget starves settlements");
        check(allocation.select(Map.of(),100).isEmpty(),"empty world allocation");
    }
    private static void waitingCargo(){
        var s=fixture();UUID id=new UUID(0,10);var lease=s.activate(id);s.collect(lease,"minecraft:oak_log",3,16);s.waitForDetail(lease);s.advanceDay();
        check(s.citizen(id).cargo().get("minecraft:oak_log")==3&&s.stock(A).getOrDefault("minecraft:oak_log",0L)==0,"budget pause teleported cargo");
        boolean stale=false;try{s.deliver(lease);}catch(IllegalStateException expected){stale=true;}check(stale,"paused lease remained valid");
        var active=s.activate(id);s.deliver(active);check(s.stock(A).get("minecraft:oak_log")==3,"resumed delivery failed");s.waitForDetail(active);s.markUnloaded(id);check(s.citizen(id).mode()==RealmSimulation.Mode.ABSTRACT,"unload left resident waiting");
    }
    private static void basicSkills(){
        check(ProductionNeeds.needsWorkers(3,0,0),"starter settlement lacks innate worker priority");
        check(ProductionNeeds.needsWorkers(50,0,0),"first farm not prioritized");
        check(ProductionNeeds.needsWorkers(50,5,1),"starvation did not recall workers");
        check(!ProductionNeeds.needsWorkers(50,5,0),"healthy town cannot specialize");
    }
    private static void identities()throws Exception{
        Set<String> names=new HashSet<>();for(long i=1;i<=100000;i++)check(names.add(ResidentNames.fromSequence(i)),"duplicate generated identity");
        var s=fixture();UUID id=new UUID(0,10);String original=s.development().person(id).name;
        s.recruit(id,B);check(s.development().person(id).name.equals(original),"recruit renamed resident");
        check(!id.equals(s.development().town(A).leader)&&id.equals(s.development().town(B).leader),"leadership did not transfer safely");
        var copy=RealmStore.decode(RealmStore.encode(s));check(copy.development().person(id).name.equals(original)&&copy.development().town(B).leader.equals(id),"name/leader lost on restart");
        Set<String> old=new HashSet<>();for(var c:copy.citizens())old.add(copy.development().person(c.id()).name);
        copy.recordDeath(id);var restarted=RealmStore.decode(RealmStore.encode(copy));UUID child=new UUID(0,999);restarted.addCitizen(child,A,.5);check(!old.contains(restarted.development().person(child).name),"dead resident name reused");
    }
    private static void socialRanks(){
        var s=fixture();UUID worker=new UUID(0,11);var p=s.development().person(worker);var t=s.development().town(A);
        check(s.development().socialRank(new UUID(0,10),s).equals("camp_leader"),"founder has no leadership title");
        check(s.development().socialRank(worker,s).equals("camp_civilian_junior"),"wrong camp rank");p.reward(200);
        for(int i=3;i<8;i++)s.addCitizen(new UUID(0,10+i),A,.5);
        check(s.development().socialRank(worker,s).equals("village_civilian_senior"),"village promotion missing");p.role=Role.SOLDIER;
        for(int i=8;i<64;i++)s.addCitizen(new UUID(0,10+i),A,.5);t.buildings.put(Building.MARKET,1);
        check(s.development().socialRank(worker,s).equals("city_military_senior"),"city military promotion missing");
        t.technologies.addAll(EnumSet.of(Technology.AGRICULTURE,Technology.MASONRY,Technology.SHIELDS));check(s.development().socialRank(worker,s).equals("realm_military_senior"),"realm rank missing");
        s.recordDeath(new UUID(0,10));check(t.leader!=null&&t.leader.equals(worker),"leadership not replaced after death");
        check(s.development().socialRank(worker,s).equals("city_leader"),"rank did not reflect succession/population decline");
    }
    private static void legacyNames()throws Exception{
        byte[] old=Base64.getDecoder().decode("TVJMTQAAAAMAAAAIAAAD6AAAAGQAAAAAAAAAAP//////////AAAAAAAAAAABAAAAAAAAAAAAAAAAAAAAAQAPbW9icmVhbG1zOmh1bWFuABNtaW5lY3JhZnQ6b3ZlcndvcmxkAAAACAAAAEAAAAAIAAAAAAAAAAEAAAAAAAAAAAAAAAAAAAAKAAAAAAAAAAAAAAAAAAAAAQAAAAAAAAAAP+AAAAAAAAAAAAAAAAAAAAAAAAEAAAAAAAAAAAAAAAAAAAABAAAAAAAAAAABABNtaW5lY3JhZnQ6b3ZlcndvcmxkAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAClBST1NQRVJJVFkAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAP//////////AAAAAAAAAAAABnN1cnZleQAAAAABAAAAAAAAAAAAAAAAAAAACgAPbW9icmVhbG1zOmh1bWFuAAhHQVRIRVJFUgAAAOkAAAAAAAAAAAAAAAAAcAB8xA==");
        var migrated=RealmStore.decode(old);var person=migrated.development().person(new UUID(0,10));
        check(!person.name.isBlank()&&person.experience==233,"format-3 migration lost experience/name");
        var copy=RealmStore.decode(RealmStore.encode(migrated));check(copy.development().person(new UUID(0,10)).name.equals(person.name),"migrated name changed on restart");
    }
    private static void eventLog()throws Exception{
        var s=fixture();for(int i=0;i<300;i++)s.development().event("growth",A,i);
        var history=s.development().chronicle();check(history.size()==256,"unbounded event history");
        check(history.getFirst().startsWith("44:")&&history.getLast().startsWith("299:"),"event ordering/ring eviction incorrect");
        var restored=RealmStore.decode(RealmStore.encode(s));check(restored.development().chronicle().equals(history),"event history lost on restart");
        boolean immutable=false;try{history.clear();}catch(UnsupportedOperationException expected){immutable=true;}check(immutable,"UI could mutate domain log");
    }
    private static void lifecycle()throws Exception{
        var s=fixture();var t=s.development().town(A);s.credit(A,"minecraft:bread",100);
        UUID dead=new UUID(0,10);var lease=s.activate(dead);s.collect(lease,"minecraft:oak_log",3,16);
        check(s.recordDeath(dead),"death ignored");check(!s.recordDeath(dead),"duplicate death accepted");
        check(s.population(A)==2&&t.losses==1&&t.foodDays==0,"death did not release housing/reset growth");
        check(s.stock(A).getOrDefault("minecraft:oak_log",0L)==0,"dead cargo delivered");
        check(s.development().growthStatus(s,A,2).equals("losses"),"loss reason missing");
        var copy=RealmStore.decode(RealmStore.encode(s));check(!copy.hasCitizen(dead)&&copy.population(A)==2,"dead resident resurrected on restart");
        next(copy);check(copy.population(A)==2,"birth on loss day");next(copy);check(copy.population(A)==3,"population did not recover with food and housing");
        long pending=copy.residents(A).stream().filter(c->copy.development().person(c.id()).pendingSpawn).count();check(pending==1,"birth not awaiting spawn");
        var restarted=RealmStore.decode(RealmStore.encode(copy));check(restarted.residents(A).stream().filter(c->restarted.development().person(c.id()).pendingSpawn).count()==1,"pending birth lost/duplicated on restart");
        UUID living=restarted.residents(A).iterator().next().id();var active=restarted.activate(living);restarted.deactivate(active);check(restarted.population(A)==3,"chunk unload counted as death");
        for(var c:List.copyOf(restarted.residents(A)))restarted.recordDeath(c.id());next(restarted);check(restarted.population(A)==0&&restarted.development().growthStatus(restarted,A,2).equals("abandoned"),"extinct town repopulated from nothing");
    }
    private static void populationCap(){
        var s=new RealmSimulation(1,2,2);s.found(new RealmSimulation.Camp(A,"mobrealms:human",new ChunkKey("minecraft:overworld",0,0),8,64,8));
        s.addCitizen(new UUID(0,10),A,.5);s.addCitizen(new UUID(0,11),A,.5);s.credit(A,"minecraft:bread",20);
        next(s);next(s);next(s);check(s.population(A)==2&&s.development().growthStatus(s,A,2).equals("limit"),"global population cap ignored");
    }
    private static void production(){
        var tiles=List.of(new Tile(0,0,0,"minecraft:oak_planks","minecraft:oak_planks"),
            new Tile(1,0,0,"minecraft:oak_planks","minecraft:oak_planks"),
            new Tile(2,0,0,"minecraft:cobblestone","minecraft:cobblestone"),
            new Tile(3,0,0,"minecraft:water","minecraft:air"));
        var project=new Project(Building.HOUSE,tiles);
        check(ProductionNeeds.next(project,Map.of("minecraft:oak_planks",2L)).equals("minecraft:cobblestone"),"workers gather already stocked next tile instead of missing stone");
        check(ProductionNeeds.next(project,Map.of("minecraft:oak_planks",1L)).equals("minecraft:oak_planks"),"aggregate material demand ignored");
        project.paid=2;
        check(ProductionNeeds.next(project,Map.of("minecraft:cobblestone",1L))==null,"prepaid materials charged twice or air harvested");
        project.paid=tiles.size();check(ProductionNeeds.next(project,Map.of())==null,"fully funded project still triggers harvesting");
        check(ProductionNeeds.next(null,Map.of("minecraft:oak_planks",32L)).equals("minecraft:cobblestone"),"no stone reserve before founding project");
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
