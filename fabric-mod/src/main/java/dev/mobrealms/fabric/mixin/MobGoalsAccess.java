package dev.mobrealms.fabric.mixin;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Mob.class)
public interface MobGoalsAccess {
    @Accessor("goalSelector") GoalSelector mobrealms$goals();
    @Accessor("targetSelector") GoalSelector mobrealms$targets();
}
