package com.pipican.worldgen;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.pipican.Pipican;
import java.util.Comparator;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * Singleplayer explorer locator for the 70 authored settlements.
 *
 * <p>Read-only commands make it possible to inspect every capital, town and
 * smaller settlement without changing player data or forcing chunk loads.
 * Coordinates are authoritative worldgen-map anchors, not teleport targets.
 */
@EventBusSubscriber(modid = Pipican.MOD_ID)
public final class RpgSitesCommand {
    private static final LandmarkCatalog CATALOG=new LandmarkCatalog();
    private RpgSitesCommand(){}

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(
            Commands.literal("rpgsites")
                .executes(context -> nearest(context.getSource()))
                .then(Commands.literal("locate")
                    .then(Commands.argument("id",StringArgumentType.word())
                        .suggests((context,builder) -> {
                            for(var landmark:CATALOG.all())
                                if(landmark.id().startsWith(builder.getRemainingLowerCase()))
                                    builder.suggest(landmark.id());
                            return builder.buildFuture();
                        })
                        .executes(context->locate(context.getSource(),
                            StringArgumentType.getString(context,"id")))))
                .then(Commands.literal("progress")
                    .executes(context->progress(context.getSource())))
        );
    }

    private static int nearest(net.minecraft.commands.CommandSourceStack source) {
        double x=source.getPosition().x(), z=source.getPosition().z();
        source.sendSuccess(()->Component.literal("Nearby authored RPG sites:"),false);
        CATALOG.all().stream().sorted(Comparator.comparingDouble(p->
            Math.pow(p.x()-x,2)+Math.pow(p.z()-z,2))).limit(8)
            .forEach(p->source.sendSuccess(()->Component.literal(
                p.id()+" ["+p.tier()+"]  X="+p.x()+"  Z="+p.z()),false));
        return 8;
    }

    private static int locate(net.minecraft.commands.CommandSourceStack source,String id) {
        var match=CATALOG.all().stream().filter(x->x.id().equals(id)).findFirst();
        if(match.isEmpty()) {
            source.sendFailure(Component.literal("Unknown RPG settlement: "+id));
            return 0;
        }
        var p=match.get();
        source.sendSuccess(()->Component.literal(
            p.id()+" ["+p.tier()+"] at "+p.x()+", "+p.z()
            +" in "+p.region()+"; /rpgsites progress shows loaded sites."),false);
        return 1;
    }

    private static int progress(net.minecraft.commands.CommandSourceStack source) {
        var level=source.getLevel();
        if(!(level.getChunkSource().getGenerator().getBiomeSource()
                instanceof RegionalBiomeSource)) {
            source.sendFailure(Component.literal("Not using Pipican RPG World preset"));
            return 0;
        }
        var state=CapitalPlacementSavedData.get(level);
        long completed=CATALOG.all().stream().filter(p->{
            var progress=state.progress(p.id());
            int total=p.tier().equals("capital")?63:1;
            return progress!=null&&progress.nextTile()>=total;
        }).count();
        source.sendSuccess(()->Component.literal(
            "Built "+completed+" / 70 named RPG sites in this save."),false);
        return (int)completed;
    }
}
