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
    private final Map<UUID,BlockPos> targets=new HashMap<>();
    private final Map<UUID,String> activities=new HashMap<>();
    private final Map<UUID,Long> attacks=new HashMap<>();
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
    public void clearActivity(UUID id){activities.remove(id);}
    public String activity(UUID id){return activities.getOrDefault(id,"idle");}
    public void dayCompleted(){for(var camp:state.camps())daily.add(camp.id());}
    public void step(){
        if(!daily.isEmpty()){
            UUID id=daily.removeFirst();state.development().daily(state,id,controller.config().learningRate(),controller.config().learningCeiling(),controller.config().growthDays());
            autonomousDiplomacy(id);return;
        }
        var camps=state.camps();if(camps.isEmpty())return;
        var camp=camps.get(Math.floorMod(cursor++,camps.size()));var town=state.development().town(camp.id());
        ServerLevel level=level(camp);if(level==null||!level.hasChunkAt(new BlockPos(camp.x(),camp.y(),camp.z())))return;
        for(var citizen:state.residents(camp.id()))if(state.development().person(citizen.id()).pendingSpawn){spawn(level,camp,citizen.id());return;}
        checkSite(level,town);
        if(town.project!=null&&(town.project.progress<town.project.paid||town.project.next()==null)){build(null,level,camp,town);return;}
        craft(camp.id());
        if(town.project==null&&state.population(camp.id())<48)plan(level,camp,town);
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
        mob.setUUID(id);mob.setPos(camp.x()+.5,camp.y(),camp.z()+.5);mob.setPersistenceRequired();
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
        for(String log:List.of("oak_log","birch_log","spruce_log","jungle_log","acacia_log","dark_oak_log","mangrove_log","crimson_stem","warped_stem"))
            if(stock.getOrDefault("minecraft:oak_planks",0L)<256&&state.consume(id,Map.of("minecraft:"+log,1L))){state.credit(id,"minecraft:oak_planks",4);return;}
        if(stock.getOrDefault("minecraft:bread",0L)<64&&state.consume(id,Map.of("minecraft:wheat",3L))){state.credit(id,"minecraft:bread",1);return;}
        if(state.development().town(id).count(Building.WORKSHOP)>0&&state.consume(id,Map.of("minecraft:raw_iron",1L,"minecraft:coal",1L)))state.credit(id,"minecraft:iron_ingot",1);
    }
    private void plan(ServerLevel level,RealmSimulation.Camp camp,Town town){
        var objective=town.objective(state.population(camp.id()));
        // One compact building per claimed chunk; the founding chunk remains the gathering commons.
        var random=level.getRandom();var edge=new ArrayList<>(town.claims);var base=edge.get(random.nextInt(edge.size()));
        int side=random.nextInt(4);var chunk=new ChunkKey(base.dimension(),base.x()+(side==0?1:side==1?-1:0),base.z()+(side==2?1:side==3?-1:0));
        BlockPos center=new BlockPos(chunk.x()*16+8,camp.y(),chunk.z()*16+8);
        if((state.development().claimed(chunk)&&!town.claims.contains(chunk))||occupied(camp,town,chunk)||state.protectedAt(chunk)||!level.hasChunkAt(center)){town.obstacle="land";return;}
        if(!level.dimension().equals(net.minecraft.world.level.Level.NETHER))center=level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,center);
        if(Math.abs(center.getY()-camp.y())>8){town.obstacle="terrain";return;}
        var tiles=blueprints.at(objective,center.getX(),center.getY(),center.getZ(),camp.species());
        for(var t:tiles){BlockPos p=new BlockPos(t.x(),t.y(),t.z());if(!level.hasChunkAt(p)||!level.getWorldBorder().isWithinBounds(p)||!level.isEmptyBlock(p)){town.obstacle="terrain";return;}}
        for(int dx=-4;dx<=4;dx++)for(int dz=-4;dz<=4;dz++)if(!level.getBlockState(center.offset(dx,-1,dz)).isSolidRender()){town.obstacle="terrain";return;}
        if(town.claims.contains(chunk)||state.development().claim(camp.id(),chunk,state.protectedChunks())){town.project=new Project(objective,tiles);town.obstacle="materials";}
    }
    public boolean work(Mob mob,ServerLevel level,RealmSimulation.Camp camp){
        activities.put(mob.getUUID(),"idle");
        var town=state.development().town(camp.id());var person=state.development().person(mob.getUUID());
        if(combat(mob,level,camp,person))return true;
        if(person.role==Role.TRADER&&travelTrade(mob,level,camp))return true;
        if(person.role==Role.GUARD||person.role==Role.SOLDIER){equip(mob,camp.id());return false;}
        if(town.project!=null && (person.role==Role.BUILDER||town.strategy==Strategy.EXPANSION)) {
            if(build(mob,level,camp,town))return true;
        }
        if(person.role==Role.FARMER && town.count(Building.FARM)>0){
            var site=town.sites.stream().filter(f->f.building()==Building.FARM&&f.active()).findFirst();
            if(site.isPresent()){
                var farm=site.get();if(level.hasChunkAt(new BlockPos(farm.x(),farm.y(),farm.z()))){
                    int side=((mob.getUUID().hashCode()+(int)(level.getGameTime()/100))&1)==0?-2:2;
                    mob.getNavigation().moveTo(farm.x()+side+.5,farm.y()+1,farm.z()+.5,1);activities.put(mob.getUUID(),"farm");
                    equipWorker(mob,camp.id(),Role.FARMER);if(level.getGameTime()%100<20){mob.swingForAttack(net.minecraft.world.InteractionHand.MAIN_HAND);person.reward(1);}return true;
                }
            }
        }
        equipWorker(mob,camp.id(),person.role);
        String material=town.project==null?"minecraft:oak_planks":town.project.next()==null?"minecraft:oak_planks":town.project.next().material();
        if(person.role==Role.MINER&&town.count(Building.WORKSHOP)>0){if(state.stock(camp.id()).getOrDefault("minecraft:iron_ingot",0L)<16)material="minecraft:iron_ingot";if(state.stock(camp.id()).getOrDefault("minecraft:coal",0L)<4)material="minecraft:coal";}
        if(material.equals("minecraft:air"))material="minecraft:oak_planks";
        if(state.stock(camp.id()).getOrDefault(material,0L)>256)return false;
        return gather(mob,level,camp,material);
    }
    private boolean build(Mob mob,ServerLevel level,RealmSimulation.Camp camp,Town town){
        var project=town.project;var tile=project.next();
        if(tile==null){town.complete(project);town.project=null;town.obstacle="survey";state.development().event("building",camp.id(),state.day());controller.save();return true;}
        var pos=new BlockPos(tile.x(),tile.y(),tile.z());
        if(!level.hasChunkAt(pos)||state.protectedAt(ChunkKey.fromBlock(RealmController.dimension(level),tile.x(),tile.z()))){town.obstacle="protected";return false;}
        var block=BuiltInRegistries.BLOCK.getValue(Identifier.parse(tile.block()));
        if(level.getBlockState(pos).is(block)){project.progress++;project.paid=Math.max(project.paid,project.progress);return true;}
        if(!level.isEmptyBlock(pos)){town.obstacle="blocked";return false;}
        if(project.progress>=project.paid&&!tile.material().equals("minecraft:air")&&state.stock(camp.id()).getOrDefault(tile.material(),0L)<1){town.obstacle="materials";return false;}
        // Approach the footprint edge; builders can reach the roof from their scaffold radius.
        if(mob!=null&&Math.hypot(mob.getX()-tile.x(),mob.getZ()-tile.z())>5){mob.getNavigation().moveTo(tile.x()+.5,project.tiles.get(0).y()+1,tile.z()+.5,1);activities.put(mob.getUUID(),"build");return true;}
        boolean alreadyPaid=project.progress<project.paid;
        if(!alreadyPaid&&!tile.material().equals("minecraft:air")&&!state.consume(camp.id(),Map.of(tile.material(),1L)))return false;
        if(!level.setBlock(pos,block.defaultBlockState(),3)){
            if(!alreadyPaid&&!tile.material().equals("minecraft:air"))state.credit(camp.id(),tile.material(),1);return false;
        }
        project.progress++;project.paid=Math.max(project.paid,project.progress);town.labor=Math.min(1000000,town.labor+1);town.obstacle="building";
        if(mob!=null){mob.swingForAttack(net.minecraft.world.InteractionHand.MAIN_HAND);state.development().person(mob.getUUID()).reward(2);activities.put(mob.getUUID(),"build");}return true;
    }
    private boolean occupied(RealmSimulation.Camp camp,Town town,ChunkKey chunk){
        if(chunk.equals(camp.territory()))return true;
        if(town.sites.stream().anyMatch(site->ChunkKey.fromBlock(chunk.dimension(),site.x(),site.z()).equals(chunk)))return true;
        return town.project!=null&&town.project.tiles.stream().anyMatch(tile->ChunkKey.fromBlock(chunk.dimension(),tile.x(),tile.z()).equals(chunk));
    }
    private boolean gather(Mob mob,ServerLevel level,RealmSimulation.Camp camp,String material){
        var town=state.development().town(camp.id());
        BlockPos target=targets.get(mob.getUUID());
        if(target!=null){var chunk=ChunkKey.fromBlock(camp.territory().dimension(),target.getX(),target.getZ());
            if(!level.hasChunkAt(target)||!town.claims.contains(chunk)||state.protectedAt(chunk)||(!chunk.equals(camp.territory())&&occupied(camp,town,chunk))||drop(level.getBlockState(target).getBlock(),material)==null)target=null;
        }
        if(target==null){
            var random=level.getRandom();
            // Expand resource commons into loaded neighboring chunks; never quarry building sites.
            for(int attempt=0;attempt<48;attempt++){
                var claims=new ArrayList<>(town.claims);var base=claims.get(random.nextInt(claims.size()));
                int side=random.nextInt(5);var chunk=new ChunkKey(base.dimension(),base.x()+(side==0?1:side==1?-1:0),base.z()+(side==2?1:side==3?-1:0));
                if(state.protectedAt(chunk)||(!chunk.equals(camp.territory())&&occupied(camp,town,chunk))||(state.development().claimed(chunk)&&!town.claims.contains(chunk)))continue;
                int x=chunk.x()*16+random.nextInt(16),z=chunk.z()*16+random.nextInt(16);
                var p=new BlockPos(x,camp.y()+random.nextInt(9)-5,z);
                if(Math.hypot(x-camp.x(),z-camp.z())<4||!level.hasChunkAt(p)||!level.getWorldBorder().isWithinBounds(p))continue;
                if(drop(level.getBlockState(p).getBlock(),material)!=null&&(town.claims.contains(chunk)||state.development().claim(camp.id(),chunk,state.protectedChunks()))){target=p;break;}
            }
            if(target==null){state.development().town(camp.id()).obstacle="resources";return false;}
            targets.put(mob.getUUID(),target);
        }
        if(state.protectedAt(ChunkKey.fromBlock(camp.territory().dimension(),target.getX(),target.getZ()))){targets.remove(mob.getUUID());return false;}
        activities.put(mob.getUUID(),"harvest");
        if(Math.hypot(mob.getX()-target.getX(),mob.getZ()-target.getZ())>3){mob.getNavigation().moveTo(target.getX()+.5,camp.y(),target.getZ()+.5,1);return true;}
        String item=drop(level.getBlockState(target).getBlock(),material);if(item==null)return false;
        if(level.setBlock(target,Blocks.AIR.defaultBlockState(),3)){
            // Account directly after successful extraction, not before removing the block.
            state.collect(controller.lease(mob.getUUID()),item,1,controller.profileFor(mob.getUUID()).carryingCapacity());state.development().person(mob.getUUID()).reward(2);town.labor=Math.min(1000000,town.labor+1);
            mob.swingForAttack(net.minecraft.world.InteractionHand.MAIN_HAND);
        }
        targets.remove(mob.getUUID());return true;
    }
    private String drop(Block block,String wanted){
        String id=BuiltInRegistries.BLOCK.getKey(block).toString();
        if(wanted.equals("minecraft:oak_planks")&&id.matches("minecraft:((oak|birch|spruce|jungle|acacia|dark_oak|mangrove)_log|(crimson|warped)_stem)"))return id;
        if(wanted.equals("minecraft:cobblestone")&&(block==Blocks.STONE||block==Blocks.COBBLESTONE||block==Blocks.BLACKSTONE||block==Blocks.NETHERRACK||block==Blocks.END_STONE))return "minecraft:cobblestone";
        if(wanted.equals("minecraft:dirt")&&(block==Blocks.DIRT||block==Blocks.GRASS_BLOCK||block==Blocks.CRIMSON_NYLIUM||block==Blocks.WARPED_NYLIUM))return "minecraft:dirt";
        if(wanted.equals("minecraft:iron_ingot")&&(block==Blocks.IRON_ORE||block==Blocks.DEEPSLATE_IRON_ORE))return "minecraft:raw_iron";
        if(wanted.equals("minecraft:coal")&&(block==Blocks.COAL_ORE||block==Blocks.DEEPSLATE_COAL_ORE))return "minecraft:coal";
        return null;
    }
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
        if(!state.hasCitizen(mob.getUUID()))return;UUID camp=state.citizen(mob.getUUID()).camp();var town=state.development().town(camp);town.losses++;
        var damage=mob.getLastDamageSource();if(damage!=null){
            if(damage.getEntity() instanceof net.minecraft.server.level.ServerPlayer player){town.reputation(player.getUUID(),-25);state.development().nation(player.getUUID()).filter(n->!n.equals(camp)).ifPresent(n->state.development().relation(camp,n).adjust(-25));}if(damage.getDirectEntity() instanceof AbstractArrow)town.rangedHits++;else town.meleeHits++;}
        targets.remove(mob.getUUID());activities.remove(mob.getUUID());attacks.remove(mob.getUUID());
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
            if(relation.lastTradeDay<state.day()&&state.residents(id).stream().noneMatch(c->c.mode()==RealmSimulation.Mode.DETAILED)){
                if(!state.development().trade(state,id,other.id(),"minecraft:bread","minecraft:oak_planks",2))state.development().trade(state,id,other.id(),"minecraft:oak_planks","minecraft:cobblestone",2);
            }
            return;
        }
    }
}
