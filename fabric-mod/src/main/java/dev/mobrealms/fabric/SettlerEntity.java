package dev.mobrealms.fabric;

import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.level.Level;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.*;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;

/** A real pathfinding human NPC, separate from vanilla villagers and player accounts. */
public final class SettlerEntity extends PathfinderMob {
    private static final ResourceKey<EntityType<?>> KEY=ResourceKey.create(Registries.ENTITY_TYPE,Identifier.parse("mobrealms:settler"));
    public static final EntityType<SettlerEntity> TYPE=Registry.register(BuiltInRegistries.ENTITY_TYPE,KEY,
        EntityType.Builder.<SettlerEntity>of(SettlerEntity::new,MobCategory.CREATURE).sized(.6f,1.8f).build(KEY));
    public SettlerEntity(EntityType<? extends SettlerEntity> type,Level level){super(type,level);}
    public static void register(){FabricDefaultAttributeRegistry.register(TYPE,Mob.createMobAttributes()
        .add(Attributes.MAX_HEALTH,20).add(Attributes.MOVEMENT_SPEED,.25).add(Attributes.ATTACK_DAMAGE,3));}
}
