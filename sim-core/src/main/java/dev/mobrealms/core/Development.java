package dev.mobrealms.core;

import java.io.*;
import java.util.*;

/** Server-owned economy, polity and adaptive strategy. No Minecraft dependencies. */
public final class Development {
    public enum Role { GATHERER, MINER, BUILDER, FARMER, GUARD, SOLDIER, TRADER, LEADER }
    public enum Building { FARM, HOUSE, STORE, WORKSHOP, MARKET, WATCHTOWER, WALL }
    public enum Strategy { PROSPERITY, EXPANSION, SECURITY }
    public enum Treaty { WAR, HOSTILE, NEUTRAL, TRADE, NON_AGGRESSION, ALLIANCE, VASSAL }
    public enum Technology { AGRICULTURE, MASONRY, SHIELDS, FLANKING, SIEGE }
    public enum WorkPhase { CLEAR, FOUNDATION, ACCESS, BUILD }
    public record Tile(int x, int y, int z, String block, String material, WorkPhase phase, String expected) {
        public Tile(int x,int y,int z,String block,String material){this(x,y,z,block,material,WorkPhase.BUILD,"");}
        public Tile {
            identifier(block); identifier(material);Objects.requireNonNull(phase);Objects.requireNonNull(expected);
            if(!expected.isEmpty())identifier(expected);
            if(phase==WorkPhase.CLEAR&&(!block.equals("minecraft:air")||!material.equals("minecraft:air")||expected.isEmpty()))throw new IllegalArgumentException("Invalid clearing step");
        }
    }
    public static final class Project {
        public final Building building;
        public final List<Tile> tiles;
        public final int preparationCount, originX, originY, originZ;
        public int progress, paid;
        public boolean counted;
        public Project(Building building, List<Tile> tiles) {
            if (tiles.isEmpty() || tiles.size() > 4096) throw new IllegalArgumentException("blueprint size");
            this.building = building; this.tiles = List.copyOf(tiles);
            var structure=tiles.stream().filter(t->t.phase()==WorkPhase.BUILD).toList();
            if(structure.isEmpty())throw new IllegalArgumentException("Missing structure");
            preparationCount=tiles.size()-structure.size();
            for(int i=0;i<tiles.size();i++)if((i<preparationCount)==(tiles.get(i).phase()==WorkPhase.BUILD))throw new IllegalArgumentException("Interleaved earthworks");
            originX=(structure.stream().mapToInt(Tile::x).min().orElseThrow()+structure.stream().mapToInt(Tile::x).max().orElseThrow())/2;
            originY=structure.stream().mapToInt(Tile::y).min().orElseThrow();
            originZ=(structure.stream().mapToInt(Tile::z).min().orElseThrow()+structure.stream().mapToInt(Tile::z).max().orElseThrow())/2;
        }
        public Tile next() { return progress < tiles.size() ? tiles.get(progress) : null; }
        public String phase(){return next()==null?"complete":next().phase().name().toLowerCase(Locale.ROOT);}
    }
    public static final class Person {
        public String species = "";
        public String name = "";
        public Role role = Role.GATHERER;
        public int experience;
        private final int[] branchExperience = new int[ResidentSkills.Branch.values().length];
        public boolean pendingSpawn;
        public void reward(int points) { experience = Math.min(10000, experience + Math.max(0, points)); }
        public void reward(ResidentSkills.Branch branch,int points) {
            reward(points);
            int index=branch.ordinal();
            branchExperience[index]=Math.min(10000,branchExperience[index]+Math.max(0,points));
        }
        public int skillExperience(ResidentSkills.Branch branch){return branchExperience[branch.ordinal()];}
        public int skillLevel(ResidentSkills.Branch branch){return ResidentSkills.level(skillExperience(branch));}
        public boolean unlocked(String node){
            for(var branch:ResidentSkills.Branch.values()){
                int level=skillLevel(branch);
                if(node.equals(ResidentSkills.root(branch))&&level>=1)return true;
                if(node.equals(ResidentSkills.mastery(branch))&&level>=3)return true;
            }
            return false;
        }
        public int rank() { return Math.min(4, experience / 100); }
        /** Bounded skill contribution for physically completed work. */
        public int workIntervalBonus() { return rank(); }
    }
    public record Site(Building building,int x,int y,int z,boolean active) {}
    public static final class Town {
        public final UUID id;
        public UUID owner;
        public UUID leader;
        public final Map<UUID,Integer> reputation = new HashMap<>();
        public void reputation(UUID player,int delta){if(reputation.size()<10000||reputation.containsKey(player))reputation.put(player,Math.max(-100,Math.min(100,reputation.getOrDefault(player,0)+delta)));}
        public final List<Site> sites = new ArrayList<>();
        public void complete(Project p){
            if(p.counted)return;p.counted=true;buildings.merge(p.building,1,Integer::sum);
            sites.add(new Site(p.building,p.originX,p.originY,p.originZ,true));
        }
        public final Set<ChunkKey> claims = new LinkedHashSet<>();
        public final EnumMap<Building,Integer> buildings = new EnumMap<>(Building.class);
        public final EnumSet<Technology> technologies = EnumSet.noneOf(Technology.class);
        public final double[] rewards = new double[3];
        public final int[] trials = new int[3];
        public Strategy strategy = Strategy.PROSPERITY;
        public Project project;
        public int foodDays, starvation, births, research, labor, losses, rangedHits, meleeHits;
        public long lastDay = -1;
        public double rangedThreat;
        public String obstacle = "survey";
        public Town(UUID id, ChunkKey origin) { this.id = id; claims.add(origin); }
        public int count(Building b) { return buildings.getOrDefault(b, 0); }
        public int housing() { return 3 + count(Building.HOUSE) * 4; }
        public String stage(int population) { return population >= 64 && count(Building.MARKET) > 0 && technologies.size()>=3 ? "realm" : population >= 24 && count(Building.MARKET) > 0 ? "city" : population >= 8 ? "village" : "camp"; }
        public Building objective(int population) {
            if (count(Building.FARM) == 0) return Building.FARM;
            if (count(Building.FARM) * 6 < population || (strategy == Strategy.PROSPERITY && starvation > 0 && count(Building.FARM) * 6 < population + 3)) return Building.FARM;
            if (count(Building.STORE) == 0) return Building.STORE;
            if (strategy == Strategy.EXPANSION && housing() <= population) return Building.HOUSE;
            if (count(Building.WORKSHOP) == 0) return Building.WORKSHOP;
            if (strategy == Strategy.SECURITY && count(Building.WATCHTOWER) == 0 && (losses > 0 || rangedThreat > .35)) return Building.WATCHTOWER;
            if (housing() <= population) return Building.HOUSE;
            if (count(Building.MARKET) == 0) return Building.MARKET;
            if (technologies.contains(Technology.MASONRY) && count(Building.WALL) == 0) return Building.WALL;
            return Building.HOUSE;
        }
    }
    public static final class Relation {
        public int score;
        public Treaty treaty = Treaty.NEUTRAL;
        public UUID overlord;
        public long lastTradeDay = -1;
        public Treaty offer;
        public UUID proposer;
        public long expires;
        public void adjust(int delta) { score = Math.max(-100, Math.min(100, score + delta)); }
    }
    private final Map<UUID,Town> towns = new LinkedHashMap<>();
    private final Map<UUID,Person> people = new HashMap<>();
    private final Map<String,Relation> relations = new TreeMap<>();
    private long nextNameSequence=1;
    private final List<String> chronicle = new ArrayList<>();
    public Town town(UUID id) { return Objects.requireNonNull(towns.get(id), "Unknown town"); }
    public Collection<Town> towns() { return Collections.unmodifiableCollection(towns.values()); }
    public Person person(UUID id) { return people.computeIfAbsent(id, key -> new Person()); }
    public void nameResident(UUID id){
        Person p=person(id);if(!p.name.isEmpty())return;
        if(nextNameSequence==Long.MAX_VALUE)throw new IllegalStateException("Name sequence exhausted");
        p.name=ResidentNames.fromSequence(nextNameSequence++);
    }
    public String socialRank(UUID resident,RealmSimulation state){
        Person p=person(resident);String stage=town(state.citizen(resident).camp()).stage(state.population(state.citizen(resident).camp()));
        if(resident.equals(town(state.citizen(resident).camp()).leader))return stage+"_leader";
        String group=p.role==Role.GUARD||p.role==Role.SOLDIER?"military":"civilian";
        return stage+"_"+group+(p.experience>=200?"_senior":"_junior");
    }
    public void removePerson(UUID id) { people.remove(id); }
    public void found(UUID id, ChunkKey chunk) { towns.put(id, new Town(id, chunk)); }
    public Optional<UUID> nation(UUID player) { return towns.values().stream().filter(t -> player.equals(t.owner)).map(t -> t.id).findFirst(); }
    public boolean claim(UUID id, ChunkKey chunk, Set<ChunkKey> protectedChunks) {
        Town town = town(id);
        if (town.claims.size() >= 64 || protectedChunks.contains(chunk) || towns.values().stream().anyMatch(t -> t.claims.contains(chunk))) return false;
        boolean adjacent = town.claims.stream().anyMatch(c -> c.dimension().equals(chunk.dimension()) && Math.abs(c.x()-chunk.x()) + Math.abs(c.z()-chunk.z()) == 1);
        if (!adjacent) return false;
        town.claims.add(chunk); return true;
    }
    public boolean claimed(ChunkKey chunk) { return towns.values().stream().anyMatch(t -> t.claims.contains(chunk)); }
    private String pair(UUID a, UUID b) {
        if (a.equals(b)) throw new IllegalArgumentException("self relation");
        town(a); town(b); return a.compareTo(b) < 0 ? a + "/" + b : b + "/" + a;
    }
    public Relation relation(UUID a, UUID b) { return relations.computeIfAbsent(pair(a,b), k -> new Relation()); }
    public boolean propose(UUID from, UUID to, Treaty proposed) {
        Relation r = relation(from,to);
        if (proposed == Treaty.WAR) { r.treaty = proposed; r.overlord = null; r.offer = null; r.proposer = null; r.adjust(-40); return true; }
        if(town(to).owner!=null){r.offer=proposed;r.proposer=from;r.expires=Math.max(town(from).lastDay,town(to).lastDay)+3;return true;}
        int required = switch(proposed) { case TRADE -> 10; case NON_AGGRESSION -> 20; case ALLIANCE -> 50; case VASSAL -> 80; default -> -20; };
        if (r.score < required || (r.treaty == Treaty.WAR && proposed != Treaty.NEUTRAL)) return false;
        r.treaty = proposed; r.overlord = proposed == Treaty.VASSAL ? from : null; return true;
    }
    public boolean answer(UUID from,UUID other,boolean accept,long day){
        Relation r=relation(from,other);if(r.offer==null||!other.equals(r.proposer)||day>r.expires)return false;
        if(accept){r.treaty=r.offer;r.overlord=r.treaty==Treaty.VASSAL?other:null;}
        r.offer=null;r.proposer=null;return true;
    }
    public List<String> chronicle() { return List.copyOf(chronicle); }
    public void event(String type, UUID town, long day) {
        chronicle.add(day + ":" + type + ":" + town);
        if (chronicle.size() > 256) chronicle.remove(0);
    }
    public String growthStatus(RealmSimulation state, UUID id, int growthDays) {
        Town t=town(id);int population=state.population(id);
        if(population==0)return "abandoned";
        if(population<2)return "residents";
        if(t.starvation>0)return "food";
        if(population>=t.housing())return "housing";
        if(state.citizenCount()>=state.maxPopulation())return "limit";
        if(t.losses>0)return "losses";
        return t.foodDays>=growthDays?"ready":"waiting";
    }
    public void daily(RealmSimulation state, UUID id, double learningRate, double ceiling, int growthDays) {
        Town town = town(id); if (town.lastDay >= state.day()) return;
        town.lastDay = state.day(); int population = state.population(id);
        if(population==0){town.obstacle="abandoned";return;}
        if(town.project!=null&&state.residents(id).stream().allMatch(c->c.mode()==RealmSimulation.Mode.ABSTRACT)){
            Project p=town.project;
            for(int i=0;i<Math.min(32,population*4)&&p.paid<p.tiles.size();i++){
                if(p.progress<p.preparationCount)break; // Earthworks require a loaded builder and checked terrain.

                Tile tile=p.tiles.get(p.paid);if(state.protectedAt(ChunkKey.fromBlock(state.camp(id).territory().dimension(),tile.x(),tile.z())))break;if(!tile.material().equals("minecraft:air")&&!state.consume(id,Map.of(tile.material(),1L)))break;p.paid++;town.labor++;
            }
            if(p.paid==p.tiles.size()&&!p.counted){town.complete(p);event("building",id,state.day());}
        }
        // A farm is a renewable, finite-capacity production asset. Workers tend it; no ore is invented.
        if(town.owner==null&&town.count(Building.FARM)>0&&state.residents(id).stream().noneMatch(c->person(c.id()).role==Role.FARMER)){
            state.residents(id).stream().filter(c->person(c.id()).role!=Role.BUILDER).findFirst().ifPresent(c->person(c.id()).role=Role.FARMER);
        }
        int farmers = (int)state.residents(id).stream().filter(c -> person(c.id()).role == Role.FARMER).count();
        long harvest = (long)town.count(Building.FARM) * Math.max(0, farmers) * 3;
        harvest = Math.min(harvest, town.count(Building.FARM) * 12L);
        if(farmers>0&&town.technologies.contains(Technology.AGRICULTURE))harvest+=town.count(Building.FARM)*2L;
        if (harvest > 0) {state.credit(id,"minecraft:bread",harvest);state.credit(id,"minecraft:wheat_seeds",town.count(Building.FARM)*4L);}
        int consumption = Math.max(1,(population + 2)/3);
        boolean fed = state.consume(id, Map.of("minecraft:bread",(long)consumption));
        if (fed) { town.foodDays++; town.starvation = 0; } else { town.starvation++; town.foodDays = 0; }
        if (population>=2 && fed && town.foodDays >= growthDays && population < town.housing() && state.citizenCount() < state.maxPopulation() && town.losses == 0) {
            UUID child = UUID.randomUUID(); state.addCitizen(child,id,.5);
            var person = person(child); person.pendingSpawn = true;
            person.role = Role.values()[Math.floorMod(population,7)]; town.foodDays = 0; town.births++;
            event("growth",id,state.day());
        }
        // Residents learn from productive work even before a workshop exists. The workshop
        // accelerates experiments, while starvation and idleness cannot create knowledge.
        if (fed || town.labor > 0) {
            int insight = town.count(Building.WORKSHOP) > 0 ? 2 + Math.min(10,town.labor/4) : 1 + Math.min(3,town.labor/8);
            town.research = Math.min(10000, town.research + insight);
        }
        int index = town.technologies.size();
        boolean materials = false;
        if (index < Technology.values().length && town.research >= 10 * (index+1)) {
            // Farming research can be learned from the farm's actual seed harvest; old
            // settlements with stone reserves retain their existing research path.
            materials = index == 0 && state.consume(id,Map.of("minecraft:wheat_seeds",8L));
            if (!materials) materials = state.consume(id,Map.of("minecraft:cobblestone",4L*(index+1)));
        }
        if (materials) {
            town.technologies.add(Technology.values()[index]); town.research -= 10*(index+1); event("technology",id,state.day());
        }
        double reward = Math.max(-1, Math.min(1, (fed ? .3 : -.5) + Math.min(.5,town.labor/40.0) - town.losses*.3));
        int arm = town.strategy.ordinal(); town.trials[arm] = Math.min(1000000,town.trials[arm]+1);
        town.rewards[arm] = Math.max(-ceiling, Math.min(ceiling, town.rewards[arm] + learningRate * (reward-town.rewards[arm])));
        if (town.rangedHits + town.meleeHits > 0) town.rangedThreat += learningRate * ((double)town.rangedHits/(town.rangedHits+town.meleeHits)-town.rangedThreat);
        int total = Arrays.stream(town.trials).sum(); int chosen = 0; double best = -Double.MAX_VALUE;
        for (int i=0;i<3;i++) {
            double ucb = town.trials[i] == 0 ? 100 : town.rewards[i] + Math.sqrt(2*Math.log(total+1.0)/town.trials[i]);
            if (ucb > best) { best = ucb; chosen = i; }
        }
        if (learningRate > 0) town.strategy = Strategy.values()[chosen];
        town.labor = town.losses = town.rangedHits = town.meleeHits = 0;
    }
    /** Bilateral barter conserves both inventories. Only agreements permit automatic trade. */
    public boolean trade(RealmSimulation state, UUID a, UUID b, String give, String receive, int amount) {
        if (amount < 1 || amount > 64 || give.equals(receive)) return false;
        Relation r = relation(a,b);
        if (!(r.treaty == Treaty.TRADE || r.treaty == Treaty.ALLIANCE || r.treaty == Treaty.VASSAL)) return false;
        if (state.stock(a).getOrDefault(give,0L) < amount || state.stock(b).getOrDefault(receive,0L) < amount) return false;
        if (state.stock(b).getOrDefault(give,0L) > Long.MAX_VALUE-amount || state.stock(a).getOrDefault(receive,0L) > Long.MAX_VALUE-amount) return false;
        state.consume(a,Map.of(give,(long)amount)); state.consume(b,Map.of(receive,(long)amount));
        state.credit(b,give,amount); state.credit(a,receive,amount); r.adjust(1); r.lastTradeDay=state.day(); return true;
    }
    public void write(DataOutputStream out) throws IOException {
        out.writeInt(towns.size());
        for (Town t:towns.values()) {
            uuid(out,t.id); out.writeBoolean(t.owner!=null); if(t.owner!=null)uuid(out,t.owner);out.writeBoolean(t.leader!=null);if(t.leader!=null)uuid(out,t.leader);
            out.writeInt(t.reputation.size());for(var e:new TreeMap<>(t.reputation).entrySet()){uuid(out,e.getKey());out.writeInt(e.getValue());}
            out.writeInt(t.claims.size()); for(var c:t.claims){out.writeUTF(c.dimension());out.writeInt(c.x());out.writeInt(c.z());}
            out.writeInt(t.sites.size());for(var site:t.sites){out.writeUTF(site.building().name());out.writeInt(site.x());out.writeInt(site.y());out.writeInt(site.z());out.writeBoolean(site.active());}
            for(var b:Building.values())out.writeInt(t.count(b));
            out.writeInt(t.technologies.size()); for(var tech:t.technologies)out.writeUTF(tech.name());
            out.writeUTF(t.strategy.name()); for(int i=0;i<3;i++){out.writeDouble(t.rewards[i]);out.writeInt(t.trials[i]);}
            out.writeInt(t.foodDays);out.writeInt(t.starvation);out.writeInt(t.births);out.writeInt(t.research);out.writeInt(t.labor);out.writeInt(t.losses);out.writeInt(t.rangedHits);out.writeInt(t.meleeHits);out.writeLong(t.lastDay);out.writeDouble(t.rangedThreat);out.writeUTF(t.obstacle);
            out.writeBoolean(t.project!=null);if(t.project!=null){var p=t.project;out.writeUTF(p.building.name());out.writeInt(p.progress);out.writeInt(p.paid);out.writeBoolean(p.counted);out.writeInt(p.tiles.size());for(var x:p.tiles){out.writeInt(x.x());out.writeInt(x.y());out.writeInt(x.z());out.writeUTF(x.block());out.writeUTF(x.material());out.writeUTF(x.phase().name());out.writeUTF(x.expected());}}
        }
        out.writeInt(people.size());for(var e:new TreeMap<>(people).entrySet()){uuid(out,e.getKey());var p=e.getValue();out.writeUTF(p.species);out.writeUTF(p.name);out.writeUTF(p.role.name());out.writeInt(p.experience);out.writeBoolean(p.pendingSpawn);for(var branch:ResidentSkills.Branch.values())out.writeInt(p.skillExperience(branch));}
        out.writeInt(relations.size());for(var e:relations.entrySet()){out.writeUTF(e.getKey());var r=e.getValue();out.writeInt(r.score);out.writeUTF(r.treaty.name());out.writeBoolean(r.overlord!=null);if(r.overlord!=null)uuid(out,r.overlord);out.writeLong(r.lastTradeDay);out.writeUTF(r.offer==null?"":r.offer.name());if(r.offer!=null){uuid(out,r.proposer);out.writeLong(r.expires);}}
        out.writeInt(chronicle.size());for(String line:chronicle)out.writeUTF(line);out.writeLong(nextNameSequence);
    }
    public void read(DataInputStream in, RealmSimulation state, int version) throws IOException {
        int size=bounded(in,1024); if(size!=towns.size())throw new IOException("Town count mismatch");
        Set<UUID> seen=new HashSet<>();Set<ChunkKey> occupied=new HashSet<>();
        for(int n=0;n<size;n++){
            UUID id=uuid(in);if(!seen.add(id))throw new IOException("Duplicate town");Town t=town(id);t.owner=in.readBoolean()?uuid(in):null;if(version>=4){t.leader=in.readBoolean()?uuid(in):null;if(t.leader!=null&&(!state.hasCitizen(t.leader)||!state.citizen(t.leader).camp().equals(id)))throw new IOException("Invalid leader");}
            int memories=bounded(in,10000);for(int i=0;i<memories;i++){UUID player=uuid(in);int score=in.readInt();if(score< -100||score>100||t.reputation.put(player,score)!=null)throw new IOException("Invalid reputation");}
            t.claims.clear();int claims=bounded(in,64);if(claims==0)throw new IOException("No origin claim");for(int i=0;i<claims;i++){var c=new ChunkKey(in.readUTF(),in.readInt(),in.readInt());if(!occupied.add(c))throw new IOException("Overlapping claims");t.claims.add(c);}
            if(!t.claims.contains(state.camp(id).territory()))throw new IOException("Lost origin");
            int sites=bounded(in,1024);for(int i=0;i<sites;i++)t.sites.add(new Site(Building.valueOf(in.readUTF()),in.readInt(),in.readInt(),in.readInt(),in.readBoolean()));
            for(var b:Building.values())t.buildings.put(b,bounded(in,1024));int techs=bounded(in,5);for(int i=0;i<techs;i++)t.technologies.add(Technology.valueOf(in.readUTF()));
            t.strategy=Strategy.valueOf(in.readUTF());for(int i=0;i<3;i++){t.rewards[i]=finite(in,-100,100);t.trials[i]=bounded(in,1000000);}
            t.foodDays=bounded(in,1000000);t.starvation=bounded(in,1000000);t.births=bounded(in,100000);t.research=bounded(in,10000);t.labor=bounded(in,1000000);t.losses=bounded(in,100000);t.rangedHits=bounded(in,1000000);t.meleeHits=bounded(in,1000000);t.lastDay=in.readLong();t.rangedThreat=finite(in,0,1);t.obstacle=in.readUTF();
            if(in.readBoolean()){Building b=Building.valueOf(in.readUTF());int progress=bounded(in,4096),paid=bounded(in,4096);boolean counted=in.readBoolean();int tiles=bounded(in,4096);List<Tile> list=new ArrayList<>();for(int i=0;i<tiles;i++){int x=in.readInt(),y=in.readInt(),z=in.readInt();String block=in.readUTF(),material=in.readUTF();list.add(version>=5?new Tile(x,y,z,block,material,WorkPhase.valueOf(in.readUTF()),in.readUTF()):new Tile(x,y,z,block,material));}t.project=new Project(b,list);if(progress>paid||paid>tiles||(counted&&paid!=tiles)||(progress<t.project.preparationCount&&paid>progress))throw new IOException("Invalid project progress");t.project.progress=progress;t.project.paid=paid;t.project.counted=counted;}
        }
        people.clear();int persons=bounded(in,100000);if(persons!=state.citizenCount())throw new IOException("Citizen count mismatch");for(int i=0;i<persons;i++){UUID id=uuid(in);if(!state.hasCitizen(id)||people.containsKey(id))throw new IOException("Invalid citizen development");var p=new Person();p.species=in.readUTF();identifier(p.species);if(version>=4){p.name=in.readUTF();if(p.name.isBlank()||p.name.length()>100)throw new IOException("Invalid resident name");}p.role=Role.valueOf(in.readUTF());p.experience=bounded(in,10000);p.pendingSpawn=in.readBoolean();if(version>=6){for(var branch:ResidentSkills.Branch.values())p.branchExperience[branch.ordinal()]=bounded(in,10000);}else p.branchExperience[ResidentSkills.branch(p.role).ordinal()]=p.experience;people.put(id,p);}
        int pairs=bounded(in,523776);for(int i=0;i<pairs;i++){String key=in.readUTF();String[] ids=key.split("/");if(ids.length!=2||!key.equals(pair(UUID.fromString(ids[0]),UUID.fromString(ids[1]))))throw new IOException("Invalid relation");var r=new Relation();r.score=in.readInt();if(r.score< -100||r.score>100)throw new IOException("Invalid score");r.treaty=Treaty.valueOf(in.readUTF());r.overlord=in.readBoolean()?uuid(in):null;r.lastTradeDay=in.readLong();String offer=in.readUTF();if(!offer.isEmpty()){r.offer=Treaty.valueOf(offer);r.proposer=uuid(in);r.expires=in.readLong();if(!r.proposer.toString().equals(ids[0])&&!r.proposer.toString().equals(ids[1]))throw new IOException("Invalid proposer");}if(relations.put(key,r)!=null)throw new IOException("Duplicate relation");}
        int history=bounded(in,256);for(int i=0;i<history;i++)chronicle.add(in.readUTF());
        if(version>=4){
            nextNameSequence=in.readLong();if(nextNameSequence<=people.size())throw new IOException("Invalid name sequence");
            Set<String> names=new HashSet<>();for(var person:people.values())if(!names.add(person.name))throw new IOException("Duplicate resident name");
        }else{
            nextNameSequence=1;for(UUID id:new TreeSet<>(people.keySet()))nameResident(id);
        }
    }
    private static int bounded(DataInputStream in,int max)throws IOException{int x=in.readInt();if(x<0||x>max)throw new IOException("Invalid count");return x;}
    private static double finite(DataInputStream in,double min,double max)throws IOException{double x=in.readDouble();if(!Double.isFinite(x)||x<min||x>max)throw new IOException("Invalid value");return x;}
    private static UUID uuid(DataInputStream in)throws IOException{return new UUID(in.readLong(),in.readLong());}
    private static void uuid(DataOutputStream out,UUID id)throws IOException{out.writeLong(id.getMostSignificantBits());out.writeLong(id.getLeastSignificantBits());}
    private static void identifier(String id){if(id==null||!id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+"))throw new IllegalArgumentException("identifier");}
}
