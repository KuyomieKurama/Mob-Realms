package dev.mobrealms.fabric;

import dev.mobrealms.core.*;
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
    private long ticks;
    private int pendingDays;
    private boolean healthy = true;
    private static final Set<String> MATERIALS = Set.of("minecraft:rotten_flesh", "minecraft:bone", "minecraft:arrow",
            "minecraft:stick", "minecraft:cobblestone", "minecraft:oak_log", "minecraft:coal", "minecraft:iron_ingot");

    public RealmController(MinecraftServer server, RealmConfig config) throws IOException {
        this.server = server; this.config = config;
        savePath = server.getWorldPath(LevelResource.ROOT).resolve("mobrealms/realms.dat");
        state = Files.exists(savePath) ? RealmStore.load(savePath)
                : new RealmSimulation(config.maxCamps(), config.maxPopulation(), config.maxDetailed());
        definitions.reload(server.getResourceManager());
        for (var camp : state.camps()) definitions.get(camp.species());
    }
    public RealmSimulation state() { return state; }
    public List<String> species() { return definitions.ids(); }
    public String goal(UUID id) { return goals.getOrDefault(id, UtilityBrain.Goal.IDLE).name().toLowerCase(Locale.ROOT); }
    public int pendingDays() { return pendingDays; }
    public void reloadDefinitions() {
        // A removed profile cannot invalidate living citizens: reject the whole replacement.
        try {
            SpeciesDefinitions next = new SpeciesDefinitions(); next.reload(server.getResourceManager());
            for (var camp : state.camps()) next.get(camp.species());
            definitions.reload(server.getResourceManager());
        } catch (Exception ex) { MobRealms.LOGGER.error("Keeping previous species definitions after invalid reload", ex); }
    }
    public void loadEntity(Entity entity) {
        if (!(entity instanceof Mob mob) || !state.hasCitizen(entity.getUUID()) || loaded.containsKey(entity.getUUID())) return;
        loaded.put(mob.getUUID(), mob);
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
        if (state.hasCitizen(id) && entity instanceof LivingEntity living && living.isDeadOrDying()) state.removeCitizen(id);
    }
    public void save() {
        if (!healthy) return;
        try { RealmStore.save(savePath, state); }
        catch (IOException ex) { healthy = false; MobRealms.LOGGER.error("Mob Realms halted after save failure; previous save retained", ex); }
    }
    public boolean healthy() { return healthy; }
    public boolean enqueueDays(int days) {
        if (!healthy || days < 1 || days > 7 || pendingDays + days > 7) return false;
        pendingDays += days; return true;
    }
    public void tick() {
        if (!healthy) return;
        ticks++;
        if (ticks % 20 == 0) {
            int passed = state.observeWorldDay(Math.max(0, Math.floorDiv(server.overworld().getDayTime(), 24000L)), 7);
            pendingDays = Math.min(7, pendingDays + passed);
        }
        if (pendingDays > 0 && scheduler.submit(() -> state.advanceDay())) pendingDays--;
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
        scheduler.run(config.budgetNanos(), config.workPerTick());
        if (config.naturalCamps() && ticks % 1200 == 0 && state.camps().size() < state.maxCamps()
                && server.overworld().getDayTime() >= config.graceDays() * 24000L) naturalCamp();
    }
    private void update(Mob mob) {
        var citizen = state.citizen(mob.getUUID()); var camp = state.camp(citizen.camp());
        ServerLevel level = (ServerLevel) mob.level();
        if (!dimension(level).equals(camp.territory().dimension())) { mob.getNavigation().stop(); return; }
        var profile = definitions.get(camp.species());
        BlockPos home = new BlockPos(camp.x(), camp.y(), camp.z());
        double distance = Math.sqrt(mob.distanceToSqr(home.getX() + .5, home.getY(), home.getZ() + .5));
        boolean sunny = Math.floorMod(level.getDayTime(), 24000L) < 12000 && level.canSeeSky(mob.blockPosition()) && !level.isRaining();
        ItemEntity target = null;
        if (!sunny || !profile.avoidsSun()) {
            var candidates = level.getEntitiesOfClass(ItemEntity.class, new AABB(home).inflate(12), e -> suitable(e, level));
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
            case IDLE -> mob.getNavigation().stop();
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
        if (!healthy) return false;
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
            for (int dy = 0; dy <= 3; dy++) {
                BlockPos p = origin.offset(dx, dy, dz);
                if (!chunk.equals(ChunkKey.fromBlock(dimension(level), p.getX(), p.getZ()))
                        || !level.hasChunkAt(p) || !level.getWorldBorder().isWithinBounds(p) || !level.isEmptyBlock(p)) return false;
            }
            roof.add(origin.offset(dx, 3, dz));
        }
        var type = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.parse(profile.entityType()));
        if (type == null) return false;
        List<Mob> residents = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Entity entity = type.create(level, EntitySpawnReason.EVENT);
            if (!(entity instanceof Mob mob)) return false;
            mob.setPos(origin.getX() + .5 + (i - 1) * .6, origin.getY(), origin.getZ() + .5);
            mob.setPersistenceRequired(); residents.add(mob);
        }
        for (var p : roof) level.setBlock(p, Blocks.COBBLESTONE.defaultBlockState(), 3);
        BlockPos marker = origin.offset(1, 0, 1);
        level.setBlock(marker, (species.endsWith("skeleton") ? Blocks.BLUE_BANNER : Blocks.GREEN_BANNER).defaultBlockState(), 3);
        UUID campId = UUID.randomUUID();
        state.found(new RealmSimulation.Camp(campId, species, chunk, origin.getX(), origin.getY(), origin.getZ()));
        for (Mob mob : residents) {
            state.addCitizen(mob.getUUID(), campId, .35 + (Math.floorMod(mob.getUUID().hashCode(), 60) / 100.0));
            mob.setCustomName(Component.translatable("entity.mobrealms.citizen", Component.translatable("species." + species.replace(':', '.'))));
            if (!level.addFreshEntity(mob)) state.removeCitizen(mob.getUUID());
            else loadEntity(mob);
        }
        save(); return true;
    }
    private void naturalCamp() {
        var players = server.getPlayerList().getPlayers(); if (players.isEmpty()) return;
        var random = server.overworld().getRandom();
        if (random.nextInt(8) != 0) return;
        var player = players.get(random.nextInt(players.size())); ServerLevel level = (ServerLevel) player.level();
        int x = player.blockPosition().getX() + random.nextInt(129) - 64;
        int z = player.blockPosition().getZ() + random.nextInt(129) - 64;
        BlockPos candidate = new BlockPos(x, player.blockPosition().getY(), z);
        if (!level.hasChunkAt(candidate) || Math.hypot(x - player.getX(), z - player.getZ()) < 32) return;
        BlockPos surface = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, candidate);
        var ids = definitions.ids(); found(level, surface, ids.get(random.nextInt(ids.size())));
    }
    public static String dimension(ServerLevel level) { return level.dimension().identifier().toString(); }
}
