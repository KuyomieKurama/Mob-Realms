package dev.mobrealms.fabric.mixin;

import dev.mobrealms.fabric.MobRealms;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Enderman;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Suppress ambient daylight teleportation during civilization work. Damage reactions remain vanilla. */
@Mixin(Enderman.class)
public abstract class EndermanCivilizationAI {
    @Inject(method="customServerAiStep",at=@At("HEAD"),cancellable=true)
    private void mobrealms$ownedDaylight(ServerLevel level,CallbackInfo ci){
        var controller=MobRealms.controller(level.getServer());
        if(controller!=null&&controller.state().hasCitizen(((Enderman)(Object)this).getUUID()))ci.cancel();
    }
}
