package dev.mobrealms.fabric;

import java.util.*;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permissions;

/** Explicit operator teleport: bounded search, including the destination dimension. */
final class SettlementTeleport {
    static int teleport(CommandSourceStack source, RealmController controller, UUID id) throws CommandSyntaxException {
        if(!source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))return 0;
        var player=source.getPlayerOrException();
        var camp=controller.state().camps().stream().filter(c->c.id().equals(id)).findFirst().orElse(null);
        if(camp!=null)for(var level:source.getServer().getAllLevels())if(RealmController.dimension(level).equals(camp.territory().dimension())){
            for(int radius=0;radius<=8;radius++)for(int dx=-radius;dx<=radius;dx++)for(int dz=-radius;dz<=radius;dz++){
                if(Math.max(Math.abs(dx),Math.abs(dz))!=radius)continue;
                for(int dy=0;dy<=8;dy++)for(int sign:new int[]{1,-1}){
                    var pos=new BlockPos(camp.x()+dx,camp.y()+dy*sign,camp.z()+dz);
                    if(!level.getWorldBorder().isWithinBounds(pos)||level.isOutsideBuildHeight(pos)||level.isOutsideBuildHeight(pos.above()))continue;
                    level.getChunkAt(pos);
                    if(!level.getBlockState(pos.below()).isFaceSturdy(level,pos.below(),net.minecraft.core.Direction.UP))continue;
                    if(!level.getBlockState(pos).isAir()||!level.getBlockState(pos.above()).isAir())continue;
                    var floor=level.getBlockState(pos.below());
                    if(floor.is(net.minecraft.world.level.block.Blocks.MAGMA_BLOCK)||floor.is(net.minecraft.world.level.block.Blocks.CAMPFIRE)||floor.is(net.minecraft.world.level.block.Blocks.SOUL_CAMPFIRE))continue;
                    double x=pos.getX()+.5,y=pos.getY(),z=pos.getZ()+.5;
                    if(!level.noCollision(player,player.getBoundingBox().move(x-player.getX(),y-player.getY(),z-player.getZ())))continue;
                    if(player.teleportTo(level,x,y,z,Set.of(),player.getYRot(),player.getXRot(),true))return 1;
                    break;
                }
            }
        }
        source.sendFailure(Component.translatable("commands.mobrealms.teleport_failed"));return 0;
    }
}
