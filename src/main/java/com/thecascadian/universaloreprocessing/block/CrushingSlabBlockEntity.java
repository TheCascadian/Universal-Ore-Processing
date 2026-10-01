package com.thecascadian.universaloreprocessing.block;

import com.thecascadian.universaloreprocessing.api.stroke.Stroke;
import com.thecascadian.universaloreprocessing.api.stroke.StrokeConsumer;
import com.thecascadian.universaloreprocessing.config.OreProcessingConfig;
import com.thecascadian.universaloreprocessing.item.FormItem;
import com.thecascadian.universaloreprocessing.ladder.Form;
import com.thecascadian.universaloreprocessing.ladder.LadderTables;
import com.thecascadian.universaloreprocessing.material.MaterialRegistry;
import com.thecascadian.universaloreprocessing.network.Feedback;
import com.thecascadian.universaloreprocessing.registry.RegistryHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Holds the one item resting on a Crushing Slab and the strikes it has taken.
 * It has no ticker: it changes only when struck, filled or emptied.
 */
public class CrushingSlabBlockEntity extends BlockEntity implements StrokeConsumer {

    // floor of the basin in block units; the renderer lays items on it
    public static final double FLOOR = 14.0D / 16.0D;

    private ItemStack item = ItemStack.EMPTY;
    private int strikes;

    public CrushingSlabBlockEntity(BlockPos pos, BlockState state) {
        super(RegistryHandler.CRUSHING_SLAB_BLOCK_ENTITY.get(), pos, state);
    }

    public ItemStack item() {
        return item;
    }

    public boolean isEmpty() {
        return item.isEmpty();
    }

    void place(ItemStack stack) {
        item = stack;
        strikes = 0;
        changed();
    }

    ItemStack take() {
        ItemStack taken = item;
        item = ItemStack.EMPTY;
        strikes = 0;
        changed();
        return taken;
    }

    @Override
    public boolean accept(Stroke stroke) {
        if (!(level instanceof ServerLevel serverLevel) || isEmpty())
            return false;
        strike(serverLevel);
        return true;
    }

    /** One strike: advance the crack stage, or break the item into clumps on the final strike. */
    void strike(ServerLevel serverLevel) {
        MaterialRegistry.InputEntry entry = MaterialRegistry.current().inputFor(item.getItem());
        Vec3 top = Vec3.atLowerCornerOf(worldPosition).add(0.5D, FLOOR + 0.05D, 0.5D);
        if (entry == null) {
            // the material vanished in a reload; hand the item back untouched
            Block.popResource(serverLevel, worldPosition.above(), take());
            return;
        }

        strikes++;
        int needed = OreProcessingConfig.get(OreProcessingConfig.COMMON.strikesPerItem);
        if (strikes < needed) {
            Feedback.play(serverLevel, top, Feedback.Verb.STRIKE, entry.materialId());
            changed();
            return;
        }

        int count = entry.raw() ? 1 : Math.max(1, LadderTables.ratios().oreClumps());
        item = ItemStack.EMPTY;
        strikes = 0;
        changed();
        Feedback.play(serverLevel, top, Feedback.Verb.BREAK, entry.materialId());

        ItemStack clumps = FormItem.create(Form.CLUMPS, entry.materialId(), count);
        ItemEntity popped = new ItemEntity(serverLevel, top.x, worldPosition.getY() + 1.05D, top.z, clumps);
        // a fixed upward pop keeps the outcome free of randomness; only the Sluice rolls dice
        popped.setDeltaMovement(0.0D, 0.25D, 0.0D);
        popped.setDefaultPickUpDelay();
        serverLevel.addFreshEntity(popped);
    }

    /** Crack stage shown on the basin: the last four strikes before breaking. */
    private int crackStage() {
        if (item.isEmpty() || strikes == 0)
            return 0;
        int needed = OreProcessingConfig.get(OreProcessingConfig.COMMON.strikesPerItem);
        return Mth.clamp(strikes - needed + 1 + CrushingSlabBlock.CRACK_STAGES, 0, CrushingSlabBlock.CRACK_STAGES);
    }

    private void changed() {
        setChanged();
        if (level == null || level.isClientSide)
            return;
        BlockState state = getBlockState();
        BlockState next = state.setValue(CrushingSlabBlock.CRACK, crackStage());
        if (next != state)
            level.setBlock(worldPosition, next, Block.UPDATE_CLIENTS);
        level.sendBlockUpdated(worldPosition, next, next, Block.UPDATE_CLIENTS);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!item.isEmpty())
            tag.put("item", item.save(registries));
        tag.putInt("strikes", strikes);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        item = tag.contains("item") ? ItemStack.parseOptional(registries, tag.getCompound("item")) : ItemStack.EMPTY;
        strikes = tag.getInt("strikes");
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
