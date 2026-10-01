package com.thecascadian.universaloreprocessing.client;

import com.thecascadian.universaloreprocessing.network.Feedback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Spawns particles tinted to the processed material for each ladder verb. */
public final class ClientFeedback {

    private ClientFeedback() {
    }

    public static void spawn(Feedback.ParticlePayload payload) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null)
            return;

        Feedback.Verb verb = payload.verbValue();
        int count = switch (verb) {
            case STRIKE -> 6;
            case BREAK -> 14;
            case GRIND -> 8;
            case STIR -> 6;
            case SETTLE -> 5;
            case WASH -> 4;
        };
        float spread = verb == Feedback.Verb.BREAK ? 0.3F : 0.2F;
        double rise = switch (verb) {
            case STRIKE, BREAK -> 0.12D;
            case GRIND -> 0.04D;
            default -> 0.02D;
        };

        RandomSource random = level.getRandom();
        Vec3 pos = payload.pos();
        for (int i = 0; i < count; i++) {
            // alternate shadow and light bands so the burst reads as the material ramp, not one flat color
            int band = i % 3 == 0 ? MaterialTints.LIGHT : i % 3 == 1 ? MaterialTints.MID : MaterialTints.SHADOW;
            int rgb = MaterialTints.band(payload.material(), band);
            DustParticleOptions options = new DustParticleOptions(new Vector3f(
                    ((rgb >> 16) & 0xFF) / 255.0F, ((rgb >> 8) & 0xFF) / 255.0F, (rgb & 0xFF) / 255.0F),
                    verb == Feedback.Verb.SETTLE ? 0.6F : 0.9F);
            level.addParticle(options,
                    pos.x + (random.nextFloat() - 0.5F) * spread * 2.0F,
                    pos.y + random.nextFloat() * 0.1F,
                    pos.z + (random.nextFloat() - 0.5F) * spread * 2.0F,
                    0.0D, rise, 0.0D);
        }
    }
}
