package dev.mobrealms.fabric;

import dev.mobrealms.core.*;
import dev.mobrealms.core.Development.*;
import net.minecraft.commands.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.*;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import java.util.*;

/** Player mutations always re-check ownership, inventory, proximity and treaty rules server-side. */
public final class NationCommands {
    private static final Map<UUID,Long> lastAction=new HashMap<>();
    private NationCommands(){}
    public static void clearSession(){lastAction.clear();}
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher){
        dispatcher.register(Commands.literal("realm")
            .executes(ctx->RealmDashboard.open(ctx.getSource(),MobRealms.controller(ctx.getSource().getServer()),null,0))
            .then(Commands.literal("page").then(Commands.argument("page",IntegerArgumentType.integer(0,127)).executes(ctx->RealmDashboard.open(ctx.getSource(),MobRealms.controller(ctx.getSource().getServer()),null,IntegerArgumentType.getInteger(ctx,"page")))))
            .then(Commands.literal("view").then(Commands.argument("id",StringArgumentType.word()).executes(ctx->{
                try{return RealmDashboard.open(ctx.getSource(),MobRealms.controller(ctx.getSource().getServer()),UUID.fromString(StringArgumentType.getString(ctx,"id")),0);}catch(IllegalArgumentException ex){return fail(ctx.getSource());}
            })))
            .then(Commands.literal("action").then(Commands.argument("arguments",StringArgumentType.greedyString()).executes(ctx->action(ctx.getSource(),StringArgumentType.getString(ctx,"arguments")))))
        );
    }
    private static int fail(CommandSourceStack source){source.sendFailure(Component.translatable("realm.mobrealms.failed"));return 0;}
    private static boolean pay(ServerPlayer player,Item item,int count){
        var items=player.getInventory().getNonEquipmentItems();int available=items.stream().filter(s->s.is(item)).mapToInt(ItemStack::getCount).sum();if(available<count)return false;
        for(var stack:items)if(stack.is(item)){int take=Math.min(count,stack.getCount());stack.shrink(take);count-=take;if(count==0)break;}return true;
    }
    public static boolean found(ServerPlayer player,RealmController c,BlockPos pos,ItemStack banner){
        if(!c.healthy()||!banner.is(FoundingBannerItem.ITEM)||c.state().development().nation(player.getUUID()).isPresent())return false;
        if(!c.found(player.level(),pos,"mobrealms:human"))return false;
        var camp=c.state().camps().getLast();c.state().development().town(camp.id()).owner=player.getUUID();banner.shrink(1);c.save();
        player.sendSystemMessage(Component.translatable("realm.mobrealms.founded"));return true;
    }
    private static int action(CommandSourceStack source,String arguments)throws com.mojang.brigadier.exceptions.CommandSyntaxException{
        var player=source.getPlayerOrException();var c=MobRealms.controller(source.getServer());if(c==null||!c.healthy())return fail(source);
        long now=source.getServer().overworld().getGameTime();if(now-lastAction.getOrDefault(player.getUUID(),-100L)<4)return fail(source);lastAction.put(player.getUUID(),now);
        String[] args=arguments.split(" ");if(args.length>3||args.length<2)return fail(source);
        var s=c.state();UUID target;try{target=UUID.fromString(args[1]);}catch(IllegalArgumentException ex){return fail(source);}
        UUID own=s.development().nation(player.getUUID()).orElse(null);UUID show=target;boolean ok=false;
        try{
            switch(args[0]){
                case "donate"->{s.camp(target);var stack=player.getMainHandItem();if(!stack.isEmpty()&&!stack.is(FoundingBannerItem.ITEM)){String item=BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();int amount=Math.min(64,stack.getCount());if(s.stock(target).getOrDefault(item,0L)<1000000){s.credit(target,item,amount);stack.shrink(amount);ok=true;}}}
                case "gift"->{var town=s.development().town(target);if(pay(player,Items.EMERALD,4)){town.reputation(player.getUUID(),12);s.credit(target,"minecraft:emerald",4);if(own!=null&&!own.equals(target))s.development().relation(own,target).adjust(12);ok=true;}}
                case "buy"->{var town=s.development().town(target);if((own==null||own.equals(target)||s.development().relation(own,target).treaty!=Treaty.WAR)&&town.reputation.getOrDefault(player.getUUID(),0)>-20&&s.stock(target).getOrDefault("minecraft:bread",0L)>=4&&pay(player,Items.EMERALD,1)){s.consume(target,Map.of("minecraft:bread",4L));s.credit(target,"minecraft:emerald",1);var bread=new ItemStack(Items.BREAD,4);if(!player.getInventory().add(bread))player.spawnAtLocation(player.level(),bread);town.reputation(player.getUUID(),1);ok=true;}}
                case "treaty"->{if(own!=null&&args.length==3){ok=s.development().propose(own,target,Treaty.valueOf(args[2].toUpperCase(Locale.ROOT)));if(ok)s.development().event("diplomacy",own,s.day());}}
                case "accept","decline"->{if(own!=null)ok=s.development().answer(own,target,args[0].equals("accept"),s.day());}
                case "barter"->{if(own!=null)ok=s.development().trade(s,own,target,"minecraft:oak_planks","minecraft:cobblestone",4);}
                case "claim"->{if(own!=null&&own.equals(target)){var p=player.blockPosition();var chunk=ChunkKey.fromBlock(RealmController.dimension(player.level()),p.getX(),p.getZ());if(s.development().claim(own,chunk,s.protectedChunks()))ok=true;}}
                case "role"->{if(args.length==3&&s.hasCitizen(target)){var citizen=s.citizen(target);if(player.getUUID().equals(s.development().town(citizen.camp()).owner)){s.development().person(target).role=Role.valueOf(args[2].toUpperCase(Locale.ROOT));show=citizen.camp();ok=true;}}}
                case "recruit"->{if(own!=null&&s.hasCitizen(target)){var citizen=s.citizen(target);var town=s.development().town(citizen.camp());var mob=player.level().getEntity(target);
                    if(!own.equals(citizen.camp())&&town.owner==null&&mob!=null&&mob.distanceToSqr(player)<=256&&s.population(own)<s.development().town(own).housing()
                        &&(town.reputation.getOrDefault(player.getUUID(),0)>=20||town.starvation>=2)&&pay(player,Items.EMERALD,8)){
                        s.credit(citizen.camp(),"minecraft:emerald",8);s.recruit(target,own);s.development().person(target).role=Role.GATHERER;town.reputation(player.getUUID(),-5);show=own;ok=true;
                    }
                }}
                default->{return fail(source);}
            }
        }catch(IllegalArgumentException|NullPointerException ex){return fail(source);}
        if(!ok)return fail(source);c.save();source.sendSuccess(()->Component.translatable("realm.mobrealms.success"),false);
        return RealmDashboard.open(source,c,show,0);
    }
}
