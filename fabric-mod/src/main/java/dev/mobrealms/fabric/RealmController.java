package dev.mobrealms.fabric;

import dev.mobrealms.core.*;
import dev.mobrealms.fabric.mixin.MobGoalsAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.item.ItemEntity;
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
    private final Set<UUID> queued = new HashSet<>();
    private final TickScheduler scheduler = new TickScheduler(2048);
    private final UtilityBrain brain = new UtilityBrain();
    private final EconomyController economy;
    private boolean economyQueued;
    private long ticks;
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
    public String goal(UUID id) { String work=economy.activity(id); return work.equals("idle") ? goals.getOrDefault(id, UtilityBrain.Goal.IDLE).name().toLowerCase(Locale.ROOT) : work; }
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
        loaded.put(mob.getUUID(), mob);
        if(mob instanceof SettlerEntity&&!mob.hasCustomName())mob.setCustomName(Component.translatable("name.mobrealms."+Math.floorMod(mob.getUUID().hashCode(),12)));
        var access = (MobGoalsAccess) mob;
        access.mobrealms$goals().removeAllGoals(g -> true);
        access.mobrealms$targets().removeAllGoals(g -> true);
        access.mobrealms$goals().addGoal(0, new FloatGoal(mob));
        mob.setTarget(null); mob.setPersistenceRequired(); mob.setCanPickUpLoot(false);
        activate(mob);
    }
    private void activate(Mob mob) {
        if (leases.containsKey(mob.getUUID())) return;
        if (leases.size() >= state.maxDetailed()) { mob.setNoAi(true); return; }
        leases.put(mob.getUUID(), state.activate(mob.getUUID())); mob.setNoAi(false);
    }
    public void unloadEntity(Entity entity) {
        UUID id = entity.getUUID(); loaded.remove(id); goals.remove(id);
        var lease = leases.remove(id);
        if (lease != null && state.hasCitizen(id)) state.deactivate(lease);
        if (state.hasCitizen(id) && entity instanceof Mob mob && mob.isDeadOrDying()) { economy.death(mob); state.removeCitizen(id); }
    }
    public void save() {
        if (!healthy) return;
        try { RealmStore.save(savePath, state); }
        catch (IOException ex) { healthy = false; MobRealms.LOGGER.error("Mob Realms halted after save failure; previous save retained", ex); }
    }
    public boolean healthy() { return healthy; }
    public boolean enqueueDays(int days) {
        if (!healthy || !state.enqueueDays(days)) return false;
        save(); return healthy;
    }
    public void tick() {
        if (!healthy) return;
        ticks++;
        if (ticks % 20 == 0) {
            int passed = state.observeWorldDay(Math.max(0, Math.floorDiv(server.overworld().getOverworldClockTime(), 24000L)), 7);
            int accepted = Math.min(RealmSimulation.MAX_PENDING_DAYS - state.pendingDays(), passed);
            if (accepted > 0) state.enqueueDays(accepted);
        }
        if (state.pendingDays() > 0 && !dayQueued && !economy.busy()) {
            dayQueued = scheduler.submit(() -> {
                dayQueued = false;
                if (state.processDaySlice(32)) {
                    economy.dayCompleted();
                    if (config.naturalCamps() && state.day() >= config.graceDays())
                        naturalAttemptsLeft = Math.max(naturalAttemptsLeft, config.naturalAttempts());
                    if (state.pendingDays() == 0) save();
                }
            });
        }
        for (var mob : loaded.values()) {
            if (!mob.isAlive()) continue;
            activate(mob);
            if (!leases.containsKey(mob.getUUID())) continue;
            if (Math.floorMod(mob.getUUID().hashCode(), config.aiInterval()) != ticks % config.aiInterval()) continue;
            UUID id = mob.getUUID();
            if (queued.add(id) && !scheduler.submit(() -> {
                queued.remove(id);
                if (loaded.get(id) == mob && leases.containsKey(id) && mob.isAlive()) update(mob);
            })) queued.remove(id);
        }
        if (config.naturalCamps() && ticks % config.naturalIntervalTicks() == 0
                && Math.max(state.day(), server.overworld().getOverworldClockTime() / 24000L) >= config.graceDays())
            naturalAttemptsLeft = Math.max(naturalAttemptsLeft, config.naturalAttempts());
        if (naturalAttemptsLeft > 0 && !naturalQueued) {
            naturalQueued = scheduler.submit(() -> {
                naturalQueued = false; naturalAttemptsLeft--;
                if (state.camps().size() >= state.maxCamps() || naturalCamp()) naturalAttemptsLeft = 0;
            });
        }
        if(!economyQueued && (ticks % 10 == 0 || economy.busy()))economyQueued=scheduler.submit(() -> {economyQueued=false;economy.step();});
        scheduler.run(config.budgetNanos(), config.workPerTick());
    }
    private void update(Mob mob) {
        economy.clearActivity(mob.getUUID());
        var citizen = state.citizen(mob.getUUID()); var camp = state.camp(citizen.camp());
        ServerLevel level = (ServerLevel) mob.level();
        if (!dimension(level).equals(camp.territory().dimension())) { mob.getNavigation().stop(); return; }
        var profile = profileFor(mob.getUUID());
        BlockPos home = economy.home(mob.getUUID(),camp);
        double distance = Math.sqrt(mob.distanceToSqr(home.getX() + .5, home.getY(), home.getZ() + .5));
        boolean sunny = Math.floorMod(level.getOverworldClockTime(), 24000L) < 12000 && !level.isRaining();
        if((!sunny || !profile.avoidsSun()) && mob.getHealth() >= mob.getMaxHealth()*.25 && citizen.cargo().isEmpty() && economy.work(mob,level,camp)) return;
        ItemEntity target = null;
        if (!sunny || !profile.avoidsSun()) {
            var candidates = level.getEntitiesOfClass(ItemEntity.class, new AABB(home).inflate(12), e -> suitable(e, level) && state.development().town(camp.id()).claims.contains(ChunkKey.fromBlock(dimension(level), e.blockPosition().getX(), e.blockPosition().getZ())));
            target = candidates.stream().min(Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);
        }
        var goal = brain.choose(profile, new UtilityBrain.Observation(sunny, mob.getHealth() < mob.getMaxHealth() * .25,
                !citizen.cargo().isEmpty(), target != null, distance, citizen.diligence()));
        goals.put(mob.getUUID(), goal);
        switch (goal) {
            case SHELTER, REGROUP -> mob.getNavigation().moveTo(home.getX() + .5, home.getY(), home.getZ() + .5, 1.0);
            case DELIVER -> {
                if (distance < 2.5) { state.deliver(leases.get(mob.getUUID())); mob.getNavigation().stop(); }
                else mob.getNavigation().moveTo(home.getX() + .5, home.getY(), home.getZ() + .5, 1.0);
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
