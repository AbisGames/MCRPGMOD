package com.pipican.gametest;

import com.pipican.Pipican;
import java.util.concurrent.atomic.AtomicLong;

/** Wall-clock diagnostics: synchronous chunk work cannot advance GameTest ticks. */
@net.neoforged.fml.common.EventBusSubscriber(modid=Pipican.MOD_ID)
public final class WorldgenTestProgress {
    private static Thread globalWatch;
    @net.neoforged.bus.api.SubscribeEvent
    public static void started(net.neoforged.neoforge.event.server.ServerStartedEvent event){
        if(event.getServer() instanceof net.minecraft.gametest.framework.GameTestServer)
            globalWatch=watch(Thread.currentThread(),"GameTest server");
    }
    @net.neoforged.bus.api.SubscribeEvent
    public static void stopping(net.neoforged.neoforge.event.server.ServerStoppingEvent event){
        if(globalWatch!=null){globalWatch.interrupt();globalWatch=null;}
    }
    private static final AtomicLong LAST=new AtomicLong();
    private static volatile String phase="starting";
    private WorldgenTestProgress(){}
    static void touch(String current){phase=current;LAST.set(System.nanoTime());}
    static Thread watch(Thread server,String suite){
        touch("suite "+suite);
        return Thread.ofPlatform().daemon().name("worldgen-progress-watchdog").start(()->{
            try{
                while(true){
                    Thread.sleep(60000);
                    long idle=(System.nanoTime()-LAST.get())/1_000_000_000L;
                    if(idle<120)continue;
                    Pipican.LOGGER.warn("Worldgen fixture made no progress for {}s phase {} server stack {}",idle,phase,
                        java.util.Arrays.toString(server.getStackTrace()));
                    for(var entry:Thread.getAllStackTraces().entrySet()){
                        var thread=entry.getKey();
                        if(thread==server||thread==Thread.currentThread())continue;
                        if(thread.getName().contains("Worker")||thread.getName().contains("worldgen"))
                            Pipican.LOGGER.warn("Worldgen worker {} state {} stack {}",thread.getName(),thread.getState(),
                                java.util.Arrays.toString(entry.getValue()));
                    }
                    if(idle>=300){
                        Pipican.LOGGER.error("Aborting isolated GameTest JVM after five minutes without fixture progress: {}",phase);
                        Runtime.getRuntime().halt(4);
                    }
                }
            }catch(InterruptedException expected){Thread.currentThread().interrupt();}
        });
    }
}
