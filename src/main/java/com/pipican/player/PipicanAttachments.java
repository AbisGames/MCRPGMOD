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

    /** The player's Tamashigane: path, souls, stage and Bankai timers (phase 6). Saved, kept through death. */
    public static final Supplier<AttachmentType<com.pipican.soul.SoulData>> SOUL = REGISTER.register("soul", () ->
            AttachmentType.builder(() -> com.pipican.soul.SoulData.NONE)
                    .serialize(com.pipican.soul.SoulData.MAP_CODEC)
                    .copyOnDeath()
                    .build());

    /** Movement and look flags the client applies itself (double jump, glide, hidden name...), from the class state. Not saved; sent to everyone. */
    public static final Supplier<AttachmentType<MoveFlags>> MOVE_FLAGS = REGISTER.register("move_flags", () ->
            AttachmentType.builder(() -> MoveFlags.NONE)
                    .sync((holder, to) -> true, MoveFlags.STREAM_CODEC)
                    .build());

    /** The movement flags: names of {@code BonusEffect.Kind}s the client acts on. */
    public record MoveFlags(java.util.List<String> flags) {
        public static final MoveFlags NONE = new MoveFlags(java.util.List.of());
        public static final net.minecraft.network.codec.StreamCodec<io.netty.buffer.ByteBuf, MoveFlags> STREAM_CODEC =
                net.minecraft.network.codec.ByteBufCodecs.STRING_UTF8.apply(net.minecraft.network.codec.ByteBufCodecs.list())
                        .map(MoveFlags::new, MoveFlags::flags);

        public boolean has(String flag) {
            return flags.contains(flag);
        }
    }

    private PipicanAttachments() {}
}
