package com.thecascadian.universaloreprocessing.block;

import com.thecascadian.universaloreprocessing.item.FormItem;
import com.thecascadian.universaloreprocessing.ladder.Form;
import com.thecascadian.universaloreprocessing.network.Feedback;
import com.thecascadian.universaloreprocessing.registry.RegistryHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/**
 * The clumps waiting in a Quern: one stack of one material. Ground dust is
 * never stored; it leaves through the spout as soon as it is made.
 */
public class QuernBlockEntity extends BlockEntity {

    private ItemStack contents = ItemStack.EMPTY;
    private final IItemHandler handler = new Handler();

    public QuernBlockEntity(BlockPos pos, BlockState state) {
        super(RegistryHandler.QUERN_BLOCK_ENTITY.get(), pos, state);
    }

    public ItemStack contents() {
        return contents;
    }

    public IItemHandler handler() {
        return handler;
    }

    /** True when the stack is clumps that can join what the quern already holds. */
    boolean canTake(ItemStack stack) {
        if (FormItem.formOf(stack) != Form.CLUMPS)
            return false;
        return contents.isEmpty() || ItemStack.isSameItemSameComponents(contents, stack)
                && contents.getCount() < contents.getMaxStackSize();
    }

    /** Inserts as much of the stack as fits and returns the remainder. */
    ItemStack insert(ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || !canTake(stack))
            return stack;
        int room = contents.isEmpty() ? stack.getMaxStackSize() : contents.getMaxStackSize() - contents.getCount();
        int moved = Math.min(room, stack.getCount());
        if (!simulate) {
            if (contents.isEmpty())
                contents = stack.copyWithCount(moved);
            else
                contents.grow(moved);
            setChanged();
        }
        return stack.copyWithCount(stack.getCount() - moved);
    }

    ItemStack takeAll() {
        ItemStack taken = contents;
        contents = ItemStack.EMPTY;
        setChanged();
        return taken;
    }

    /** Grinds one clump and sends the dust out of the spout. */
    void grindOne(Direction spout) {
        if (!(level instanceof ServerLevel serverLevel) || contents.isEmpty())
            return;
        ItemStack dust = GrindstoneFeed.grindQuietly(contents);
        contents.shrink(1);
        if (contents.isEmpty())
            contents = ItemStack.EMPTY;
        setChanged();
        Feedback.play(serverLevel, Vec3.atCenterOf(worldPosition), Feedback.Verb.TURN, FormItem.materialId(dust));

        // a container directly below catches the dust; otherwise it spills from the spout
        IItemHandler below = serverLevel.getCapability(Capabilities.ItemHandler.BLOCK, worldPosition.below(), Direction.UP);
        if (below != null)
            dust = ItemHandlerHelper.insertItem(below, dust, false);
        if (dust.isEmpty())
            return;
        Vec3 out = Vec3.atCenterOf(worldPosition).add(spout.getStepX() * 0.6D, -0.2D, spout.getStepZ() * 0.6D);
        ItemEntity entity = new ItemEntity(serverLevel, out.x, out.y, out.z, dust);
        entity.setDeltaMovement(spout.getStepX() * 0.05D, 0.0D, spout.getStepZ() * 0.05D);
        serverLevel.addFreshEntity(entity);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!contents.isEmpty())
            tag.put("contents", contents.save(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        contents = tag.contains("contents") ? ItemStack.parseOptional(registries, tag.getCompound("contents"))
                : ItemStack.EMPTY;
    }

    /** Insert-only: hoppers and pipes may add clumps but never take them back out. */
    private class Handler implements IItemHandler {

        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return contents;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return insert(stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return FormItem.formOf(stack) == Form.CLUMPS;
        }
    }
}
