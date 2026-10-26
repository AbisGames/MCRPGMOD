package com.pipican;

import com.mojang.logging.LogUtils;
import com.pipican.core.PipicanConfig;
import com.pipican.core.PipicanRegistries;
import com.pipican.network.PipicanNetwork;
import com.pipican.player.PipicanAttachments;
import com.pipican.trinket.PipicanMenus;
import com.pipican.worldgen.RegionalOreRegistries;
import com.pipican.worldgen.WorldgenCodecs;
import com.pipican.worldgen.RegionalDensityCodecs;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import org.slf4j.Logger;

/**
 * Mod entry point. Only wires things together - see docs/CLASS_SYSTEM.md for how the pieces fit.
 *
 * <p>Packages:
 * <ul>
 *   <li>{@code core} - registries, config, lookup helpers</li>
 *   <li>{@code rpgclass} - the class template ({@link com.pipican.rpgclass.RpgClass}) and its parts</li>
 *   <li>{@code stat}, {@code skill}, {@code trinket} - the other data-driven pieces</li>
 *   <li>{@code effect} - reusable effect types used by passives, weaknesses and skill nodes</li>
 *   <li>{@code player} - per-player saved data and the logic that applies it</li>
 *   <li>{@code event}, {@code command}, {@code network}, {@code client} - glue</li>
 * </ul>
 */
@Mod(Pipican.MOD_ID)
public final class Pipican {
    public static final String MOD_ID = "pipican";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Pipican(IEventBus modBus, ModContainer container) {
        PipicanAttachments.REGISTER.register(modBus);
        PipicanMenus.REGISTER.register(modBus);
        RegionalOreRegistries.register(modBus);
        WorldgenCodecs.register(modBus);
        RegionalDensityCodecs.register(modBus);
        modBus.addListener(PipicanRegistries::onNewDataPackRegistries);
        modBus.addListener(PipicanNetwork::register);
        container.registerConfig(ModConfig.Type.SERVER, PipicanConfig.SPEC);
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
