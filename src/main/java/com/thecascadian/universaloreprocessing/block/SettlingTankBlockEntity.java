package com.thecascadian.universaloreprocessing.block;

import com.thecascadian.universaloreprocessing.item.FormItem;
import com.thecascadian.universaloreprocessing.ladder.Form;
import com.thecascadian.universaloreprocessing.ladder.Waste;
import com.thecascadian.universaloreprocessing.registry.RegistryHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * The material, dust count and stir count of a Settling Tank. Synced to the
 * client so the surface can be tinted; it has no ticker.
 */
public class SettlingTankBlockEntity extends BlockEntity {

    private String material = "";
    private int count;
    private int stirs;
    // clay balls settled under the shards; paid once, when the batch is ready
    private int clay;

    public SettlingTankBlockEntity(BlockPos pos, BlockState state) {
        super(RegistryHandler.SETTLING_TANK_BLOCK_ENTITY.get(), pos, state);
    }

    public String material() {
        return material;
    }

    public int count() {
        return count;
    }

    boolean canTake(ItemStack stack) {
        if (FormItem.formOf(stack) != Form.DUST || count >= SettlingTankBlock.CAPACITY)
            return false;
        return count == 0 || material.equals(FormItem.materialId(stack));
    }

    void add(String material, int amount) {
        if (count == 0) {
            this.material = material;
            this.stirs = 0;
        }
        count += amount;
        changed();
    }

    int stir() {
        stirs++;
        setChanged();
        return stirs;
    }

    /** The batch has finished settling: the clay it leaves is counted now, so hoppers can take it. */
    void settleClay(ServerLevel serverLevel) {
        clay = Waste.CLAY.add(serverLevel, worldPosition, count).getCount();
        setChanged();
    }

    /** Takes up to {@code amount} shards; the tank empties when the shards and clay are gone. */
    ItemStack takeShards(int amount) {
        int taken = Math.min(amount, count);
        if (taken <= 0)
            return ItemStack.EMPTY;
        ItemStack shards = FormItem.create(Form.SHARDS, material, taken);
        count -= taken;
        emptyIfDone();
        changed();
        return shards;
    }

    ItemStack takeClay(int amount) {
        int taken = Math.min(amount, clay);
        if (taken <= 0)
            return ItemStack.EMPTY;
        clay -= taken;
        emptyIfDone();
        changed();
        return new ItemStack(Waste.CLAY.item(), taken);
    }

    private void emptyIfDone() {
        if (count > 0 || clay > 0)
            return;
        stirs = 0;
        if (level != null)
            SettlingTankBlock.emptied(level, worldPosition);
    }

    /**
     * Dust goes in from above or the sides; once the tank is ready, shards
     * (slot 0) and clay (slot 1) come out from below.
     */
    public IItemHandler handlerFor(Direction side) {
        return new IItemHandler() {
            @Override
            public int getSlots() {
                return 2;
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                if (!ready())
                    return ItemStack.EMPTY;
                if (slot == 1)
                    return clay > 0 ? new ItemStack(Waste.CLAY.item(), clay) : ItemStack.EMPTY;
                return count > 0 ? FormItem.create(Form.SHARDS, material, count) : ItemStack.EMPTY;
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                if (slot != 0 || side == Direction.DOWN || !(level instanceof ServerLevel serverLevel))
                    return stack;
                return SettlingTankBlock.addDust(serverLevel, worldPosition, SettlingTankBlockEntity.this, stack, simulate);
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                if (side != Direction.DOWN || !ready())
                    return ItemStack.EMPTY;
                if (slot == 1) {
                    int taken = Math.min(Math.min(amount, clay), 64);
                    if (simulate)
                        return taken <= 0 ? ItemStack.EMPTY : new ItemStack(Waste.CLAY.item(), taken);
                    return takeClay(taken);
                }
                int taken = Math.min(Math.min(amount, count), 64);
                if (simulate)
                    return taken <= 0 ? ItemStack.EMPTY : FormItem.create(Form.SHARDS, material, taken);
                return takeShards(taken);
            }

            @Override
            public int getSlotLimit(int slot) {
                return slot == 1 ? 64 : SettlingTankBlock.CAPACITY;
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return slot == 0 && side != Direction.DOWN && FormItem.formOf(stack) == Form.DUST;
            }
        };
    }

    private boolean ready() {
        return (count > 0 || clay > 0) && getBlockState().getValue(SettlingTankBlock.STAGE) == SettlingTankBlock.READY;
    }

    private void changed() {
        setChanged();
        if (level != null && !level.isClientSide)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("material", material);
        tag.putInt("count", count);
        tag.putInt("stirs", stirs);
        tag.putInt("clay", clay);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        material = tag.getString("material");
        count = tag.getInt("count");
        stirs = tag.getInt("stirs");
        clay = tag.getInt("clay");
        if (level != null && level.isClientSide)
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
