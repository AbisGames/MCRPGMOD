package com.pipican.player;

import com.pipican.Pipican;
import java.util.function.Supplier;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Data attachments the mod puts on entities. */
public final class PipicanAttachments {
    public static final DeferredRegister<AttachmentType<?>> REGISTER = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Pipican.MOD_ID);

    /** The player's class, levels, skills, stats and trinkets. Synced to the owning player only. */
    public static final Supplier<AttachmentType<PlayerClassData>> CLASS_DATA = REGISTER.register("class_data", () ->
            AttachmentType.builder(PlayerClassData::new)
                    .serialize(PlayerClassData.MAP_CODEC)
                    .copyOnDeath()
                    .sync((holder, to) -> holder == to, PlayerClassData.STREAM_CODEC)
                    .build());

    private PipicanAttachments() {}
}
