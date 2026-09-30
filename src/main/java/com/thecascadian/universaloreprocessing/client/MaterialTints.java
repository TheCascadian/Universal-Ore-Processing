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
 * Derives the tint color of a material by averaging the opaque pixels of the
 * texture of its resolved output item. Results are cached per output item and
 * cleared on resource reload; unreadable textures fall back to a hash color.
 */
public final class MaterialTints {

    private static final Map<Item, Integer> CACHE = new ConcurrentHashMap<>();

    private MaterialTints() {
    }

    public static void clear() {
        CACHE.clear();
    }

    /** Returns an opaque ARGB color for the material, or a neutral white for stacks without one. */
    public static int colorFor(String materialId) {
        if (materialId == null)
            return 0xFFFFFFFF;

        Optional<MaterialRegistry.Material> material = MaterialRegistry.current().get(materialId);
        if (material.isEmpty())
            return hashColor(materialId);

        Item output = material.get().output();
        Integer cached = CACHE.get(output);
        if (cached != null)
            return cached;

        int color = averageColor(output).orElseGet(() -> hashColor(materialId));
        CACHE.put(output, color);
        return color;
    }

    private static Optional<Integer> averageColor(Item output) {
        Minecraft minecraft = Minecraft.getInstance();
        TextureAtlasSprite sprite = minecraft.getItemRenderer()
                .getModel(new ItemStack(output), null, null, 0).getParticleIcon();
        ResourceLocation name = sprite.contents().name();
        ResourceLocation file = name.withPath(path -> "textures/" + path + ".png");

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

        int r = (int) (red / samples);
        int g = (int) (green / samples);
        int b = (int) (blue / samples);
        return Optional.of(0xFF000000 | (r << 16) | (g << 8) | b);
    }

    private static int hashColor(String materialId) {
        float hue = (materialId.hashCode() & 0xFFFF) / 65536.0F;
        return 0xFF000000 | Mth.hsvToRgb(hue, 0.5F, 0.85F);
    }
}
