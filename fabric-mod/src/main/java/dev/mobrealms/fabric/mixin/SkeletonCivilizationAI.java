package dev.mobrealms.fabric.mixin;

import dev.mobrealms.fabric.MobRealms;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Equipping an item must not reinsert vanilla movement/attack goals after realm ownership. */
@Mixin(AbstractSkeleton.class)
public abstract class SkeletonCivilizationAI {
    @Inject(method="reassessWeaponGoal",at=@At("HEAD"),cancellable=true)
    private void mobrealms$ownedWeapons(CallbackInfo ci){
        var mob=(AbstractSkeleton)(Object)this;
        if(mob.level() instanceof ServerLevel level){
            var controller=MobRealms.controller(level.getServer());
            if(controller!=null&&controller.state().hasCitizen(mob.getUUID()))ci.cancel();
        }
    }
}
