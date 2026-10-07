package dev.mobrealms.fabric;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.mobrealms.core.ChunkKey;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.permissions.Permissions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

public final class MobRealms implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("mobrealms");
    private final Map<MinecraftServer, RealmController> controllers = new IdentityHashMap<>();
    @Override public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            try {
                var config = RealmConfig.load(FabricLoader.getInstance().getConfigDir().resolve("mobrealms.properties"));
                var controller = new RealmController(server, config); controllers.put(server, controller);
                for (var level : server.getAllLevels()) for (var entity : level.getAllEntities()) controller.loadEntity(entity);
                LOGGER.info("Mob Realms loaded {} camps", controller.state().camps().size());
            } catch (Exception ex) { throw new IllegalStateException("Cannot safely load Mob Realms world data", ex); }
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            var c = controllers.get(server); if (c != null) c.tick();
        });
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            var c = controllers.get(level.getServer()); if (c != null) c.loadEntity(entity);
        });
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
            var c = controllers.get(level.getServer()); if (c != null) c.unloadEntity(entity);
        });
        ServerLifecycleEvents.BEFORE_SAVE.register((server, flush, force) -> {
            var c = controllers.get(server); if (c != null) c.save();
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            var c = controllers.get(server); if (c != null) c.save();
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(controllers::remove);
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, manager, success) -> {
            var c = controllers.get(server); if (success && c != null) c.reloadDefinitions();
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, context, selection) -> dispatcher.register(
            Commands.literal("civ").requires(s -> s.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .then(Commands.literal("admin").executes(ctx -> AdminDialog.open(ctx.getSource(), require(ctx.getSource()), 0))
                    .then(Commands.literal("page").then(Commands.argument("page", IntegerArgumentType.integer(0, 204))
                        .executes(ctx -> AdminDialog.open(ctx.getSource(), require(ctx.getSource()), IntegerArgumentType.getInteger(ctx, "page")))))
                    .then(Commands.literal("simulate").then(Commands.argument("days", IntegerArgumentType.integer(1, dev.mobrealms.core.RealmSimulation.MAX_PENDING_DAYS))
                        .executes(ctx -> {
                            var source = ctx.getSource(); source.getPlayerOrException(); var c = require(source);
                            int days = IntegerArgumentType.getInteger(ctx, "days");
                            message(source, c.enqueueDays(days) ? "queued" : "busy", days);
                            return AdminDialog.open(source, c, 0);
                        })))
                    .then(Commands.literal("cancel").executes(ctx -> {
                        var source = ctx.getSource(); source.getPlayerOrException(); var c = require(source);
                        message(source, "cancelled", c.cancelDays()); return AdminDialog.open(source, c, 0);
                    }))
                    .then(Commands.literal("protect").executes(ctx -> {
                        var source = ctx.getSource(); source.getPlayerOrException(); protection(source, true);
                        return AdminDialog.open(source, require(source), 0);
                    })))
                .then(Commands.literal("speed").then(Commands.argument("multiplier", IntegerArgumentType.integer(1, 5)).executes(ctx -> {
                    var source = ctx.getSource(); int multiplier = IntegerArgumentType.getInteger(ctx, "multiplier");
                    source.getServer().getCommands().performPrefixedCommand(source, "tick rate " + (20 * multiplier));
                    message(source, "speed_scope"); return 1;
                })))
                .then(Commands.literal("observe").executes(ctx -> {
                    var source = ctx.getSource(); source.getPlayerOrException();
                    source.getServer().getCommands().performPrefixedCommand(source, "gamemode spectator @s");
                    message(source, "observe"); return 1;
                }).then(Commands.literal("creative").executes(ctx -> {
                    var source = ctx.getSource(); source.getPlayerOrException();
                    source.getServer().getCommands().performPrefixedCommand(source, "gamemode creative @s"); return 1;
                })).then(Commands.literal("survival").executes(ctx -> {
                    var source = ctx.getSource(); source.getPlayerOrException();
                    source.getServer().getCommands().performPrefixedCommand(source, "gamemode survival @s"); return 1;
                })))
                .then(Commands.literal("info").executes(ctx -> info(ctx.getSource())))
                .then(Commands.literal("relations").executes(ctx -> {
                    var c = require(ctx.getSource());
                    for (var a : c.state().camps()) for (var b : c.state().camps())
                        if (a.id().compareTo(b.id()) < 0) message(ctx.getSource(), "relations", a.id().toString(), b.id().toString());
                    if (c.state().camps().size() < 2) message(ctx.getSource(), "no_relations");
                    return 1;
                }))
                .then(Commands.literal("simulate").then(Commands.literal("cancel").executes(ctx -> {
                    message(ctx.getSource(), "cancelled", require(ctx.getSource()).cancelDays()); return 1;
                })).then(Commands.argument("days", IntegerArgumentType.integer(1, dev.mobrealms.core.RealmSimulation.MAX_PENDING_DAYS)).executes(ctx -> {
                    int days = IntegerArgumentType.getInteger(ctx, "days");
                    boolean ok = require(ctx.getSource()).enqueueDays(days);
                    message(ctx.getSource(), ok ? "queued" : "busy", days); return ok ? 1 : 0;
                })))
                .then(Commands.literal("found").then(Commands.argument("species", StringArgumentType.greedyString())
                    .suggests((ctx, builder) -> { require(ctx.getSource()).species().forEach(builder::suggest); return builder.buildFuture(); })
                    .executes(ctx -> {
                        var source = ctx.getSource(); var c = require(source); String species = StringArgumentType.getString(ctx, "species");
                        if (!c.species().contains(species)) { message(source, "unknown_species"); return 0; }
                        boolean ok = c.found(source.getLevel(), BlockPos.containing(source.getPosition()), species);
                        message(source, ok ? "founded" : "site_blocked"); return ok ? 1 : 0;
                    })))
                .then(Commands.literal("protect").executes(ctx -> protection(ctx.getSource(), true)))
                .then(Commands.literal("unprotect").executes(ctx -> protection(ctx.getSource(), false)))
                .then(Commands.literal("goals").executes(ctx -> {
                    var c = require(ctx.getSource());
                    for (var resident : c.state().citizens()) message(ctx.getSource(), "goal", resident.id().toString(),
                            Component.translatable("goal.mobrealms." + c.goal(resident.id())));
                    return 1;
                }))
        ));
    }
    private RealmController require(CommandSourceStack source) {
        return Objects.requireNonNull(controllers.get(source.getServer()), "Mob Realms not started");
    }
    private int info(CommandSourceStack source) {
        var c = require(source);
        message(source, "summary", c.state().camps().size(), c.state().citizens().size(), c.state().day(), c.pendingDays());
        if (!c.healthy()) message(source, "halted");
        for (var camp : c.state().camps()) {
            message(source, "camp", camp.id().toString(), Component.translatable("species." + camp.species().replace(':', '.')),
                    camp.territory().dimension(), camp.x(), camp.y(), camp.z());
            c.state().stock(camp.id()).forEach((item, count) -> message(source, "stock", item, count));
        }
        return 1;
    }
    private int protection(CommandSourceStack source, boolean protect) {
        var c = require(source); var pos = BlockPos.containing(source.getPosition());
        var chunk = ChunkKey.fromBlock(RealmController.dimension(source.getLevel()), pos.getX(), pos.getZ());
        if (protect) c.state().protect(chunk); else c.state().unprotect(chunk);
        c.save(); message(source, protect ? "protected" : "unprotected", chunk.x(), chunk.z()); return 1;
    }
    private static void message(CommandSourceStack source, String key, Object... args) {
        source.sendSuccess(() -> Component.translatable("commands.mobrealms." + key, args), false);
    }
}
