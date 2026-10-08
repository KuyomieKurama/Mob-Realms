package dev.mobrealms.fabric.mixin;

import dev.mobrealms.fabric.MobRealms;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Piglins have a Brain in addition to goals; only realm members relinquish that vanilla controller. */
@Mixin(Piglin.class)
public abstract class PiglinCivilizationAI {
    @Inject(method="customServerAiStep",at=@At("HEAD"),cancellable=true)
    private void mobrealms$ownedBrain(ServerLevel level,CallbackInfo ci){
        var controller=MobRealms.controller(level.getServer());
        if(controller!=null&&controller.state().hasCitizen(((Piglin)(Object)this).getUUID()))ci.cancel();
    }
}
