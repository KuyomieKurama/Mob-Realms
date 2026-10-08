package dev.mobrealms.fabric;

import dev.mobrealms.core.*;
import dev.mobrealms.fabric.mixin.MobGoalsAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** All methods run on the server thread. Vanilla owns entity persistence; the core owns cargo. */
public final class RealmController {
    private final MinecraftServer server;
    private final RealmConfig config;
    private final SpeciesDefinitions definitions = new SpeciesDefinitions();
    private final RealmSimulation state;
    private final Path savePath;
    private final Map<UUID, Mob> loaded = new LinkedHashMap<>();
    private final Map<UUID, RealmSimulation.Lease> leases = new HashMap<>();
    private final Map<UUID, UtilityBrain.Goal> goals = new HashMap<>();
    private final Map<UUID, WorkProgress> deliveryProgress = new HashMap<>();
    private final Set<UUID> queued = new HashSet<>();
    private final TickScheduler scheduler = new TickScheduler(2048);
    private final UtilityBrain brain = new UtilityBrain();
    private final EconomyController economy;
    private final Diagnostics diagnostics = new Diagnostics();
    private boolean economyQueued;
    private final DetailAllocation detailAllocation=new DetailAllocation();
    private boolean allocationDirty=true;
    private long ticks;
    private final Set<ChunkKey> keptCampChunks = new HashSet<>();
    private boolean dayQueued, naturalQueued;
    private int naturalAttemptsLeft;
    private boolean healthy = true;
    private static final Set<String> MATERIALS = Set.of("minecraft:rotten_flesh", "minecraft:bone", "minecraft:arrow",
            "minecraft:stick", "minecraft:cobblestone", "minecraft:oak_log", "minecraft:coal", "minecraft:iron_ingot",
            "minecraft:oak_planks","minecraft:birch_log","minecraft:spruce_log","minecraft:wheat","minecraft:wheat_seeds","minecraft:bread","minecraft:raw_iron","minecraft:string","minecraft:gold_ingot");

    public RealmController(MinecraftServer server, RealmConfig config) throws IOException {
        this.server = server; this.config = config;
        savePath = server.getWorldPath(LevelResource.ROOT).resolve("mobrealms/realms.dat");
        state = Files.exists(savePath) ? RealmStore.load(savePath)
                : new RealmSimulation(config.maxCamps(), config.maxPopulation(), config.maxDetailed());
        definitions.reload(server.getResourceManager());
        for (var camp : state.camps()) definitions.get(camp.species());
        economy = new EconomyController(this,server);
        for(var camp:state.camps())if(state.development().town(camp.id()).lastDay<0)economy.endowment(camp.id());
        economy.resumeDays();
    }
    public EconomyController economy() { return economy; }
    public SpeciesProfile profileFor(UUID id) { return definitions.get(state.development().person(id).species); }
    public SpeciesProfile profile(String id) { return definitions.get(id); }
    public Collection<Mob> loadedMobs() { return List.copyOf(loaded.values()); }
    public RealmSimulation.Lease lease(UUID id) { return leases.get(id); }
    public RealmSimulation state() { return state; }
    public List<String> species() { return definitions.ids(); }
    public String goal(UUID id) {
        if(state.development().person(id).pendingSpawn)return "pending";
        if(state.citizen(id).mode()==RealmSimulation.Mode.WAITING)return "budget";
        if(state.citizen(id).mode()==RealmSimulation.Mode.ABSTRACT)return "abstract";
        String work=economy.activity(id); return work.equals("idle") ? goals.getOrDefault(id, UtilityBrain.Goal.IDLE).name().toLowerCase(Locale.ROOT) : work; }
    public int pendingDays() { return state.pendingDays(); }
    public RealmConfig config() { return config; }
    public int cancelDays() { int count = state.cancelDays(); save(); return count; }
    public void reloadDefinitions() {
        // A removed profile cannot invalidate living citizens: reject the whole replacement.
        try {
            SpeciesDefinitions next = new SpeciesDefinitions(); next.reload(server.getResourceManager());
            for (var camp : state.camps()) next.get(camp.species());
            for (var citizen : state.citizens()) next.get(state.development().person(citizen.id()).species);
            definitions.reload(server.getResourceManager());
        } catch (Exception ex) { MobRealms.LOGGER.error("Keeping previous species definitions after invalid reload", ex); }
    }
    public void loadEntity(Entity entity) {
        if (!(entity instanceof Mob mob) || !state.hasCitizen(entity.getUUID()) || loaded.containsKey(entity.getUUID())) return;
        if(mob.isDeadOrDying()){died(mob);return;}
        loaded.put(mob.getUUID(), mob);
        updateIdentity(mob);
        var access = (MobGoalsAccess) mob;
        access.mobrealms$goals().removeAllGoals(g -> true);
        access.mobrealms$targets().removeAllGoals(g -> true);
        access.mobrealms$goals().addGoal(0, new FloatGoal(mob));
        mob.setTarget(null); mob.setPersistenceRequired(); mob.setCanPickUpLoot(false);
        // Our work AI replaces vanilla sun-seeking. Protect undead workers from daylight attrition.
        if (sunSensitive(mob)) mob.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 72000, 0, true, false, false));
        // 26.3 navigation otherwise derives its search length from FOLLOW_RANGE (often only 16).
        // PathNavigationRegion uses getChunkNow, so this does not force-load the larger area.
        mob.getNavigation().setRequiredPathLength(96);
        state.markLoadedWaiting(mob.getUUID());mob.setNoAi(true);allocationDirty=true;
    }
    private void updateIdentity(Mob mob){
        var p=state.development().person(mob.getUUID());
        var title=Component.translatable("rank.mobrealms."+state.development().socialRank(mob.getUUID(),state)).withStyle(ChatFormatting.GRAY);
        var label=Component.literal("✦ ").withStyle(ChatFormatting.AQUA)
                .append(Component.literal(p.name).withStyle(ChatFormatting.GOLD))
                .append(Component.literal("  •  ").withStyle(ChatFormatting.DARK_GRAY)).append(title)
                .append(Component.literal("  ★"+(p.rank()+1)).withStyle(ChatFormatting.GREEN));
        if(!label.equals(mob.getCustomName()))mob.setCustomName(label);
        mob.setCustomNameVisible(true);
    }
    private void activate(Mob mob) {
        if (leases.containsKey(mob.getUUID())) return;
        if (leases.size() >= state.maxDetailed()) { mob.setNoAi(true); return; }
        leases.put(mob.getUUID(), state.activate(mob.getUUID())); mob.setNoAi(false);
    }
    private boolean sunSensitive(Mob mob) {
        String species=state.development().person(mob.getUUID()).species;
        return species.endsWith(":zombie") || species.endsWith(":skeleton");
    }
    private void allocateDetail(){
        Map<UUID,List<UUID>> towns=new TreeMap<>();
        for(var mob:loaded.values())if(mob.isAlive()&&state.hasCitizen(mob.getUUID()))towns.computeIfAbsent(state.citizen(mob.getUUID()).camp(),id->new ArrayList<>()).add(mob.getUUID());
        towns.values().forEach(list->list.sort(UUID::compareTo));
        var selected=detailAllocation.select(towns,state.maxDetailed());
        for(UUID id:new ArrayList<>(leases.keySet()))if(!selected.contains(id)){
            state.waitForDetail(leases.remove(id));var mob=loaded.get(id);if(mob!=null){mob.getNavigation().stop();mob.setNoAi(true);}
        }
        for(UUID id:selected)activate(loaded.get(id));allocationDirty=false;
    }
    public int activeResidents(UUID camp){return (int)leases.keySet().stream().filter(id->state.hasCitizen(id)&&state.citizen(id).camp().equals(camp)).count();}
    public int loadedResidents(UUID camp){return (int)loaded.keySet().stream().filter(id->state.hasCitizen(id)&&state.citizen(id).camp().equals(camp)).count();}
    public void unloadEntity(Entity entity) {
        UUID id = entity.getUUID(); loaded.remove(id); goals.remove(id);deliveryProgress.remove(id);economy.forget(id);
        var lease = leases.remove(id);
        if (lease != null && state.hasCitizen(id)) state.deactivate(lease);
        if(state.hasCitizen(id))state.markUnloaded(id);allocationDirty=true;
        if (entity instanceof Mob mob && mob.isDeadOrDying()) died(mob);
    }
    public void died(Mob mob) {
        UUID id=mob.getUUID();if(!state.hasCitizen(id))return;
        economy.death(mob);state.recordDeath(id);
        loaded.remove(id);leases.remove(id);goals.remove(id);deliveryProgress.remove(id);queued.remove(id);
        mob.getNavigation().stop();allocationDirty=true;
    }
    public void save() {
        if (!healthy) return;
        long started = config.diagnosticsEnabled() ? System.nanoTime() : 0;
        try { RealmStore.save(savePath, state); }
        catch (IOException ex) { healthy = false; MobRealms.LOGGER.error("Mob Realms halted after save failure; previous save retained", ex); }
        finally { if (config.diagnosticsEnabled()) diagnostics.recordSave(System.nanoTime() - started); }
    }
    public boolean healthy() { return healthy; }
    public boolean enqueueDays(int days) {
        if (!healthy || !state.enqueueDays(days)) return false;
        save(); return healthy;
    }
    private final Map<UUID,Long> recruitmentCooldown=new HashMap<>();
    private int recruitmentCursor;
    private void recruitNearby(){
        var camps=state.camps();if(camps.isEmpty())return;
        var camp=camps.get(Math.floorMod(recruitmentCursor++,camps.size()));
        if(ticks<recruitmentCooldown.getOrDefault(camp.id(),0L)||state.population(camp.id())>=state.development().town(camp.id()).housing())return;
        var home=new BlockPos(camp.x(),camp.y(),camp.z());
        if(state.population(camp.id())<2){
            for(ServerLevel level:server.getAllLevels()){
                if(!dimension(level).equals(camp.territory().dimension())||!level.hasChunkAt(home))continue;
                String type=profile(camp.species()).entityType();
                var candidates=level.getEntitiesOfClass(Mob.class,new AABB(home).inflate(16,8,16),m->
                    wildRecruit(m,type)&&m.blockPosition().distSqr(home)<=16*16);
                for(var candidate:candidates){
                    if(state.resettle(candidate.getUUID(),camp.id())){
                        loadEntity(candidate);
                        recruitmentCooldown.put(camp.id(),ticks+1200L);
                        MobRealms.LOGGER.info("Mob Realms resettlement: camp={} resident={} population={}",
                                camp.id(),candidate.getUUID(),state.population(camp.id()));
                        save();return;
                    }
                }
                if((state.stock(camp.id()).getOrDefault("minecraft:bread",0L)>=8
                            ||state.stock(camp.id()).getOrDefault("minecraft:wheat_seeds",0L)>=12)
                        && state.residents(camp.id()).stream().noneMatch(c->state.development().person(c.id()).pendingSpawn)
                        && safeRefugeeSpawn(level,home,type)){
                    UUID refugee=UUID.randomUUID();
                    if(state.immigrate(refugee,camp.id())){
                        recruitmentCooldown.put(camp.id(),ticks+1200L);
                        MobRealms.LOGGER.info("Mob Realms immigration: camp={} resident={} population={}",
                                camp.id(),refugee,state.population(camp.id()));
                        save();return;
                    }
                }
            }
            return;
        }
        Mob recruiter=loaded.values().stream().filter(m->m.isAlive()&&leases.containsKey(m.getUUID())&&state.citizen(m.getUUID()).camp().equals(camp.id())&&m.getTarget()==null&&m.blockPosition().distSqr(home)<=144).findFirst().orElse(null);
        if(recruiter==null)return;
        var level=(ServerLevel)recruiter.level();
        if(!dimension(level).equals(camp.territory().dimension()))return;
        String type=profile(camp.species()).entityType();
        var candidates=level.getEntitiesOfClass(Mob.class,recruiter.getBoundingBox().inflate(4),m->
            wildRecruit(m,type));
        for(var candidate:candidates){
            if(!recruiter.hasLineOfSight(candidate))continue;
            recruiter.getLookControl().setLookAt(candidate,30,30);
            if(state.admit(candidate.getUUID(),camp.id())){
                loadEntity(candidate);recruitmentCooldown.put(camp.id(),ticks+1200L);return;
            }
        }
    }
    private boolean wildRecruit(Mob mob,String type){
        return mob.isAlive()&&!mob.isBaby()&&!state.hasCitizen(mob.getUUID())&&!mob.hasCustomName()&&!mob.isPersistenceRequired()
                &&!mob.isLeashed()&&!mob.isPassenger()&&!mob.isVehicle()&&!(mob instanceof net.minecraft.world.entity.TamableAnimal)
                &&mob.getTarget()==null&&BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString().equals(type);
    }
    private boolean safeRefugeeSpawn(ServerLevel level,BlockPos home,String type){
        var entity=BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(type)).create(level,EntitySpawnReason.EVENT);
        if(!(entity instanceof Mob mob))return false;
        for(int dx=-4;dx<=4;dx++)for(int dz=-4;dz<=4;dz++)for(int dy=-2;dy<=2;dy++){
            var pos=home.offset(dx,dy,dz);
            if(!level.hasChunkAt(pos)||!level.getWorldBorder().isWithinBounds(pos)
                    ||!level.getBlockState(pos.below()).isSolidRender())continue;
            mob.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);
            if(level.noCollision(mob))return true;
        }
        return false;
    }
    public void tick() {
        if (!healthy) return;
        long tickStarted = config.diagnosticsEnabled() ? System.nanoTime() : 0;
        ticks++;
        if (ticks % 1200 == 0) for (Mob mob : loaded.values())
            if (mob.isAlive() && sunSensitive(mob) && !mob.hasEffect(MobEffects.FIRE_RESISTANCE))
                mob.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 72000, 0, true, false, false));
        if (ticks % 20 == 1) keepCampChunksLoaded();
        if (ticks % 20 == 0) {
            int passed = state.observeWorldDay(Math.max(0, Math.floorDiv(server.overworld().getOverworldClockTime(), 24000L)), 7);
            int accepted = Math.min(RealmSimulation.MAX_PENDING_DAYS - state.pendingDays(), passed);
            if (accepted > 0) state.enqueueDays(accepted);
        }
        if (state.pendingDays() > 0 && !dayQueued && !economy.busy()) {
            dayQueued = scheduler.submit(() -> diagnostics.measure(0, () -> {
                dayQueued = false;
                if (state.processDaySlice(32)) {
                    economy.dayCompleted();
                    if (config.naturalCamps() && state.day() >= config.graceDays())
                        naturalAttemptsLeft = Math.max(naturalAttemptsLeft, config.naturalAttempts());
                    if (state.pendingDays() == 0) save();
                }
            }));
        }
        if(allocationDirty||ticks%200==0)diagnostics.measure(4, this::allocateDetail);
        for (var mob : loaded.values()) {
            if (!mob.isAlive()) continue;
            if (!leases.containsKey(mob.getUUID())) continue;
            var person=state.development().person(mob.getUUID());
            var branch=ResidentSkills.branch(person.role);
            int skill=person.skillLevel(branch)+(PuffishNpcBridge.unlocked(person,branch,true)?2:0);
            int interval=Math.max(1,config.aiInterval()-(ResidentSkills.workBonus(mob.getUUID(),person.role,person.experience)+skill)*2);
            if (Math.floorMod(mob.getUUID().hashCode(), interval) != ticks % interval) continue;
            UUID id = mob.getUUID();
            if (queued.add(id) && !scheduler.submit(() -> diagnostics.resident(mob, () -> {
                queued.remove(id);
                if (loaded.get(id) == mob && leases.containsKey(id) && mob.isAlive()) update(mob);
            }))) queued.remove(id);
        }
        if (config.naturalCamps() && ticks % config.naturalIntervalTicks() == 0
                && Math.max(state.day(), server.overworld().getOverworldClockTime() / 24000L) >= config.graceDays())
            naturalAttemptsLeft = Math.max(naturalAttemptsLeft, config.naturalAttempts());
        if (naturalAttemptsLeft > 0 && !naturalQueued) {
            naturalQueued = scheduler.submit(() -> diagnostics.measure(3, () -> {
                naturalQueued = false; naturalAttemptsLeft--;
                if (state.camps().size() >= state.maxCamps() || naturalCamp()) naturalAttemptsLeft = 0;
            }));
        }
        if(!economyQueued && (ticks % 10 == 0 || economy.busy()))economyQueued=scheduler.submit(() -> diagnostics.measure(2, () -> {economyQueued=false;economy.step();}));
        if(ticks%100==0)scheduler.submit(() -> diagnostics.measure(5, this::recruitNearby));
        long schedulerStarted = config.diagnosticsEnabled() ? System.nanoTime() : 0;
        int completed = scheduler.run(config.budgetNanos(), config.workPerTick());
        if (config.diagnosticsEnabled()) diagnostics.tick(System.nanoTime() - tickStarted, System.nanoTime() - schedulerStarted, completed);
    }
    /** Keep the home and the nearby work area entity-ticking even when nobody is online. */
    private void keepCampChunksLoaded() {
        int added = 0;
        for (var camp : state.camps()) {
            ServerLevel level = server.getAllLevels().iterator().next();
            for (ServerLevel candidate : server.getAllLevels())
                if (dimension(candidate).equals(camp.territory().dimension())) { level = candidate; break; }
            if (!dimension(level).equals(camp.territory().dimension())) continue;
            int cx = Math.floorDiv(camp.x(), 16), cz = Math.floorDiv(camp.z(), 16);
            for (int dx = -3; dx <= 3; dx++) for (int dz = -3; dz <= 3; dz++) {
                int x = cx + dx, z = cz + dz;
                ChunkKey key = new ChunkKey(camp.territory().dimension(), x, z);
                if (keptCampChunks.add(key) && !level.getForceLoadedChunks().contains(net.minecraft.world.level.ChunkPos.pack(x, z))) {
                    level.setChunkForced(x, z, true);
                    added++;
                    if (added >= 16) {
                        MobRealms.LOGGER.info("Mob Realms kept {} additional camp chunks loaded for offline work ({} tracked)", added, keptCampChunks.size());
                        return;
                    }
                }
            }
        }
        if (added > 0) MobRealms.LOGGER.info("Mob Realms kept {} additional camp chunks loaded for offline work ({} tracked)", added, keptCampChunks.size());
    }
    private final class Diagnostics {
        private static final int INTERVAL = 1200;
        private final String[] names = {"day", "resident_ai", "economy", "natural_founding", "allocation", "recruitment"};
        private final long[] workNanos = new long[names.length], workMax = new long[names.length];
        private final int[] workCount = new int[names.length];
        private long tickNanos, tickMax, schedulerNanos, saveNanos, saveMax;
        private int tickCount, slowTicks, overBudget, queuePeak, completedTasks, saveCount, localDeliveries;
        private UUID slowestResident;
        private long slowestResidentNanos;
        void measure(int category, Runnable task) {
            if (!config.diagnosticsEnabled()) { task.run(); return; }
            long started = System.nanoTime();
            try { task.run(); }
            finally {
                long elapsed = System.nanoTime() - started;
                workNanos[category] += elapsed;
                workMax[category] = Math.max(workMax[category], elapsed);
                workCount[category]++;
            }
        }
        void recordSave(long elapsed) {
            saveNanos += elapsed;
            saveMax = Math.max(saveMax, elapsed);
            saveCount++;
        }
        void resident(Mob mob, Runnable task) {
            if (!config.diagnosticsEnabled()) { task.run(); return; }
            long started = System.nanoTime();
            try { task.run(); }
            finally {
                long elapsed = System.nanoTime() - started;
                workNanos[1] += elapsed;
                workMax[1] = Math.max(workMax[1], elapsed);
                workCount[1]++;
                if (elapsed > slowestResidentNanos) {
                    slowestResident = mob.getUUID();
                    slowestResidentNanos = elapsed;
                }
            }
        }
        void tick(long elapsed, long schedulerElapsed, int completed) {
            tickNanos += elapsed;
            tickMax = Math.max(tickMax, elapsed);
            schedulerNanos += schedulerElapsed;
            completedTasks += completed;
            queuePeak = Math.max(queuePeak, scheduler.pending());
            if (elapsed >= 10_000_000L) slowTicks++;
            if (schedulerElapsed >= config.budgetNanos() && scheduler.pending() > 0) overBudget++;
            if (++tickCount == INTERVAL) report();
        }
        private void report() {
            MobRealms.LOGGER.info("Mob Realms diagnostics: ticks={} mod_avg_ms={} mod_max_ms={} mod_slow_10ms={} scheduler_avg_ms={} budget_backlog_ticks={} tasks={} queue_now={} queue_peak={} residents={}/{} loaded={} active={} local_deliveries={} saves={} save_total_ms={} save_max_ms={}",
                    tickCount, ms(tickNanos / tickCount), ms(tickMax), slowTicks, ms(schedulerNanos / tickCount), overBudget,
                    completedTasks, scheduler.pending(), queuePeak, state.citizenCount(), state.maxPopulation(), loaded.size(), leases.size(), localDeliveries, saveCount, ms(saveNanos), ms(saveMax));
            for (int i = 0; i < names.length; i++) if (workCount[i] > 0)
                MobRealms.LOGGER.info("Mob Realms work: type={} calls={} total_ms={} max_ms={}", names[i], workCount[i], ms(workNanos[i]), ms(workMax[i]));
            if (slowestResident != null) MobRealms.LOGGER.info("Mob Realms slowest resident: id={} max_ai_ms={}", slowestResident, ms(slowestResidentNanos));
            Map<String,Integer> activities = new TreeMap<>();
            for (UUID id : leases.keySet()) activities.merge(goal(id), 1, Integer::sum);
            MobRealms.LOGGER.info("Mob Realms goals: {}", activities);
            for (var mob : loaded.values()) {
                UUID id = mob.getUUID();
                if (!leases.containsKey(id) || !state.hasCitizen(id)) continue;
                var citizen = state.citizen(id);
                var camp = state.camp(citizen.camp());
                MobRealms.LOGGER.info("Mob Realms resident: id={} camp={} role={} goal={} pos={} home={} nav_done={} nav_target={} wanted={} blocked_by={} retries={} progress_age_s={}",
                        id, camp.id(), state.development().person(id).role, goal(id), mob.blockPosition().toShortString(),
                        camp.x() + "," + camp.y() + "," + camp.z(), mob.getNavigation().isDone(), mob.getNavigation().getTargetPos(),
                        economy.wanted(id), economy.blockedBy(id), economy.retries(id), economy.sinceProgress(id));
            }
            for (var camp : state.camps()) {
                var town = state.development().town(camp.id());
                var project = town.project;
                MobRealms.LOGGER.info("Mob Realms camp: id={} species={} residents={} loaded={} active={} obstacle={} project={} phase={} progress={} paid={} tiles={} cobblestone={} planks={} research={} technologies={}",
                        camp.id(), camp.species(), state.population(camp.id()), loadedResidents(camp.id()), activeResidents(camp.id()),
                        town.obstacle, project == null ? "none" : project.building, project == null ? "none" : project.phase(),
                        project == null ? 0 : project.progress, project == null ? 0 : project.paid, project == null ? 0 : project.tiles.size(),
                        state.stock(camp.id()).getOrDefault("minecraft:cobblestone",0L),state.stock(camp.id()).getOrDefault("minecraft:oak_planks",0L),
                        town.research,town.technologies);
            }
            Arrays.fill(workNanos, 0); Arrays.fill(workMax, 0); Arrays.fill(workCount, 0);
            tickNanos = tickMax = schedulerNanos = saveNanos = saveMax = 0;
            tickCount = slowTicks = overBudget = queuePeak = completedTasks = saveCount = localDeliveries = 0;
            slowestResident = null; slowestResidentNanos = 0;
        }
        private String ms(long nanos) { return String.format(Locale.ROOT, "%.3f", nanos / 1_000_000.0); }
    }
    private void update(Mob mob) {
        updateIdentity(mob);
        economy.clearActivity(mob.getUUID());
        var citizen = state.citizen(mob.getUUID()); var camp = state.camp(citizen.camp());
        ServerLevel level = (ServerLevel) mob.level();
        if (!dimension(level).equals(camp.territory().dimension())) { mob.getNavigation().stop(); return; }
        var profile = profileFor(mob.getUUID());
        BlockPos home = economy.home(mob.getUUID(),camp);
        economy.recover(mob,camp,new BlockPos(camp.x(),camp.y(),camp.z()));
        double distance = Math.sqrt(mob.distanceToSqr(home.getX() + .5, home.getY(), home.getZ() + .5));
        boolean sunny = level.dimensionType().hasSkyLight() && level.isBrightOutside() && !level.isRaining();
        boolean daylightRest=sunny&&profile.avoidsSun()&&mob.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD).isEmpty();
        boolean needsRest=mob.getHealth()<mob.getMaxHealth()*.25&&(mob.isOnFire()||state.stock(camp.id()).getOrDefault("minecraft:bread",0L)>0);
        var depot=new BlockPos(camp.x(),camp.y(),camp.z());
        boolean carrying = !citizen.cargo().isEmpty();
        if (carrying) {
            if(deliverAtSettlement(mob, level, camp, depot)){
                carrying=false;deliveryProgress.remove(mob.getUUID());
            }else{
                double distanceToDepot=Math.sqrt(mob.distanceToSqr(depot.getX()+.5,depot.getY(),depot.getZ()+.5));
                var watchdog=deliveryProgress.computeIfAbsent(mob.getUUID(),id->new WorkProgress(level.getGameTime(),distanceToDepot));
                if(watchdog.expired(level.getGameTime(),distanceToDepot)){
                    if(rescueCarrier(mob,level,camp,depot)&&deliverAtSettlement(mob,level,camp,depot))carrying=false;
                    deliveryProgress.remove(mob.getUUID());
                }
            }
        }else deliveryProgress.remove(mob.getUUID());
        if(!daylightRest && !needsRest && !carrying && economy.work(mob,level,camp)) { goals.put(mob.getUUID(),UtilityBrain.Goal.IDLE); return; }
        economy.releaseResource(mob.getUUID());
        ItemEntity target = null;
        if (!daylightRest) {
            var candidates = level.getEntitiesOfClass(ItemEntity.class, new AABB(home).inflate(12), e -> suitable(e, level) && state.development().town(camp.id()).claims.contains(ChunkKey.fromBlock(dimension(level), e.blockPosition().getX(), e.blockPosition().getZ())));
            target = candidates.stream().min(Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);
        }
        var goal = brain.choose(profile, new UtilityBrain.Observation(daylightRest, needsRest,
                carrying, target != null, distance, citizen.diligence()));
        goals.put(mob.getUUID(), goal);
        switch (goal) {
            case SHELTER, REGROUP -> {
                var refuge=needsRest?depot:home;
                if(mob.distanceToSqr(refuge.getX()+.5,refuge.getY(),refuge.getZ()+.5)<4&&(!daylightRest||!level.canSeeSky(mob.blockPosition()))){
                    mob.getNavigation().stop();economy.activity(mob.getUUID(),needsRest?"recover":daylightRest?"rest_day":"idle");
                }else{
                    var path=mob.getNavigation().createPath(refuge,0);
                    if(path!=null&&path.canReach())mob.getNavigation().moveTo(path,1);
                    else{mob.getNavigation().stop();economy.activity(mob.getUUID(),"shelter_blocked");}
                }
            }
            case DELIVER -> {
                if(deliverAtSettlement(mob,level,camp,depot)){mob.getNavigation().stop();}
                else if(!economy.approach(mob,level,depot))economy.activity(mob.getUUID(),"delivery_blocked");
            }
            case GATHER -> {
                if (target == null) break;
                if (mob.distanceToSqr(target) > 2.5) mob.getNavigation().moveTo(target, 1.0);
                else {
                    ItemStack stack = target.getItem(); int count = Math.min(stack.getCount(), profile.carryingCapacity());
                    String item = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                    ItemStack removed = stack.split(count);
                    try { state.collect(leases.get(mob.getUUID()), item, count, profile.carryingCapacity()); }
                    catch (RuntimeException ex) { stack.grow(removed.getCount()); throw ex; }
                    if (stack.isEmpty()) target.discard(); else target.setItem(stack);
                }
            }
            case PATROL -> patrol(mob, level, camp, home);
            case IDLE -> mob.getNavigation().stop();
        }
    }
    private boolean deliverAtSettlement(Mob mob, ServerLevel level, RealmSimulation.Camp camp, BlockPos depot) {
        double dx = mob.getX() - depot.getX() - .5, dz = mob.getZ() - depot.getZ() - .5;
        boolean atDepot = mob.distanceToSqr(depot.getX() + .5, depot.getY(), depot.getZ() + .5) < 16;
        // Residents gathering just beyond a claim edge can still hand cargo to their nearby settlement.
        boolean localDropoff = dx * dx + dz * dz <= 32 * 32 && Math.abs(mob.getY() - depot.getY()) <= 24;
        if (!atDepot && !localDropoff) return false;
        state.deliver(leases.get(mob.getUUID()));
        economy.progress(mob.getUUID());
        if (localDropoff && !atDepot && config.diagnosticsEnabled()) diagnostics.localDeliveries++;
        return true;
    }
    private boolean rescueCarrier(Mob mob,ServerLevel level,RealmSimulation.Camp camp,BlockPos depot){
        for(int radius=0;radius<=4;radius++)for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++){
            if(Math.max(Math.abs(dx),Math.abs(dz))!=radius)continue;
            for(int dy=-2;dy<=3;dy++){
                var feet=depot.offset(dx,dy,dz);
                var chunk=ChunkKey.fromBlock(dimension(level),feet.getX(),feet.getZ());
                if(!level.hasChunkAt(feet)||!state.development().town(camp.id()).claims.contains(chunk)||state.protectedAt(chunk)
                        ||!level.getWorldBorder().isWithinBounds(feet)||!level.getFluidState(feet).isEmpty()
                        ||!level.getBlockState(feet.below()).isFaceSturdy(level,feet.below(),net.minecraft.core.Direction.UP)
                        ||!level.noCollision(mob,mob.getBoundingBox().move(feet.getX()+.5-mob.getX(),feet.getY()-mob.getY(),feet.getZ()+.5-mob.getZ())))continue;
                var from=mob.blockPosition();mob.getNavigation().stop();mob.setPos(feet.getX()+.5,feet.getY(),feet.getZ()+.5);
                MobRealms.LOGGER.warn("Rescued stranded carrier {} for camp {} from {} to {}",mob.getUUID(),camp.id(),from,feet);
                return true;
            }
        }
        return false;
    }
    private void patrol(Mob mob, ServerLevel level, RealmSimulation.Camp camp, BlockPos home) {
        if (!mob.getNavigation().isDone()) return;
        var random = level.getRandom();
        for (int attempt = 0; attempt < 8; attempt++) {
            int dx = random.nextInt(15) - 7, dz = random.nextInt(15) - 7;
            if (dx * dx + dz * dz < 9) continue;
            BlockPos target = home.offset(dx, 0, dz);
            if (!state.development().town(camp.id()).claims.contains(ChunkKey.fromBlock(dimension(level), target.getX(), target.getZ()))
                    || state.protectedAt(ChunkKey.fromBlock(dimension(level), target.getX(), target.getZ())) || !level.hasChunkAt(target)) continue;
            for (int dy = -2; dy <= 2; dy++) {
                BlockPos feet = target.offset(0, dy, 0);
                if (level.getBlockState(feet.below()).isSolidRender() && level.isEmptyBlock(feet)
                        && level.isEmptyBlock(feet.above()) && level.getWorldBorder().isWithinBounds(feet)) {
                    if (mob.getNavigation().moveTo(feet.getX() + .5, feet.getY(), feet.getZ() + .5, .8)) return;
                }
            }
        }
    }
    private boolean suitable(ItemEntity entity, ServerLevel level) {
        ItemStack stack = entity.getItem();
        return entity.isAlive() && !entity.hasPickUpDelay() && !stack.isEmpty()
                && !state.protectedAt(ChunkKey.fromBlock(dimension(level), entity.blockPosition().getX(), entity.blockPosition().getZ()))
                && MATERIALS.contains(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString())
                && ItemStack.isSameItemSameComponents(stack, new ItemStack(stack.getItem()));
    }
    /** Starter roof and banner are a one-time camp endowment, not a production chain. */
    public boolean found(ServerLevel level, BlockPos origin, String species) {
        if (!healthy || !definitions.allows(species,dimension(level))) return false;
        SpeciesProfile profile = definitions.get(species);
        ChunkKey chunk = ChunkKey.fromBlock(dimension(level), origin.getX(), origin.getZ());
        if (!state.canFound(chunk) || state.citizens().size() + 3 > state.maxPopulation()) return false;
        if (state.camps().stream().anyMatch(c -> c.territory().dimension().equals(dimension(level))
                && Math.hypot(c.x() - origin.getX(), c.z() - origin.getZ()) < 48)) return false;
        // All touched blocks must be air, loaded, unprotected and within this one claimed chunk.
        List<BlockPos> roof = new ArrayList<>();
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            BlockPos floor = origin.offset(dx, -1, dz);
            if (!level.hasChunkAt(floor) || !level.getBlockState(floor).isSolidRender()) return false;
            for (int dy = 0; dy <= 4; dy++) {
                BlockPos p = origin.offset(dx, dy, dz);
                if (!chunk.equals(ChunkKey.fromBlock(dimension(level), p.getX(), p.getZ()))
                        || !level.hasChunkAt(p) || !level.getWorldBorder().isWithinBounds(p) || !level.isEmptyBlock(p)) return false;
            }
            roof.add(origin.offset(dx, 4, dz));
        }
        var type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(profile.entityType()));
        if (type == null) return false;
        List<Mob> residents = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Entity entity;
            try { entity = type.create(level, EntitySpawnReason.EVENT); }
            catch (RuntimeException ex) {
                MobRealms.LOGGER.warn("Species {} cannot create a resident", species, ex); return false;
            }
            if (!(entity instanceof Mob mob)) return false;
            mob.setPos(origin.getX() + .5 + (i - 1) * .6, origin.getY(), origin.getZ() + .5);
            mob.setPersistenceRequired();
            if(mob instanceof net.minecraft.world.entity.monster.piglin.Piglin piglin)piglin.setImmuneToZombification(true);
            residents.add(mob);
        }
        for (var p : roof) level.setBlock(p, Blocks.SPRUCE_PLANKS.defaultBlockState(), 3);
        for(int dx:new int[]{-1,1})for(int dz:new int[]{-1,1})for(int dy=0;dy<4;dy++)level.setBlock(origin.offset(dx,dy,dz),Blocks.OAK_FENCE.defaultBlockState(),3);
        level.setBlock(origin.above(3),Blocks.LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING,true),3);
        BlockPos marker = origin.offset(0, 0, 1);
        level.setBlock(marker, BuiltInRegistries.BLOCK.getValue(Identifier.parse(species.endsWith("skeleton") ? "minecraft:blue_banner" : "minecraft:green_banner")).defaultBlockState(), 3);
        UUID campId = UUID.randomUUID();
        state.found(new RealmSimulation.Camp(campId, species, chunk, origin.getX(), origin.getY(), origin.getZ()));
        for (Mob mob : residents) {
            state.addCitizen(mob.getUUID(), campId, .35 + (Math.floorMod(mob.getUUID().hashCode(), 60) / 100.0));
            mob.setCustomName(Component.translatable("entity.mobrealms.citizen", Component.translatable("species." + species.replace(':', '.'))));
            if (!level.addFreshEntity(mob)) state.removeCitizen(mob.getUUID());
            else loadEntity(mob);
        }
        economy.endowment(campId);
        save(); return true;
    }
    private boolean naturalCamp() {
        var players = server.getPlayerList().getPlayers().stream()
                .toList();
        if (players.isEmpty()) return false;
        var random = server.overworld().getRandom();
        var player = players.get(random.nextInt(players.size())); ServerLevel level = (ServerLevel) player.level();
        int x = player.blockPosition().getX() + random.nextInt(193) - 96;
        int z = player.blockPosition().getZ() + random.nextInt(193) - 96;
        BlockPos candidate = new BlockPos(x, player.blockPosition().getY(), z);
        if (!level.hasChunkAt(candidate) || Math.hypot(x - player.getX(), z - player.getZ()) < 32) return false;
        BlockPos surface = level.dimension().equals(Level.NETHER) ? candidate : level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, candidate);
        if(level.dimension().equals(Level.NETHER)){
            for(int dy=-4;dy<=4;dy++){BlockPos p=candidate.offset(0,dy,0);if(level.getBlockState(p.below()).isSolidRender()&&level.isEmptyBlock(p)){surface=p;break;}}
        }
        // Conservative terrain filter; placed natural blocks cannot be distinguished. Protect player land explicitly.
        for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
            if (!level.hasChunkAt(surface.offset(dx, -1, dz))) return false;
            var floor = level.getBlockState(surface.offset(dx, -1, dz));
            if (!(floor.is(Blocks.GRASS_BLOCK) || floor.is(Blocks.DIRT) || floor.is(Blocks.SAND)
                    || floor.is(Blocks.PODZOL) || floor.is(Blocks.MYCELIUM) || floor.is(Blocks.SNOW_BLOCK)||floor.is(Blocks.NETHERRACK)||floor.is(Blocks.CRIMSON_NYLIUM)||floor.is(Blocks.WARPED_NYLIUM)||floor.is(Blocks.END_STONE))) return false;
        }
        var ids = definitions.ids().stream().filter(id->definitions.allows(id,dimension(level))).toList();
        return !ids.isEmpty() && found(level, surface, ids.get(random.nextInt(ids.size())));
    }
    public static String dimension(ServerLevel level) { return level.dimension().identifier().toString(); }
}
