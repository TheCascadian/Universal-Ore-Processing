package com.thecascadian.universaloreprocessing.block;

import com.thecascadian.universaloreprocessing.registry.RegistryHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The material, dust count and stir count of a slurry cauldron. Synced to the
 * client only so the fluid surface can be tinted; it has no ticker.
 */
public class SlurryCauldronBlockEntity extends BlockEntity {

    private String material = "";
    private int count;
    private int stirs;

    public SlurryCauldronBlockEntity(BlockPos pos, BlockState state) {
        super(RegistryHandler.SLURRY_CAULDRON_BLOCK_ENTITY.get(), pos, state);
    }

    public String material() {
        return material;
    }

    public int count() {
        return count;
    }

    void start(String material) {
        this.material = material;
        this.count = 1;
        this.stirs = 0;
        changed();
    }

    void addDust() {
        count++;
        changed();
    }

    /** Records one stir and returns the total so far. */
    int stir() {
        stirs++;
        setChanged();
        return stirs;
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
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        material = tag.getString("material");
        count = tag.getInt("count");
        stirs = tag.getInt("stirs");
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
