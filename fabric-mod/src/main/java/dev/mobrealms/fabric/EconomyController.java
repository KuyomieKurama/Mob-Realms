package dev.mobrealms.fabric;

import dev.mobrealms.core.*;
import dev.mobrealms.core.Development.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.Heightmap;
import java.io.IOException;
import java.util.*;

/** World effects remain server-side and never load chunks to satisfy economic work. */
public final class EconomyController {
    private final RealmController controller;
    private final MinecraftServer server;
    private final RealmSimulation state;
    private final Blueprints blueprints=new Blueprints();
    private final ArrayDeque<UUID> daily=new ArrayDeque<>();
    private int cursor;
    private final Map<UUID,Integer> siteSurveys=new HashMap<>();
    private final Map<UUID,String> reportedObstacle=new HashMap<>();
    private final Map<UUID,Long> reportedAt=new HashMap<>();
    private record ResourceTarget(BlockPos source,BlockPos work,String material,String item) {}
    private record Spot(String dimension,BlockPos pos) {}
    private final Map<UUID,ResourceTarget> targets=new HashMap<>();
    private final Map<UUID,WorkProgress> targetProgress=new HashMap<>();
    private final Map<UUID,ResourceSurvey> surveys=new HashMap<>();
    private final Map<UUID,LinkedHashSet<BlockPos>> discoveries=new HashMap<>();
    private final Map<Spot,Long> rejected=new LinkedHashMap<>();
    private final Map<UUID,BlockPos> walkingTo=new HashMap<>();
    private final Map<UUID,BlockPos> walkingEnd=new HashMap<>();
    private final Map<UUID,WorkProgress> walkingProgress=new HashMap<>();
    private final Map<UUID,Long> tended=new HashMap<>();
    private final Map<UUID,Long> yielding=new HashMap<>();
    private final Map<UUID,Long> productiveAt=new HashMap<>();
    private final Map<UUID,String> wanted=new HashMap<>();
    private final Map<UUID,Long> scanned=new HashMap<>();
    private record Footprint(Project project,int minX,int maxX,int minZ,int maxZ) {}
    private final Map<UUID,Footprint> footprints=new HashMap<>();
    private final Map<UUID,String> blockedBy=new HashMap<>();
    private final Map<UUID,Integer> retries=new HashMap<>();
    private final Map<UUID,String> activities=new HashMap<>();
    private final Map<UUID,Long> attacks=new HashMap<>();
    private final Map<UUID,Long> meals=new HashMap<>();
    public EconomyController(RealmController controller,MinecraftServer server)throws IOException{
        this.controller=controller;this.server=server;this.state=controller.state();blueprints.reload(server.getResourceManager());
    }
    public void resumeDays(){for(var camp:state.camps())if(state.development().town(camp.id()).lastDay<state.day())daily.add(camp.id());}
    public BlockPos home(UUID citizen,RealmSimulation.Camp camp){
        var houses=state.development().town(camp.id()).sites.stream().filter(s->s.building()==Building.HOUSE&&s.active()).toList();
        if(houses.isEmpty())return new BlockPos(camp.x(),camp.y(),camp.z());
        var house=houses.get(Math.floorMod(citizen.hashCode(),houses.size()));return new BlockPos(house.x(),house.y()+1,house.z());
    }
    public boolean busy(){return !daily.isEmpty();}
    public void clearActivity(UUID id){activities.remove(id);blockedBy.remove(id);}
    public void activity(UUID id,String value){activities.put(id,value);}
    public String wanted(UUID id){return wanted.getOrDefault(id,"");}
    public String blockedBy(UUID id){return blockedBy.getOrDefault(id,"none");}
    public long scanned(UUID id){return state.hasCitizen(id)?scanned.getOrDefault(state.citizen(id).camp(),0L):0;}
    public void releaseResource(UUID id){targets.remove(id);targetProgress.remove(id);wanted.remove(id);}
    public int retries(UUID id){return retries.getOrDefault(id,0);}
    public long sinceProgress(UUID id){Long tick=productiveAt.get(id);return tick==null?-1:Math.max(0,server.overworld().getGameTime()-tick)/20;}
    public String target(UUID id){var target=targets.get(id);return target==null?"":target.work().toShortString();}
    public void progress(UUID id){productiveAt.put(id,server.overworld().getGameTime());blockedBy.remove(id);}
    public void forget(UUID id){targets.remove(id);targetProgress.remove(id);walkingTo.remove(id);walkingEnd.remove(id);walkingProgress.remove(id);activities.remove(id);wanted.remove(id);blockedBy.remove(id);retries.remove(id);productiveAt.remove(id);tended.remove(id);yielding.remove(id);attacks.remove(id);meals.remove(id);}
    public String activity(UUID id){return activities.getOrDefault(id,"idle");}
    public void dayCompleted(){for(var camp:state.camps())daily.add(camp.id());}
    public void step(){
        if(!daily.isEmpty()){
            UUID id=daily.removeFirst();state.development().daily(state,id,controller.config().learningRate(),controller.config().learningCeiling(),controller.config().growthDays());
            autonomousDiplomacy(id);return;
        }
        var camps=state.camps();if(camps.isEmpty())return;
        var camp=camps.get(Math.floorMod(cursor++,camps.size()));var town=state.development().town(camp.id());
        String previous=reportedObstacle.get(camp.id());long now=server.overworld().getGameTime();
        if(!town.obstacle.equals(previous)&&now-reportedAt.getOrDefault(camp.id(),-200L)>=200){
            state.development().event("obstacle_"+town.obstacle,camp.id(),state.day());reportedObstacle.put(camp.id(),town.obstacle);reportedAt.put(camp.id(),now);
        }
        if(state.population(camp.id())==0){town.obstacle="abandoned";return;}
        ServerLevel level=level(camp);if(level==null||!level.hasChunkAt(new BlockPos(camp.x(),camp.y(),camp.z()))){town.obstacle="unloaded";return;}
        if(town.obstacle.equals("unloaded")||town.obstacle.equals("abandoned"))town.obstacle="survey";
        for(var citizen:state.residents(camp.id()))if(state.development().person(citizen.id()).pendingSpawn){spawn(level,camp,citizen.id());return;}
        checkSite(level,town);
        if(town.project!=null&&(town.project.progress<town.project.paid||town.project.next()==null)){build(null,level,camp,town);return;}
        craft(camp.id());
        if(town.project==null&&state.population(camp.id())<controller.config().settlementTargetPopulation())plan(level,camp,town);
    }
    private ServerLevel level(RealmSimulation.Camp camp){for(var level:server.getAllLevels())if(RealmController.dimension(level).equals(camp.territory().dimension()))return level;return null;}
    private void checkSite(ServerLevel level,Town town){
        if(town.sites.isEmpty())return;int index=Math.floorMod(cursor,town.sites.size());var site=town.sites.get(index);var center=new BlockPos(site.x(),site.y(),site.z());if(!level.hasChunkAt(center))return;
        boolean active;
        if(site.building()==Building.FARM)active=!level.isEmptyBlock(center)&&!level.isEmptyBlock(center.offset(1,0,1));
        else if(site.building()==Building.WALL)active=!level.isEmptyBlock(center.offset(3,1,0));
        else active=level.getBlockState(center).isSolidRender()&&!level.isEmptyBlock(center.above(site.building()==Building.WATCHTOWER?9:6));
        if(active!=site.active()){
            town.sites.set(index,new Site(site.building(),site.x(),site.y(),site.z(),active));town.buildings.merge(site.building(),active?1:-1,Integer::sum);town.obstacle=active?"survey":"repair";
        }
    }
    private void spawn(ServerLevel level,RealmSimulation.Camp camp,UUID id){
        if(level.getEntity(id)!=null){state.development().person(id).pendingSpawn=false;return;}
        var type=BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(controller.profileFor(id).entityType()));
        var entity=type.create(level,EntitySpawnReason.EVENT);if(!(entity instanceof Mob mob))return;
        mob.setUUID(id);mob.setPersistenceRequired();
        boolean clear=false;
        for(int attempt=0;attempt<16;attempt++){
            int dx=level.getRandom().nextInt(9)-4,dz=level.getRandom().nextInt(9)-4;
            var feet=new BlockPos(camp.x()+dx,camp.y(),camp.z()+dz);
            if(!level.hasChunkAt(feet)||!level.getWorldBorder().isWithinBounds(feet))continue;
            for(int dy=-2;dy<=2;dy++){
                var pos=feet.offset(0,dy,0);if(!level.getBlockState(pos.below()).isSolidRender())continue;
                mob.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);
                if(level.noCollision(mob)){clear=true;break;}
            }
            if(clear)break;
        }
        if(!clear){state.development().town(camp.id()).obstacle="spawn";return;}
        if(mob instanceof Piglin piglin)piglin.setImmuneToZombification(true);
        if(level.addFreshEntity(mob)){state.development().person(id).pendingSpawn=false;controller.loadEntity(mob);controller.save();}
    }
    public void endowment(UUID id){
        state.development().town(id).lastDay=state.day();
        state.credit(id,"minecraft:bread",24);state.credit(id,"minecraft:wheat_seeds",24);
        state.development().event("founded",id,state.day());
        int i=0;for(var c:state.residents(id))state.development().person(c.id()).role=new Role[]{Role.BUILDER,Role.GATHERER,Role.MINER}[i++%3];
    }
    private void craft(UUID id){
        var stock=state.stock(id);
        for(String log:stock.keySet())
            if(ResourceMaterials.wood(log)&&stock.getOrDefault("minecraft:oak_planks",0L)<4096&&state.consume(id,Map.of(log,1L))){state.credit(id,"minecraft:oak_planks",4);return;}
        if(stock.getOrDefault("minecraft:bread",0L)<64&&state.consume(id,Map.of("minecraft:wheat",3L))){state.credit(id,"minecraft:bread",1);return;}
        if(state.development().town(id).count(Building.WORKSHOP)>0&&state.consume(id,Map.of("minecraft:raw_iron",1L,"minecraft:coal",1L)))state.credit(id,"minecraft:iron_ingot",1);
    }
    private void plan(ServerLevel level,RealmSimulation.Camp camp,Town town){
        var objective=town.objective(state.population(camp.id()));
        // One compact building per claimed chunk; the founding chunk remains the gathering commons.
        var candidates=new LinkedHashSet<ChunkKey>();
        for(var base:town.claims)for(int side=0;side<4;side++)candidates.add(new ChunkKey(base.dimension(),base.x()+(side==0?1:side==1?-1:0),base.z()+(side==2?1:side==3?-1:0)));
        var plots=new ArrayList<>(candidates);int scan=siteSurveys.getOrDefault(camp.id(),0);siteSurveys.put(camp.id(),scan+1);
        var chunk=plots.get(Math.floorMod(scan/9,plots.size()));int offset=Math.floorMod(scan,9);
        // Leave room for the north entrance run inside this claimed plot.
        int x=chunk.x()*16+6+(offset%3)*2,z=chunk.z()*16+10+(offset/3);
        BlockPos center=new BlockPos(x,camp.y(),z);
        if((state.development().claimed(chunk)&&!town.claims.contains(chunk))||occupied(camp,town,chunk)||state.protectedAt(chunk)||!level.hasChunkAt(center)){town.obstacle="land";return;}
        var snapshot=new BuildingTerrain(level,state,chunk,camp.y());
        var outline=blueprints.at(objective,x,0,z,camp.species());
        int minX=outline.stream().mapToInt(Tile::x).min().orElseThrow(),maxX=outline.stream().mapToInt(Tile::x).max().orElseThrow();
        int minZ=outline.stream().mapToInt(Tile::z).min().orElseThrow(),maxZ=outline.stream().mapToInt(Tile::z).max().orElseThrow();
        var heights=new ArrayList<Integer>();
        for(int xx=minX;xx<=maxX;xx++)for(int zz=minZ;zz<=maxZ;zz++)heights.add(snapshot.surface(xx,zz));
        Collections.sort(heights);int floor=heights.get(heights.size()/2);
        if(Math.abs((long)floor-camp.y())>16){town.obstacle="terrain_relief";return;}
        var result=TerrainPlanner.plan(blueprints.at(objective,x,floor,z,camp.species()),snapshot);
        if(!result.accepted()){town.obstacle=result.obstacle();return;}
        if(town.claims.contains(chunk)||state.development().claim(camp.id(),chunk,state.protectedChunks())){
            town.project=new Project(objective,result.tiles());town.obstacle=town.project.preparationCount>0?"preparing":"materials";
        }
    }
    public void recover(Mob mob,RealmSimulation.Camp camp,BlockPos home){
        long now=mob.level().getGameTime();
        if(mob.getHealth()<mob.getMaxHealth()*.5&&mob.distanceToSqr(home.getX()+.5,home.getY(),home.getZ()+.5)<16
            &&now-meals.getOrDefault(mob.getUUID(),-100L)>=100&&state.consume(camp.id(),Map.of("minecraft:bread",1L))){
            mob.heal(2);meals.put(mob.getUUID(),now);activities.put(mob.getUUID(),"recover");
        }
    }
    public boolean work(Mob mob,ServerLevel level,RealmSimulation.Camp camp){
        activities.put(mob.getUUID(),"idle");
        var town=state.development().town(camp.id());var person=state.development().person(mob.getUUID());
        if(combat(mob,level,camp,person)){releaseResource(mob.getUUID());return true;}
        if(yielding.getOrDefault(mob.getUUID(),0L)>level.getGameTime()){
            activities.put(mob.getUUID(),"step_aside");return true;
        }
        boolean basicNeeds=ProductionNeeds.needsWorkers(state.population(camp.id()),town.count(Building.FARM),town.starvation);
        if(!basicNeeds&&person.role==Role.TRADER&&travelTrade(mob,level,camp))return true;
        if(!basicNeeds&&(person.role==Role.GUARD||person.role==Role.SOLDIER)){equip(mob,camp.id());return false;}
        if(town.project!=null && (person.role==Role.BUILDER||town.strategy==Strategy.EXPANSION
            ||(town.project.next()!=null&&town.project.next().phase()==WorkPhase.CLEAR))) {
            if(build(mob,level,camp,town))return true;
        }
        // Tending requires arrival; between visits the farmer helps construction and gathering.
        long now=level.getGameTime();
        if(person.role==Role.FARMER&&now-tended.getOrDefault(mob.getUUID(),-1200L)>=1200){
            var site=town.sites.stream().filter(f->f.building()==Building.FARM&&f.active()).findFirst();
            if(site.isPresent()){
                var farm=site.get();var edge=new BlockPos(farm.x()+4,farm.y(),farm.z());
                if(level.hasChunkAt(edge)){
                    if(mob.distanceToSqr(edge.getX()+.5,edge.getY(),edge.getZ()+.5)<=9){
                        mob.getNavigation().stop();mob.swingForAttack(net.minecraft.world.InteractionHand.MAIN_HAND);person.reward(1);
                        tended.put(mob.getUUID(),now);progress(mob.getUUID());activities.put(mob.getUUID(),"farm");return true;
                    }
                    if(approach(mob,level,edge)){releaseResource(mob.getUUID());activities.put(mob.getUUID(),"farm");return true;}
                    tended.put(mob.getUUID(),now-1000); // retry after ten seconds, do useful work meanwhile
                }
            }
        }
        equipWorker(mob,camp.id(),person.role);
        var materials=ProductionNeeds.missing(town.project,ResourceMaterials.available(state.stock(camp.id())));
        if(materials.isEmpty()&&town.project!=null){
            if(build(mob,level,camp,town))return true;
            activities.put(mob.getUUID(),town.obstacle.equals("materials")?"processing":"build_blocked");
            blockedBy.put(mob.getUUID(),town.obstacle);return true;
        }
        if(materials.isEmpty()&&person.role==Role.MINER&&town.count(Building.WORKSHOP)>0){
            if(state.stock(camp.id()).getOrDefault("minecraft:coal",0L)<4)materials.add("minecraft:coal");
            if(state.stock(camp.id()).getOrDefault("minecraft:iron_ingot",0L)<16)materials.add("minecraft:iron_ingot");
        }
        if(materials.isEmpty()){mob.getNavigation().stop();wanted.remove(mob.getUUID());activities.put(mob.getUUID(),"stocked");return true;}
        // Different workers prefer different deficits, but can take any useful material encountered.
        Collections.rotate(materials,Math.floorMod(mob.getUUID().hashCode(),materials.size()));
        return gather(mob,level,camp,materials);
    }
    private boolean build(Mob mob,ServerLevel level,RealmSimulation.Camp camp,Town town){
        var project=town.project;var tile=project.next();
        if(mob!=null)releaseResource(mob.getUUID());
        if(tile==null){boolean completed=project.counted;town.complete(project);town.project=null;town.obstacle="survey";if(!completed)state.development().event("building",camp.id(),state.day());controller.save();return true;}
        var pos=new BlockPos(tile.x(),tile.y(),tile.z());
        var chunk=ChunkKey.fromBlock(RealmController.dimension(level),tile.x(),tile.z());
        if(!level.hasChunkAt(pos)){town.obstacle="unloaded";return false;}
        if(state.protectedAt(chunk)||!town.claims.contains(chunk)||!level.getWorldBorder().isWithinBounds(pos)||level.getBlockEntity(pos)!=null){town.obstacle="protected";return false;}
        if(mob==null&&tile.phase()!=WorkPhase.BUILD){town.obstacle="preparing";return false;}
        var block=BuiltInRegistries.BLOCK.getValue(Identifier.parse(tile.block()));
        var current=level.getBlockState(pos);
        if(current.is(block)){project.progress++;project.paid=Math.max(project.paid,project.progress);return true;}
        boolean clearing=tile.phase()==WorkPhase.CLEAR;
        if(clearing){
            var cell=BuildingTerrain.classify(level,pos);
            boolean soilChange="minecraft:dirt".equals(BuildingTerrain.salvage(tile.expected()))&&"minecraft:dirt".equals(BuildingTerrain.salvage(cell.block()));
            if((!cell.block().equals(tile.expected())&&!soilChange)||cell.kind()==TerrainPlanner.Kind.BLOCKED||cell.kind()==TerrainPlanner.Kind.FLUID){town.obstacle="terrain_changed";return false;}
        }else if(!current.isAir()&&!current.canBeReplaced()){town.obstacle="blocked";return false;}
        if(!clearing&&project.progress>=project.paid&&!tile.material().equals("minecraft:air")&&state.stock(camp.id()).getOrDefault(tile.material(),0L)<1){town.obstacle="materials";return false;}
        String activity=switch(tile.phase()){case CLEAR->"clear_site";case FOUNDATION->"foundation";case ACCESS->"access";case BUILD->"build";};
        // Builders work from the footprint edge, including the roof and foundation scaffold radius.
        if(mob!=null&&Math.hypot(mob.getX()-tile.x(),mob.getZ()-tile.z())>5){
            if(approach(mob,level,new BlockPos(tile.x(),project.originY,tile.z()))){activities.put(mob.getUUID(),activity);return true;}
            town.obstacle="unreachable";blockedBy.put(mob.getUUID(),"unreachable");return false;
        }
        if(clearing){
            // No automatic yield during catch-up. Every recovered block belongs to this live extraction.
            if(mob==null)return false;
            if(!freeBuildingCell(level,pos.above(),camp,project,town))return false;
            if(level.setBlock(pos,Blocks.AIR.defaultBlockState(),3)){
                level.levelEvent(2001,pos,Block.getId(current));String item=BuildingTerrain.salvage(tile.expected());
                if(item!=null)state.collect(controller.lease(mob.getUUID()),item,1,controller.profileFor(mob.getUUID()).carryingCapacity());
                project.progress++;project.paid=Math.max(project.paid,project.progress);town.labor=Math.min(1000000,town.labor+1);
                town.obstacle="clearing";progress(mob.getUUID());mob.swingForAttack(net.minecraft.world.InteractionHand.MAIN_HAND);
                state.development().person(mob.getUUID()).reward(1);activities.put(mob.getUUID(),activity);return true;
            }
            return false;
        }
        // Do not seal a resident or player inside a newly placed solid block.
        if(!block.defaultBlockState().getCollisionShape(level,pos).isEmpty()&&!freeBuildingCell(level,pos,camp,project,town))return false;
        boolean alreadyPaid=project.progress<project.paid;
        if(!alreadyPaid&&!tile.material().equals("minecraft:air")&&!state.consume(camp.id(),Map.of(tile.material(),1L)))return false;
        if(!level.setBlock(pos,block.defaultBlockState(),3)){
            if(!alreadyPaid&&!tile.material().equals("minecraft:air"))state.credit(camp.id(),tile.material(),1);return false;
        }
        project.progress++;project.paid=Math.max(project.paid,project.progress);town.labor=Math.min(1000000,town.labor+1);
        town.obstacle=tile.phase()==WorkPhase.BUILD?"building":activity;
        if(mob!=null){progress(mob.getUUID());mob.swingForAttack(net.minecraft.world.InteractionHand.MAIN_HAND);state.development().person(mob.getUUID()).reward(2);activities.put(mob.getUUID(),activity);}return true;
    }
    private boolean freeBuildingCell(ServerLevel level,BlockPos pos,RealmSimulation.Camp camp,Project project,Town town){
        var occupants=level.getEntities((Entity)null,new net.minecraft.world.phys.AABB(pos),e->e.isAlive()&&!e.isSpectator());
        if(occupants.isEmpty())return true;
        for(var entity:occupants)if(entity instanceof Mob resident&&state.hasCitizen(resident.getUUID())
            &&state.citizen(resident.getUUID()).camp().equals(camp.id())&&controller.lease(resident.getUUID())!=null){
            if(approach(resident,level,new BlockPos(project.originX+7,project.originY,project.originZ)))yielding.put(resident.getUUID(),level.getGameTime()+100);
        }
        town.obstacle="occupied";return false;
    }
    private boolean occupied(RealmSimulation.Camp camp,Town town,ChunkKey chunk){
        if(chunk.equals(camp.territory()))return true;
        if(town.sites.stream().anyMatch(site->ChunkKey.fromBlock(chunk.dimension(),site.x(),site.z()).equals(chunk)))return true;
        return town.project!=null&&town.project.tiles.stream().anyMatch(tile->ChunkKey.fromBlock(chunk.dimension(),tile.x(),tile.z()).equals(chunk));
    }
    private boolean harvestAllowed(ServerLevel level,RealmSimulation.Camp camp,BlockPos pos){
        var town=state.development().town(camp.id());
        var chunk=ChunkKey.fromBlock(camp.territory().dimension(),pos.getX(),pos.getZ());
        if(Math.abs((long)chunk.x()-camp.territory().x())>3||Math.abs((long)chunk.z()-camp.territory().z())>3
            ||!level.hasChunkAt(pos)||!level.getWorldBorder().isWithinBounds(pos)||state.protectedAt(chunk)
            ||(state.development().claimed(chunk)&&!town.claims.contains(chunk))||level.getBlockEntity(pos)!=null)return false;
        if(Math.hypot(pos.getX()-camp.x(),pos.getZ()-camp.z())<4)return false;
        for(var site:town.sites)if(Math.abs(pos.getX()-site.x())<=6&&pos.getZ()>=site.z()-12&&pos.getZ()<=site.z()+6)return false;
        if(town.project!=null){
            var footprint=footprints.get(camp.id());
            if(footprint==null||footprint.project()!=town.project){
                var tiles=town.project.tiles;
                footprint=new Footprint(town.project,tiles.stream().mapToInt(Tile::x).min().orElseThrow()-1,tiles.stream().mapToInt(Tile::x).max().orElseThrow()+1,
                    tiles.stream().mapToInt(Tile::z).min().orElseThrow()-1,tiles.stream().mapToInt(Tile::z).max().orElseThrow()+1);footprints.put(camp.id(),footprint);
            }
            if(pos.getX()>=footprint.minX()&&pos.getX()<=footprint.maxX()&&pos.getZ()>=footprint.minZ()&&pos.getZ()<=footprint.maxZ())return false;
        }else footprints.remove(camp.id());
        return true;
    }
    private ResourceTarget extraction(Mob mob,ServerLevel level,RealmSimulation.Camp camp,BlockPos source,List<String> materials){
        if(!level.hasChunkAt(source)||Math.abs(source.getY()-mob.getY())>8)return null;
        if(rejected.getOrDefault(new Spot(camp.territory().dimension(),source),0L)>level.getGameTime())return null;
        for(String material:materials){
            String item=drop(level.getBlockState(source).getBlock(),material);if(item==null)continue;
            if(!harvestAllowed(level,camp,source))return null;
            BlockPos work=source;
            // Expose shallow stone/ore through natural soil one actual block at a time.
            if(material.equals("minecraft:cobblestone")||material.equals("minecraft:iron_ingot")||material.equals("minecraft:coal")){
                for(int up=1;up<=4;up++){
                    var above=source.above(up);var block=level.getBlockState(above);
                    if(drop(block.getBlock(),"minecraft:dirt")==null)break;
                    work=above;item="minecraft:dirt";
                }
            }
            if(!harvestAllowed(level,camp,work))return null;
            boolean exposed=false;for(var direction:net.minecraft.core.Direction.values()){
                var side=work.relative(direction);if(level.hasChunkAt(side)&&level.getBlockState(side).getCollisionShape(level,side).isEmpty()&&level.getFluidState(side).isEmpty()){exposed=true;break;}
            }
            if(!exposed)return null;
            final BlockPos reserved=work;
            if(targets.entrySet().stream().anyMatch(e->!e.getKey().equals(mob.getUUID())&&state.hasCitizen(e.getKey())
                &&state.camp(state.citizen(e.getKey()).camp()).territory().dimension().equals(camp.territory().dimension())&&e.getValue().work().equals(reserved)))return null;
            return new ResourceTarget(source,work,material,item);
        }
        return null;
    }
    private void reject(Mob mob,RealmSimulation.Camp camp,ResourceTarget target){
        rejected.put(new Spot(camp.territory().dimension(),target.source()),mob.level().getGameTime()+600);
        while(rejected.size()>2048)rejected.remove(rejected.keySet().iterator().next());
        targets.remove(mob.getUUID());targetProgress.remove(mob.getUUID());mob.getNavigation().stop();walkingTo.remove(mob.getUUID());
        retries.merge(mob.getUUID(),1,(a,b)->Math.min(1000000,a+b));blockedBy.put(mob.getUUID(),"unreachable");state.development().town(camp.id()).obstacle="unreachable";
        activities.put(mob.getUUID(),"reroute");
    }
    private boolean gather(Mob mob,ServerLevel level,RealmSimulation.Camp camp,List<String> materials){
        UUID id=mob.getUUID();var town=state.development().town(camp.id());long now=level.getGameTime();
        var target=targets.get(id);
        if(target!=null){
            var validated=extraction(mob,level,camp,target.source(),materials);
            if(validated==null){targets.remove(id);targetProgress.remove(id);target=null;}else target=validated;
        }
        if(target!=null&&targetProgress.get(id).expired(now,Math.sqrt(mob.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(target.work()))))){
            reject(mob,camp,target);return true;
        }
        var known=discoveries.computeIfAbsent(camp.id(),key->new LinkedHashSet<>());
        if(target==null){
            for(var pos:known){target=extraction(mob,level,camp,pos,materials);if(target!=null)break;}
            var survey=surveys.computeIfAbsent(camp.id(),key->new ResourceSurvey(camp.territory(),3,0));
            int loadedColumns=0,protectedColumns=0;
            search:for(int attempt=0;target==null&&attempt<32;attempt++){
                var column=survey.next();scanned.merge(camp.id(),1L,Long::sum);var surface=new BlockPos(column.x(),camp.y(),column.z());
                if(!level.hasChunkAt(surface))continue;loadedColumns++;
                var chunk=ChunkKey.fromBlock(camp.territory().dimension(),column.x(),column.z());
                if(state.protectedAt(chunk)){protectedColumns++;continue;}
                if(!level.dimension().equals(net.minecraft.world.level.Level.NETHER))surface=level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,surface);
                for(int offset:new int[]{-1,0,1,2,3,4,-2,-3,-4,-5,-6}){
                    var pos=surface.offset(0,offset,0);target=extraction(mob,level,camp,pos,materials);
                    if(target!=null){known.add(pos);while(known.size()>64)known.remove(known.iterator().next());break search;}
                }
            }
            if(target==null){
                wanted.put(id,materials.getFirst());town.obstacle="resources";
                String reason=loadedColumns==0?"unloaded":protectedColumns==loadedColumns?"protected":"resources";
                blockedBy.put(id,reason);activities.put(id,scanned.getOrDefault(camp.id(),0L)>=survey.columnsPerPass()?"supply_blocked":"survey");mob.getNavigation().stop();return true;
            }
            targets.put(id,target);targetProgress.put(id,new WorkProgress(now,Math.sqrt(mob.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(target.work())))));
        }else targets.put(id,target);
        wanted.put(id,target.material());blockedBy.remove(id);activities.put(id,target.source().equals(target.work())?"harvest":"excavate");
        BlockPos pos=target.work();
        if(mob.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pos))>20||Math.hypot(mob.getX()-pos.getX()-.5,mob.getZ()-pos.getZ()-.5)<1.1){
            if(!approach(mob,level,pos))reject(mob,camp,target);return true;
        }
        // Never extract through an intervening wall; only a visible block is removed.
        var hit=level.clip(new net.minecraft.world.level.ClipContext(mob.getEyePosition(),net.minecraft.world.phys.Vec3.atCenterOf(pos),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,mob));
        if(hit.getType()!=net.minecraft.world.phys.HitResult.Type.MISS&&!hit.getBlockPos().equals(pos)){
            if(!approach(mob,level,pos))reject(mob,camp,target);return true;
        }
        var harvested=level.getBlockState(pos);
        if(level.setBlock(pos,Blocks.AIR.defaultBlockState(),3)){
            level.levelEvent(2001,pos,Block.getId(harvested));
            state.collect(controller.lease(id),target.item(),1,controller.profileFor(id).carryingCapacity());
            state.development().person(id).reward(2);town.labor=Math.min(1000000,town.labor+1);progress(id);
            mob.swingForAttack(net.minecraft.world.InteractionHand.MAIN_HAND);
            if(target.source().equals(pos))known.remove(target.source());
        }
        targets.remove(id);targetProgress.remove(id);mob.getNavigation().stop();return true;
    }
    /** Replace obsolete paths and reject partial paths; a progress watchdog handles stuck mobs. */
    public boolean approach(Mob mob,ServerLevel level,BlockPos target){
        UUID id=mob.getUUID();long now=level.getGameTime();
        if(!target.equals(walkingTo.get(id))){
            mob.getNavigation().stop();walkingTo.put(id,target);walkingProgress.put(id,new WorkProgress(now,Math.sqrt(mob.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(target)))));
        }
        if(walkingProgress.get(id).expired(now,Math.sqrt(mob.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(target))))){
            mob.getNavigation().stop();walkingTo.remove(id);walkingEnd.remove(id);walkingProgress.remove(id);return false;
        }
        if(!mob.getNavigation().isDone()&&Objects.equals(walkingEnd.get(id),mob.getNavigation().getTargetPos()))return true;
        mob.getNavigation().stop();
        for(int i=0;i<8;i++){
            int dx=new int[]{-2,2,0,0,-2,-2,2,2}[i],dz=new int[]{0,0,-2,2,-2,2,-2,2}[i];
            var column=new BlockPos(target.getX()+dx,mob.blockPosition().getY(),target.getZ()+dz);
            if(!level.hasChunkAt(column))continue;
            BlockPos surface=level.dimension().equals(net.minecraft.world.level.Level.NETHER)?column:level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,column);
            for(int dy:new int[]{0,1,-1,2,-2,3,-3,4,-4}){
                var feet=new BlockPos(column.getX(),Math.abs(surface.getY()-mob.getY())<=4?surface.getY()+dy:column.getY()+dy,column.getZ());
                if(!level.getWorldBorder().isWithinBounds(feet)||!level.getBlockState(feet.below()).isFaceSturdy(level,feet.below(),net.minecraft.core.Direction.UP)
                    ||!level.getFluidState(feet).isEmpty()||!level.noCollision(mob,mob.getBoundingBox().move(feet.getX()+.5-mob.getX(),feet.getY()-mob.getY(),feet.getZ()+.5-mob.getZ())))continue;
                var path=mob.getNavigation().createPath(feet,0);
                if(path!=null&&path.canReach()&&mob.getNavigation().moveTo(path,1)){walkingEnd.put(id,feet);return true;}
                break;
            }
        }
        return false;
    }
    private String drop(Block block,String wanted){return ResourceMaterials.drop(BuiltInRegistries.BLOCK.getKey(block).toString(),wanted);}
    private void equipWorker(Mob mob,UUID camp,Role role){
        if(!mob.getMainHandItem().isEmpty())return;
        Item tool=switch(role){case BUILDER,GATHERER->Items.STONE_AXE;case MINER->Items.STONE_PICKAXE;case FARMER->Items.STONE_HOE;default->null;};
        if(tool!=null&&state.consume(camp,Map.of("minecraft:cobblestone",3L,"minecraft:oak_planks",2L)))mob.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(tool));
    }
    private void equip(Mob mob,UUID camp){
        var town=state.development().town(camp);if(town.count(Building.WORKSHOP)==0)return;
        if(controller.profileFor(mob.getUUID()).entityType().contains("skeleton")&&mob.getMainHandItem().isEmpty()&&state.consume(camp,Map.of("minecraft:string",3L,"minecraft:oak_planks",2L)))mob.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.BOW));
        if(mob.getMainHandItem().isEmpty()&&state.consume(camp,Map.of("minecraft:cobblestone",2L,"minecraft:oak_planks",1L)))mob.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.STONE_SWORD));
        if(mob.getItemBySlot(EquipmentSlot.HEAD).isEmpty()&&state.consume(camp,Map.of("minecraft:iron_ingot",5L)))mob.setItemSlot(EquipmentSlot.HEAD,new ItemStack(Items.IRON_HELMET));
        if(town.technologies.contains(Technology.SHIELDS)&&town.rangedThreat>.35&&mob.getOffhandItem().isEmpty()&&state.consume(camp,Map.of("minecraft:iron_ingot",1L,"minecraft:oak_planks",6L)))mob.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(Items.SHIELD));
    }
    private boolean combat(Mob mob,ServerLevel level,RealmSimulation.Camp camp,Person person){
        if(person.role!=Role.GUARD&&person.role!=Role.SOLDIER)return false;
        LivingEntity enemy=null;UUID enemyCamp=null;
        for(Mob other:controller.loadedMobs()){
            if(other==mob||other.level()!=level||!other.isAlive()||mob.distanceToSqr(other)>256)continue;
            var c=state.citizen(other.getUUID());
            if(!c.camp().equals(camp.id())&&state.development().relation(camp.id(),c.camp()).treaty==Treaty.WAR){enemy=other;enemyCamp=c.camp();break;}
        }
        if(enemy==null)for(var player:level.players()){
            if(player.isCreative()||player.isSpectator()||mob.distanceToSqr(player)>256)continue;
            var nation=state.development().nation(player.getUUID());
            if(nation.isPresent()&&!nation.get().equals(camp.id())&&state.development().relation(camp.id(),nation.get()).treaty==Treaty.WAR){enemy=player;enemyCamp=nation.get();break;}
        }
        if(enemy==null){mob.stopUsingItem();return false;}
        equip(mob,camp.id());activities.put(mob.getUUID(),"defend");
        if(mob.getOffhandItem().is(Items.SHIELD))mob.startUsingItem(net.minecraft.world.InteractionHand.OFF_HAND);
        var town=state.development().town(camp.id());
        if(town.technologies.contains(Technology.FLANKING)&&mob.distanceToSqr(enemy)>16){
            double angle=Math.toRadians(enemy.getYRot()+((mob.getUUID().hashCode()&1)==0?90:-90));
            mob.getNavigation().moveTo(enemy.getX()+Math.cos(angle)*3,enemy.getY(),enemy.getZ()+Math.sin(angle)*3,1.1+person.rank()*.05);
        }else mob.getNavigation().moveTo(enemy,1.1+person.rank()*.05);
        if(mob instanceof net.minecraft.world.entity.monster.RangedAttackMob ranged&&mob.getMainHandItem().is(Items.BOW)
                &&level.getGameTime()-attacks.getOrDefault(mob.getUUID(),-100L)>=40&&state.consume(camp.id(),Map.of("minecraft:arrow",1L))){
            ranged.performRangedAttack(enemy,1);attacks.put(mob.getUUID(),level.getGameTime());person.reward(5);return true;
        }
        if(town.technologies.contains(Technology.SIEGE)&&enemyCamp!=null&&controller.profileFor(mob.getUUID()).entityType().endsWith("creeper")&&level.getGameTime()%40==0){
            var direction=enemy.position().subtract(mob.position()).normalize();var breach=BlockPos.containing(mob.position().add(direction.scale(1.5)));
            var chunk=ChunkKey.fromBlock(RealmController.dimension(level),breach.getX(),breach.getZ());
            if(state.development().town(enemyCamp).claims.contains(chunk)&&!state.protectedAt(chunk)&&level.hasChunkAt(breach)&&level.getBlockState(breach).isSolidRender())level.setBlock(breach,Blocks.AIR.defaultBlockState(),3);
        }
        var attack=mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);if(attack!=null)attack.setBaseValue(3+person.rank());
        long now=level.getGameTime();if(mob.distanceToSqr(enemy)<6&&now-attacks.getOrDefault(mob.getUUID(),-100L)>=20){mob.doHurtTarget(level,enemy);attacks.put(mob.getUUID(),now);person.reward(5);}
        return true;
    }
    public void death(Mob mob){
        if(!state.hasCitizen(mob.getUUID()))return;UUID camp=state.citizen(mob.getUUID()).camp();var town=state.development().town(camp);
        var damage=mob.getLastDamageSource();if(damage!=null){
            if(damage.getEntity() instanceof net.minecraft.server.level.ServerPlayer player){town.reputation(player.getUUID(),-25);state.development().nation(player.getUUID()).filter(n->!n.equals(camp)).ifPresent(n->state.development().relation(camp,n).adjust(-25));}if(damage.getDirectEntity() instanceof AbstractArrow)town.rangedHits++;else town.meleeHits++;}
        forget(mob.getUUID());
    }
    private boolean travelTrade(Mob mob,ServerLevel level,RealmSimulation.Camp camp){
        for(var other:state.camps()){
            if(other.id().equals(camp.id())||!other.territory().dimension().equals(camp.territory().dimension())||Math.hypot(other.x()-camp.x(),other.z()-camp.z())>128)continue;
            var r=state.development().relation(camp.id(),other.id());if(r.lastTradeDay>=state.day()||!(r.treaty==Treaty.TRADE||r.treaty==Treaty.ALLIANCE))continue;
            if(!level.hasChunkAt(new BlockPos(other.x(),other.y(),other.z())))continue;
            activities.put(mob.getUUID(),"trade");
            if(mob.distanceToSqr(other.x()+.5,other.y(),other.z()+.5)>9){mob.getNavigation().moveTo(other.x()+.5,other.y(),other.z()+.5,1);return true;}
            boolean traded=state.development().trade(state,camp.id(),other.id(),"minecraft:bread","minecraft:oak_planks",2)
                ||state.development().trade(state,camp.id(),other.id(),"minecraft:oak_planks","minecraft:cobblestone",2);
            if(traded)state.development().person(mob.getUUID()).reward(10);return traded;
        }
        return false;
    }
    private void autonomousDiplomacy(UUID id){
        var town=state.development().town(id);if(town.owner!=null)return;
        for(var other:state.camps()){
            if(other.id().equals(id)||!other.territory().dimension().equals(state.camp(id).territory().dimension()))continue;
            var relation=state.development().relation(id,other.id());
            if(state.development().town(other.id()).owner!=null){if(relation.offer==null&&relation.score>=10)state.development().propose(id,other.id(),Treaty.TRADE);continue;}
            if(!state.camp(id).species().endsWith("enderman")&&town.starvation>=2&&Math.hypot(other.x()-state.camp(id).x(),other.z()-state.camp(id).z())<160
                &&Math.floorMod(Objects.hash(id,other.id(),state.day()),100)<controller.config().aggression()*30){
                relation.adjust(-10);if(relation.score<=-50)state.development().propose(id,other.id(),Treaty.WAR);else if(relation.score<=-20)relation.treaty=Treaty.HOSTILE;
            }
            if(relation.treaty==Treaty.NEUTRAL&&town.count(Building.MARKET)>0){relation.adjust(2);state.development().propose(id,other.id(),Treaty.TRADE);}
            if(relation.lastTradeDay<state.day()&&state.residents(id).stream().allMatch(c->c.mode()==RealmSimulation.Mode.ABSTRACT)){
                if(!state.development().trade(state,id,other.id(),"minecraft:bread","minecraft:oak_planks",2))state.development().trade(state,id,other.id(),"minecraft:oak_planks","minecraft:cobblestone",2);
            }
            return;
        }
    }
}
