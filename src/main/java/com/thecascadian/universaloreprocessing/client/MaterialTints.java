package com.thecascadian.universaloreprocessing.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.thecascadian.universaloreprocessing.UniversalOreProcessing;
import com.thecascadian.universaloreprocessing.material.MaterialRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Material colors for tinting. The base color is the average of the opaque
 * pixels of the material's smelting output texture; from it a three step
 * hue-shifted ramp is built, so shadows lean cool and highlights lean warm
 * instead of being one color multiplied darker. Cleared on resource reload.
 */
public final class MaterialTints {

    public static final int SHADOW = 0;
    public static final int MID = 1;
    public static final int LIGHT = 2;

    private static final Map<Item, int[]> CACHE = new ConcurrentHashMap<>();

    private MaterialTints() {
    }

    public static void clear() {
        CACHE.clear();
    }

    /** Opaque ARGB tint for one band of the material ramp; white for unknown input. */
    public static int band(String materialId, int band) {
        if (materialId == null || materialId.isEmpty())
            return 0xFFFFFFFF;
        Optional<MaterialRegistry.Material> material = MaterialRegistry.current().get(materialId);
        if (material.isEmpty())
            return ramp(hashColor(materialId))[Mth.clamp(band, SHADOW, LIGHT)];
        int[] ramp = CACHE.computeIfAbsent(material.get().output(),
                output -> ramp(averageColor(output).orElseGet(() -> hashColor(materialId))));
        return ramp[Mth.clamp(band, SHADOW, LIGHT)];
    }

    /** Shadow rotates toward blue and darkens, light rotates toward yellow and desaturates. */
    static int[] ramp(int rgb) {
        float[] hsv = toHsv(rgb);
        return new int[] {
                fromHsv(rotateToward(hsv[0], 240.0F / 360.0F, 14.0F / 360.0F), Math.min(1.0F, hsv[1] * 1.12F), hsv[2] * 0.86F),
                0xFF000000 | rgb,
                fromHsv(rotateToward(hsv[0], 60.0F / 360.0F, 10.0F / 360.0F), hsv[1] * 0.72F, Math.min(1.0F, hsv[2] * 1.14F + 0.04F))
        };
    }

    private static float rotateToward(float hue, float target, float amount) {
        float delta = target - hue;
        if (delta > 0.5F)
            delta -= 1.0F;
        else if (delta < -0.5F)
            delta += 1.0F;
        float step = Math.copySign(Math.min(Math.abs(delta), amount), delta);
        return (hue + step + 1.0F) % 1.0F;
    }

    private static float[] toHsv(int rgb) {
        float r = ((rgb >> 16) & 0xFF) / 255.0F;
        float g = ((rgb >> 8) & 0xFF) / 255.0F;
        float b = (rgb & 0xFF) / 255.0F;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;
        float hue;
        if (delta == 0.0F)
            hue = 0.0F;
        else if (max == r)
            hue = ((g - b) / delta) / 6.0F;
        else if (max == g)
            hue = ((b - r) / delta + 2.0F) / 6.0F;
        else
            hue = ((r - g) / delta + 4.0F) / 6.0F;
        return new float[] { (hue + 1.0F) % 1.0F, max == 0.0F ? 0.0F : delta / max, max };
    }

    private static int fromHsv(float hue, float saturation, float value) {
        return 0xFF000000 | Mth.hsvToRgb(hue, Mth.clamp(saturation, 0.0F, 1.0F), Mth.clamp(value, 0.0F, 1.0F));
    }

    private static Optional<Integer> averageColor(Item output) {
        Minecraft minecraft = Minecraft.getInstance();
        TextureAtlasSprite sprite = minecraft.getItemRenderer()
                .getModel(new ItemStack(output), null, null, 0).getParticleIcon();
        ResourceLocation file = sprite.contents().name().withPath(path -> "textures/" + path + ".png");

        Optional<Resource> resource = minecraft.getResourceManager().getResource(file);
        if (resource.isEmpty())
            return Optional.empty();

        try (InputStream stream = resource.get().open(); NativeImage image = NativeImage.read(stream)) {
            return average(image);
        } catch (IOException e) {
            UniversalOreProcessing.LOGGER.warn("[UniversalOreProcessing] Could not read texture {} for tinting.", file);
            return Optional.empty();
        }
    }

    private static Optional<Integer> average(NativeImage image) {
        // animated textures are vertical strips; only the first square frame is sampled
        int height = Math.min(image.getHeight(), image.getWidth());
        long red = 0;
        long green = 0;
        long blue = 0;
        long samples = 0;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int abgr = image.getPixelRGBA(x, y);
                if ((abgr >>> 24) < 255)
                    continue;
                red += abgr & 0xFF;
                green += (abgr >> 8) & 0xFF;
                blue += (abgr >> 16) & 0xFF;
                samples++;
            }
        }
        if (samples == 0)
            return Optional.empty();
        return Optional.of((int) (red / samples) << 16 | (int) (green / samples) << 8 | (int) (blue / samples));
    }

    private static int hashColor(String materialId) {
        float hue = (materialId.hashCode() & 0xFFFF) / 65536.0F;
        return Mth.hsvToRgb(hue, 0.5F, 0.85F);
    }
}
