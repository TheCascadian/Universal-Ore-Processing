package com.thecascadian.universaloreprocessing.ladder;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.thecascadian.universaloreprocessing.config.OreProcessingConfig;
import com.thecascadian.universaloreprocessing.item.FormItem;
import com.thecascadian.universaloreprocessing.material.MaterialRegistry;
import com.thecascadian.universaloreprocessing.registry.RegistryHandler;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * One furnace or blast furnace recipe that smelts every solid ladder form of
 * every discovered material. The output count follows the rung ratio from the
 * datapack table and is resolved per stack by {@link Yields}, using the input
 * count at the moment the item is consumed.
 */
public class LadderCookingRecipe extends AbstractCookingRecipe {

    private final Serializer serializer;

    public LadderCookingRecipe(Serializer serializer, float experience, int cookingTime) {
        super(serializer.type.get(), "", CookingBookCategory.MISC,
                Ingredient.of(RegistryHandler.CLUMPS.get(), RegistryHandler.DUST.get(), RegistryHandler.SHARDS.get()),
                ItemStack.EMPTY, experience, cookingTime);
        this.serializer = serializer;
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        if (!OreProcessingConfig.get(OreProcessingConfig.COMMON.ladderSmelting))
            return false;
        ItemStack stack = input.item();
        return FormItem.formOf(stack) != null
                && MaterialRegistry.current().get(FormItem.materialId(stack)).isPresent();
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        ItemStack stack = input.item();
        Form form = FormItem.formOf(stack);
        if (form == null)
            return ItemStack.EMPTY;
        Optional<MaterialRegistry.Material> material = MaterialRegistry.current().get(FormItem.materialId(stack));
        if (material.isEmpty())
            return ItemStack.EMPTY;
        int count = Yields.forItem(LadderTables.ratios().of(form), stack.getCount());
        // the furnace stalls on an empty result, so a table ratio below one still yields one item
        return new ItemStack(material.get().output(), Math.max(count, 1));
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return serializer;
    }

    public static final class Serializer implements RecipeSerializer<LadderCookingRecipe> {

        private final Supplier<RecipeType<?>> type;
        private final MapCodec<LadderCookingRecipe> codec;
        private final StreamCodec<RegistryFriendlyByteBuf, LadderCookingRecipe> streamCodec;

        public Serializer(Supplier<RecipeType<?>> type, int defaultCookingTime) {
            this.type = type;
            this.codec = RecordCodecBuilder.mapCodec(instance -> instance.group(
                    com.mojang.serialization.Codec.FLOAT.fieldOf("experience").orElse(0.1F)
                            .forGetter(LadderCookingRecipe::getExperience),
                    com.mojang.serialization.Codec.INT.fieldOf("cookingtime").orElse(defaultCookingTime)
                            .forGetter(LadderCookingRecipe::getCookingTime))
                    .apply(instance, (experience, time) -> new LadderCookingRecipe(this, experience, time)));
            this.streamCodec = StreamCodec.composite(
                    ByteBufCodecs.FLOAT, LadderCookingRecipe::getExperience,
                    ByteBufCodecs.VAR_INT, LadderCookingRecipe::getCookingTime,
                    (experience, time) -> new LadderCookingRecipe(this, experience, time));
        }

        @Override
        public MapCodec<LadderCookingRecipe> codec() {
            return codec;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, LadderCookingRecipe> streamCodec() {
            return streamCodec;
        }
    }
}
