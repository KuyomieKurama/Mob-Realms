package dev.mobrealms.fabric;

import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.InteractionResult;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;

public final class FoundingBannerItem extends Item {
    private static final ResourceKey<Item> KEY=ResourceKey.create(Registries.ITEM,Identifier.parse("mobrealms:founding_banner"));
    public static final Item ITEM=Registry.register(BuiltInRegistries.ITEM,KEY,new FoundingBannerItem());
    private FoundingBannerItem(){super(new Item.Properties().setId(KEY).stacksTo(1));}
    public static void register(){}
    @Override public InteractionResult useOn(UseOnContext context){
        if(context.getLevel().isClientSide())return InteractionResult.SUCCESS;
        if(!(context.getPlayer() instanceof ServerPlayer player))return InteractionResult.PASS;
        var c=MobRealms.controller(player.level().getServer());if(c==null)return InteractionResult.FAIL;
        return NationCommands.found(player,c,context.getClickedPos().above(),context.getItemInHand())?InteractionResult.SUCCESS:InteractionResult.FAIL;
    }
}
