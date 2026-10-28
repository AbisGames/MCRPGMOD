package com.pipican.gametest;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.levelgen.structure.PoolElementStructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;

/** Actual two-block player clearance from a cave doorway to open terrain.
 * Walking, a one-block step, swimming and ladder climbing are supported.
 */
final class ExpeditionAccessTests {
    private ExpeditionAccessTests() {}
    static boolean reachesOutside(ServerLevel level,StructureStart start) {
        for(var piece:start.getPieces()){
            if(!(piece instanceof PoolElementStructurePiece pool))continue;
            var origin=pool.getPosition();
            var rotation=pool.getRotation();
            var entry=new BlockPos(40,1,1).rotate(rotation).offset(origin);
            var outward=rotation.rotate(Direction.NORTH);
            for(int cx=Math.floorDiv(entry.getX()-18,16);cx<=Math.floorDiv(entry.getX()+18,16);cx++)
                for(int cz=Math.floorDiv(entry.getZ()-18,16);cz<=Math.floorDiv(entry.getZ()+18,16);cz++)level.getChunk(cx,cz);
            var queue=new ArrayDeque<BlockPos>();
            var seen=new HashSet<BlockPos>();
            for(int x=36;x<=44;x++){
                var pos=new BlockPos(x,1,3).rotate(rotation).offset(origin);
                if(standing(level,pos)&&seen.add(pos))queue.add(pos);
            }
            while(!queue.isEmpty()&&seen.size()<100000){
                var pos=queue.removeFirst();
                int forward=(pos.getX()-entry.getX())*outward.getStepX()
                    +(pos.getZ()-entry.getZ())*outward.getStepZ();
                // Open surface beyond the template, with breathing room.
                if(forward>=8&&level.canSeeSky(pos.above())&&level.getFluidState(pos.above()).isEmpty())return true;
                for(var direction:Direction.Plane.HORIZONTAL)for(int dy=-1;dy<=1;dy++){
                    var next=pos.relative(direction).above(dy);
                    if(dy==1&&!clear(level,pos.above(2)))continue;
                    if(dy==-1&&!clear(level,next.above(2)))continue;
                    enqueue(level,entry,next,queue,seen);
                }
                if(climbable(level,pos)){
                    enqueue(level,entry,pos.above(),queue,seen);
                    enqueue(level,entry,pos.below(),queue,seen);
                }
            }
        }
        return false;
    }
    private static void enqueue(ServerLevel level,BlockPos entry,BlockPos pos,
            ArrayDeque<BlockPos> queue,Set<BlockPos> seen){
        if(Math.abs(pos.getX()-entry.getX())>16||Math.abs(pos.getZ()-entry.getZ())>16
                ||pos.getY()<=level.getMinY()||pos.getY()+2>=level.getMaxY()||seen.contains(pos))return;
        if(standing(level,pos)){seen.add(pos);queue.add(pos);}
    }
    private static boolean standing(ServerLevel level,BlockPos pos){
        return clear(level,pos)&&clear(level,pos.above())
            &&(level.getBlockState(pos.below()).isSolid()||climbable(level,pos)||climbable(level,pos.below()));
    }
    private static boolean climbable(ServerLevel level,BlockPos pos){
        var state=level.getBlockState(pos);
        return (state.is(BlockTags.CLIMBABLE)&&state.canSurvive(level,pos))||level.getFluidState(pos).is(FluidTags.WATER);
    }
    private static boolean clear(ServerLevel level,BlockPos pos){
        var state=level.getBlockState(pos);
        return !level.getFluidState(pos).is(FluidTags.LAVA)&&(state.is(BlockTags.CLIMBABLE)||state.getCollisionShape(level,pos).isEmpty());
    }
}
