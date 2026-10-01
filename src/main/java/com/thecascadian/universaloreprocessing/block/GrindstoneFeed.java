package com.thecascadian.universaloreprocessing.block;

import com.thecascadian.universaloreprocessing.config.OreProcessingConfig;
import com.thecascadian.universaloreprocessing.item.FormItem;
import com.thecascadian.universaloreprocessing.ladder.Form;
import com.thecascadian.universaloreprocessing.network.Feedback;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * The Grind verb on the vanilla grindstone. Players right-click it with clumps;
 * hoppers insert through an item handler that stores nothing and grinds each
 * accepted item at once, dropping the dust below the wheel.
 */
public final class GrindstoneFeed {

    private GrindstoneFeed() {
    }

    public static boolean accepts(ItemStack stack) {
        return FormItem.formOf(stack) == Form.CLUMPS;
    }

    /** Grinds one clump into one dust and returns it, with feedback at the wheel. */
    public static ItemStack grind(ServerLevel level, BlockPos pos, ItemStack clumps) {
        Feedback.play(level, Vec3.atCenterOf(pos), Feedback.Verb.GRIND, FormItem.materialId(clumps));
        return grindQuietly(clumps);
    }

    /** The dust one clump grinds into, keeping its washed mark; the caller plays its own feedback. */
    public static ItemStack grindQuietly(ItemStack clumps) {
        ItemStack dust = FormItem.create(Form.DUST, FormItem.materialId(clumps), 1);
        if (FormItem.isWashed(clumps))
            dust = SluiceBlock.washed(dust);
        return dust;
    }

    public static IItemHandler handlerFor(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel))
            return null;
        if (!OreProcessingConfig.get(OreProcessingConfig.COMMON.grindstoneHopperInput))
            return null;
        return new Handler(serverLevel, pos.immutable());
    }

    private record Handler(ServerLevel level, BlockPos pos) implements IItemHandler {

        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (stack.isEmpty() || !accepts(stack))
                return stack;
            if (!simulate) {
                ItemStack dust = grind(level, pos, stack);
                ItemEntity entity = new ItemEntity(level, pos.getX() + 0.5D, pos.getY() - 0.2D, pos.getZ() + 0.5D, dust);
                entity.setDeltaMovement(0.0D, -0.1D, 0.0D);
                level.addFreshEntity(entity);
            }
            return stack.copyWithCount(stack.getCount() - 1);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return accepts(stack);
        }
    }
}
