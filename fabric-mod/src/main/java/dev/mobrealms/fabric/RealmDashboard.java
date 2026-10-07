package dev.mobrealms.fabric;

import com.google.gson.*;
import dev.mobrealms.core.*;
import dev.mobrealms.core.Development.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.permissions.Permissions;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.*;

public final class RealmDashboard {
    private RealmDashboard(){}
    public static int open(CommandSourceStack source,RealmController c,UUID selected,int page)throws CommandSyntaxException{
        var player=source.getPlayerOrException();var s=c.state();var root=new JsonObject();
        root.addProperty("day",s.day());root.addProperty("queued",s.pendingDays());root.addProperty("admin",source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER));
        root.addProperty("player",player.getUUID().toString());
        var camps=s.camps();if(selected!=null)for(int i=0;i<camps.size();i++)if(camps.get(i).id().equals(selected)){page=i/8;break;}int pages=Math.max(1,(camps.size()+7)/8);page=Math.min(Math.max(page,0),pages-1);root.addProperty("page",page);root.addProperty("pages",pages);
        var own=s.development().nation(player.getUUID());root.addProperty("own",own.map(UUID::toString).orElse(""));
        UUID requested=selected;
        if(selected==null||camps.stream().noneMatch(x->x.id().equals(requested)))selected=camps.isEmpty()?null:camps.get(page*8).id();
        root.addProperty("selected",selected==null?"":selected.toString());
        var list=new JsonArray();
        for(int i=page*8;i<Math.min(camps.size(),page*8+8);i++){
            var camp=camps.get(i);var t=s.development().town(camp.id());var j=new JsonObject();
            j.addProperty("id",camp.id().toString());j.addProperty("species",camp.species());j.addProperty("x",camp.x());j.addProperty("z",camp.z());j.addProperty("y",camp.y());j.addProperty("dimension",camp.territory().dimension());j.addProperty("population",s.population(camp.id()));j.addProperty("stage",t.stage(s.population(camp.id())));
            var claims=new JsonArray();for(var chunk:t.claims){var a=new JsonArray();a.add(chunk.x());a.add(chunk.z());claims.add(a);}j.add("claims",claims);list.add(j);
        }
        root.add("towns",list);
        if(selected!=null){var camp=s.camp(selected);var t=s.development().town(selected);var detail=new JsonObject();detail.addProperty("dimension",camp.territory().dimension());detail.addProperty("species",camp.species());detail.addProperty("population",s.population(selected));detail.addProperty("housing",t.housing());detail.addProperty("owner",t.owner==null?"":t.owner.toString());detail.addProperty("strategy",t.strategy.name().toLowerCase(Locale.ROOT));detail.addProperty("obstacle",t.obstacle);detail.addProperty("objective",(t.project==null?t.objective(s.population(selected)):t.project.building).name().toLowerCase(Locale.ROOT));detail.addProperty("progress",t.project==null?0:t.project.paid);detail.addProperty("total",t.project==null?1:t.project.tiles.size());detail.addProperty("research",t.research);detail.addProperty("ranged",t.rangedThreat);detail.addProperty("reputation",t.reputation.getOrDefault(player.getUUID(),0));
            detail.add("stock",new Gson().toJsonTree(s.stock(selected)));var buildings=new JsonObject();for(var b:Building.values())buildings.addProperty(b.name().toLowerCase(Locale.ROOT),t.count(b));detail.add("buildings",buildings);
            var tech=new JsonArray();for(var x:t.technologies)tech.add(x.name().toLowerCase(Locale.ROOT));detail.add("technologies",tech);var weights=new JsonArray();for(double w:t.rewards)weights.add(w);detail.add("weights",weights);
            if(own.isPresent()&&!own.get().equals(selected)){var r=s.development().relation(own.get(),selected);detail.addProperty("relation",r.score);detail.addProperty("treaty",r.treaty.name().toLowerCase(Locale.ROOT));detail.addProperty("offer",r.offer==null?"":r.offer.name().toLowerCase(Locale.ROOT));}else{detail.addProperty("relation",0);detail.addProperty("treaty","neutral");}
            var people=new JsonArray();for(var citizen:s.residents(selected)){if(people.size()>=256)continue;var p=s.development().person(citizen.id());var j=new JsonObject();j.addProperty("id",citizen.id().toString());j.addProperty("role",p.role.name().toLowerCase(Locale.ROOT));j.addProperty("rank",p.rank());j.addProperty("xp",p.experience);j.addProperty("goal",c.goal(citizen.id()));people.add(j);}detail.add("people",people);root.add("detail",detail);
        }
        ServerPlayNetworking.send(player,new RealmPayload(root.toString()));return 1;
    }
}
