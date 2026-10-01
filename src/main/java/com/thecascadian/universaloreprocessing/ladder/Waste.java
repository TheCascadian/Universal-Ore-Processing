package com.thecascadian.universaloreprocessing.ladder;

import com.thecascadian.universaloreprocessing.registry.RegistryHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.neoforge.attachment.AttachmentType;

import java.util.function.Supplier;

/**
 * Host rock left over by the ladder: gravel from crushing, sand from grinding
 * and clay from settling. Each machine adds the items it processed to a tally
 * kept on its chunk, and every time the tally reaches the table's "every"
 * value one waste item is paid out, so the outcome involves no randomness.
 */
public enum Waste {
    GRAVEL(Items.GRAVEL, RegistryHandler.GRAVEL_TALLY),
    SAND(Items.SAND, RegistryHandler.SAND_TALLY),
    CLAY(Items.CLAY_BALL, RegistryHandler.CLAY_TALLY);

    private final Item item;
    private final Supplier<AttachmentType<Integer>> tally;

    Waste(Item item, Supplier<AttachmentType<Integer>> tally) {
        this.item = item;
        this.tally = tally;
    }

    public Item item() {
        return item;
    }

    /** Items processed per waste item; zero turns this waste off. */
    public int every() {
        LadderTables.Ratios ratios = LadderTables.ratios();
        return switch (this) {
            case GRAVEL -> ratios.gravelEvery();
            case SAND -> ratios.sandEvery();
            case CLAY -> ratios.clayEvery();
        };
    }

    /** Adds {@code processed} items to the tally at {@code pos} and returns the waste they complete. */
    public ItemStack add(ServerLevel level, BlockPos pos, int processed) {
        int every = every();
        if (every <= 0 || processed <= 0)
            return ItemStack.EMPTY;
        LevelChunk chunk = level.getChunkAt(pos);
        int total = chunk.getData(tally) + processed;
        chunk.setData(tally, total % every);
        chunk.setUnsaved(true);
        return total >= every ? new ItemStack(item, total / every) : ItemStack.EMPTY;
    }
}
