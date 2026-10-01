package com.thecascadian.universaloreprocessing.network;

import com.thecascadian.universaloreprocessing.ladder.LadderTables;
import com.thecascadian.universaloreprocessing.registry.RegistryHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.function.Consumer;

/**
 * Server to client feedback. Sounds are played on the server with vanilla
 * events; tinted particles need the client-side material color, so the server
 * sends a tiny payload and the client spawns them. The client handlers are
 * installed by client setup, so this class references no client-only code.
 */
public final class Feedback {

    private Feedback() {
    }

    /** One verb of the ladder, each with its own vanilla sound tuned in volume and pitch. */
    public enum Verb {
        STRIKE(SoundEvents.STONE_HIT, 0.9F, 0.7F),
        BREAK(SoundEvents.STONE_BREAK, 0.8F, 1.15F),
        GRIND(SoundEvents.GRINDSTONE_USE, 0.55F, 1.25F),
        STIR(SoundEvents.BOAT_PADDLE_WATER, 0.6F, 1.35F),
        SETTLE(SoundEvents.AMETHYST_BLOCK_CHIME, 0.7F, 1.4F),
        WASH(SoundEvents.GENERIC_SPLASH, 0.35F, 1.6F);

        private final SoundEvent sound;
        private final float volume;
        private final float pitch;

        Verb(SoundEvent sound, float volume, float pitch) {
            this.sound = sound;
            this.volume = volume;
            this.pitch = pitch;
        }
    }

    public record ParticlePayload(Vec3 pos, String material, int verb) implements CustomPacketPayload {
        public static final Type<ParticlePayload> TYPE = new Type<>(RegistryHandler.id("feedback"));
        public static final StreamCodec<ByteBuf, ParticlePayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.DOUBLE, p -> p.pos().x,
                ByteBufCodecs.DOUBLE, p -> p.pos().y,
                ByteBufCodecs.DOUBLE, p -> p.pos().z,
                ByteBufCodecs.STRING_UTF8, ParticlePayload::material,
                ByteBufCodecs.VAR_INT, ParticlePayload::verb,
                (x, y, z, material, verb) -> new ParticlePayload(new Vec3(x, y, z), material, verb));

        public Verb verbValue() {
            Verb[] values = Verb.values();
            return values[Math.floorMod(verb, values.length)];
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record RatiosPayload(double clumps, double dust, double shards, int oreClumps) implements CustomPacketPayload {
        public static final Type<RatiosPayload> TYPE = new Type<>(RegistryHandler.id("ratios"));
        public static final StreamCodec<ByteBuf, RatiosPayload> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.DOUBLE, RatiosPayload::clumps,
                ByteBufCodecs.DOUBLE, RatiosPayload::dust,
                ByteBufCodecs.DOUBLE, RatiosPayload::shards,
                ByteBufCodecs.VAR_INT, RatiosPayload::oreClumps,
                RatiosPayload::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    // replaced by client setup; stays a no-op on a dedicated server
    private static Consumer<ParticlePayload> particleHandler = payload -> {
    };

    public static void setParticleHandler(Consumer<ParticlePayload> handler) {
        particleHandler = handler;
    }

    public static void register(PayloadRegistrar registrar) {
        registrar.playToClient(ParticlePayload.TYPE, ParticlePayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> particleHandler.accept(payload)));
        registrar.playToClient(RatiosPayload.TYPE, RatiosPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> LadderTables.acceptSynced(new LadderTables.Ratios(
                        payload.clumps(), payload.dust(), payload.shards(), payload.oreClumps()))));
    }

    /** Plays the verb's sound and sends tinted particles to every player tracking the position. */
    public static void play(ServerLevel level, Vec3 pos, Verb verb, String material) {
        level.playSound(null, pos.x, pos.y, pos.z, verb.sound, SoundSource.BLOCKS, verb.volume,
                verb.pitch);
        if (material != null) {
            PacketDistributor.sendToPlayersTrackingChunk(level, new ChunkPos(BlockPos.containing(pos)),
                    new ParticlePayload(pos, material, verb.ordinal()));
        }
    }

    public static void syncRatios(ServerPlayer player) {
        LadderTables.Ratios ratios = LadderTables.ratios();
        PacketDistributor.sendToPlayer(player,
                new RatiosPayload(ratios.clumps(), ratios.dust(), ratios.shards(), ratios.oreClumps()));
    }
}
