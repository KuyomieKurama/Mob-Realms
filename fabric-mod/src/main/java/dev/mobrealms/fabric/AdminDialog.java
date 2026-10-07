package dev.mobrealms.fabric;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.commands.CommandSourceStack;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

/** Vanilla dialog renderer; every button runs a permission-checked server command as the player. */
public final class AdminDialog {
    private AdminDialog() {}
    private static JsonObject text(String key, Object... args) {
        var value = new JsonObject(); value.addProperty("translate", key);
        if (args.length > 0) {
            var values = new JsonArray();
            for (Object arg : args) {
                if (arg instanceof JsonObject object) values.add(object);
                else values.add(String.valueOf(arg));
            }
            value.add("with", values);
        }
        return value;
    }
    private static void message(JsonArray body, JsonObject contents) {
        var line = new JsonObject(); line.addProperty("type", "minecraft:plain_message");
        line.add("contents", contents); line.addProperty("width", 420); body.add(line);
    }
    private static JsonObject button(String key, String command) {
        var button = new JsonObject(); button.add("label", text("gui.mobrealms." + key));
        button.addProperty("width", 200);
        var action = new JsonObject(); action.addProperty("type", "minecraft:run_command");
        action.addProperty("command", command); button.add("action", action); return button;
    }
    public static int open(CommandSourceStack source, RealmController controller, int requestedPage) throws CommandSyntaxException {
        source.getPlayerOrException();
        var camps = controller.state().camps();
        int pages = Math.max(1, (camps.size() + 4) / 5), page = Math.min(Math.max(0, requestedPage), pages - 1);
        var dialog = new JsonObject();
        dialog.addProperty("type", "minecraft:multi_action");
        dialog.add("title", text("gui.mobrealms.title"));
        dialog.addProperty("pause", false); dialog.addProperty("after_action", "close");
        dialog.addProperty("columns", 2);
        var body = new JsonArray();
        message(body, text("commands.mobrealms.summary", camps.size(), controller.state().citizens().size(), controller.state().day(), controller.pendingDays()));
        message(body, text("gui.mobrealms.natural", text("gui.mobrealms." + (controller.config().naturalCamps() ? "enabled" : "disabled")), controller.config().graceDays()));
        message(body, text("gui.mobrealms.limitations"));
        message(body, text("commands.mobrealms.speed_scope"));
        if (!controller.healthy()) message(body, text("commands.mobrealms.halted"));
        message(body, text("gui.mobrealms.page", page + 1, pages));
        for (int i = page * 5; i < Math.min(camps.size(), page * 5 + 5); i++) {
            var camp = camps.get(i);
            message(body, text("gui.mobrealms.camp", text("species." + camp.species().replace(':', '.')), camp.x(), camp.y(), camp.z(),
                    controller.state().stock(camp.id()).values().stream().mapToLong(Long::longValue).sum()));
        }
        dialog.add("body", body);
        var inputs = new JsonArray(); var days = new JsonObject();
        days.addProperty("type", "minecraft:text"); days.addProperty("key", "days");
        days.add("label", text("gui.mobrealms.days")); days.addProperty("initial", "7");
        days.addProperty("max_length", 3); inputs.add(days); dialog.add("inputs", inputs);
        var actions = new JsonArray();
        var simulate = button("simulate", "civ admin simulate 7");
        var dynamic = new JsonObject(); dynamic.addProperty("type", "minecraft:dynamic/run_command");
        dynamic.addProperty("template", "civ admin simulate $(days)"); simulate.add("action", dynamic); actions.add(simulate);
        actions.add(button("cancel", "civ admin cancel"));
        actions.add(button("observe", "civ observe"));
        actions.add(button("creative", "civ observe creative"));
        actions.add(button("speed_normal", "civ speed 1"));
        actions.add(button("speed_fast", "civ speed 5"));
        actions.add(button("refresh", "civ admin page " + page));
        actions.add(button("protect", "civ admin protect"));
        if (page > 0) actions.add(button("previous", "civ admin page " + (page - 1)));
        if (page + 1 < pages) actions.add(button("next", "civ admin page " + (page + 1)));
        dialog.add("actions", actions);
        var exit = new JsonObject(); exit.add("label", text("gui.mobrealms.close")); dialog.add("exit_action", exit);
        // Parse the generated JSON through Minecraft's own dialog command/registry codec.
        // The original source permissions are retained; no elevated command source is created.
        source.getServer().getCommands().performPrefixedCommand(source, "dialog show @s " + dialog);
        return 1;
    }
}
