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
                for(int cz=Math.floorDiv(entry.getZ()-18,16);cz<=Math.floorDiv(entry.getZ()+18,16);cz++){
                    WorldgenTestProgress.touch("entrance neighborhood chunk "+cx+","+cz);level.getChunk(cx,cz);
                }
            var queue=new ArrayDeque<BlockPos>();
            var seen=new HashSet<BlockPos>();
            var surfaces=new HashMap<Long,Double>();
            for(int x=36;x<=44;x++){
                var pos=new BlockPos(x,1,3).rotate(rotation).offset(origin);
                if(standing(level,pos)&&seen.add(pos))queue.add(pos);
            }
            while(!queue.isEmpty()&&seen.size()<100000){
                var pos=queue.removeFirst();
                int forward=(pos.getX()-entry.getX())*outward.getStepX()
                    +(pos.getZ()-entry.getZ())*outward.getStepZ();
                // Reach exterior terrain with breathing room. A forest canopy
                // may hide the sky; an underground stone ceiling must not pass.
                if(forward>=8&&level.getFluidState(pos.above()).isEmpty()){
                    long column=BlockPos.asLong(pos.getX(),0,pos.getZ());
                    double surface=surfaces.computeIfAbsent(column,key->exteriorSurface(level,pos.getX(),pos.getZ()));
                    if(floorHeight(level,pos)+0.001>=surface)return true;
                }
                for(var direction:Direction.Plane.HORIZONTAL)for(int dy=-1;dy<=1;dy++){
                    var next=pos.relative(direction).above(dy);
                    double from=floorHeight(level,pos),to=floorHeight(level,next);
                    if(!Double.isFinite(to)||Math.abs(to-from)>1.25)continue;
                    double raised=Math.max(from,to);
                    if(!clearAt(level,pos,raised)||!clearAt(level,next,raised))continue;
                    enqueue(level,entry,next,queue,seen);
                }
                if(climbable(level,pos)){
                    enqueue(level,entry,pos.above(),queue,seen);
                    enqueue(level,entry,pos.below(),queue,seen);
                }
            }
            var shaft=entry.relative(outward);
            int count=0,lowest=Integer.MAX_VALUE,highest=Integer.MIN_VALUE;
            for(int y=level.getMinY();y<level.getMaxY();y++){
                var pos=new BlockPos(shaft.getX(),y,shaft.getZ());
                if(level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.LADDER)){
                    count++;lowest=Math.min(lowest,y);highest=Math.max(highest,y);
                }
            }
            com.pipican.Pipican.LOGGER.warn("Unreachable expedition entry {} outward {} visited {} ladder column {} count {} lowest {} highest {} entry block {} head {} floor {} outside surface {}",
                entry,outward,seen.size(),shaft,count,lowest,highest,level.getBlockState(shaft),level.getBlockState(shaft.above()),
                level.getBlockState(shaft.below()),level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,shaft.relative(outward).getX(),shaft.relative(outward).getZ()));
            com.pipican.Pipican.LOGGER.warn("Exit search max forward {} exterior-ground columns {}",
                seen.stream().mapToInt(pos->(pos.getX()-entry.getX())*outward.getStepX()+(pos.getZ()-entry.getZ())*outward.getStepZ()).max().orElse(-1),surfaces.size());
        }
        return false;
    }
    static double exteriorSurface(ServerLevel level,int x,int z){
        int y=level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,x,z);
        while(y>level.getMinY()){
            var state=level.getBlockState(new BlockPos(x,y-1,z));
            boolean growth=state.is(BlockTags.LOGS)||state.is(BlockTags.LEAVES)
                ||state.is(net.minecraft.world.level.block.Blocks.SNOW)
                ||state.getBlock() instanceof net.minecraft.world.level.block.VegetationBlock;
            if(!growth)for(var palette:com.pipican.worldgen.RegionalNatureBlocks.PALETTES.values())
                if(state.is(palette.trunk().get())||state.is(palette.canopy().get())||state.is(palette.bloom().get())){growth=true;break;}
            if(!state.isAir()&&!growth){
                var shape=state.getCollisionShape(level,new BlockPos(x,y-1,z));
                if(!state.getFluidState().isEmpty())return y;
                if(!shape.isEmpty())return y-1+shape.max(Direction.Axis.Y);
            }
            y--;
        }
        return y;
    }
    private static void enqueue(ServerLevel level,BlockPos entry,BlockPos pos,
            ArrayDeque<BlockPos> queue,Set<BlockPos> seen){
        if(Math.abs(pos.getX()-entry.getX())>16||Math.abs(pos.getZ()-entry.getZ())>16
                ||pos.getY()<=level.getMinY()||pos.getY()+2>=level.getMaxY()||seen.contains(pos))return;
        if(standing(level,pos)){seen.add(pos);queue.add(pos);}
    }
    private static boolean standing(ServerLevel level,BlockPos pos){
        double height=floorHeight(level,pos);
        return Double.isFinite(height)&&clearAt(level,pos,height);
    }
    private static double floorHeight(ServerLevel level,BlockPos pos){
        if(climbable(level,pos))return pos.getY();
        var here=level.getBlockState(pos).getCollisionShape(level,pos);
        if(!here.isEmpty()){
            double top=here.max(Direction.Axis.Y);
            return top<1?pos.getY()+top:Double.NaN;
        }
        if(climbable(level,pos.below()))return pos.getY();
        var below=level.getBlockState(pos.below()).getCollisionShape(level,pos.below());
        return below.isEmpty()?Double.NaN:pos.getY()-1+below.max(Direction.Axis.Y);
    }
    private static boolean clearAt(ServerLevel level,BlockPos pos,double feet){
        for(int y=(int)Math.floor(feet);y<feet+2;y++)
            if(level.getFluidState(new BlockPos(pos.getX(),y,pos.getZ())).is(FluidTags.LAVA))return false;
        return level.noBlockCollision(null,new net.minecraft.world.phys.AABB(
            pos.getX()+0.2,feet+0.0001,pos.getZ()+0.2,pos.getX()+0.8,feet+2,pos.getZ()+0.8));
    }
    private static boolean climbable(ServerLevel level,BlockPos pos){
        var state=level.getBlockState(pos);
        return (state.is(BlockTags.CLIMBABLE)&&state.canSurvive(level,pos))||level.getFluidState(pos).is(FluidTags.WATER);
    }
}
