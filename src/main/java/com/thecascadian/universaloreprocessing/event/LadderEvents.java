package com.thecascadian.universaloreprocessing.event;

import com.thecascadian.universaloreprocessing.UniversalOreProcessing;
import com.thecascadian.universaloreprocessing.api.stroke.Stroke;
import com.thecascadian.universaloreprocessing.api.stroke.StrokeConsumer;
import com.thecascadian.universaloreprocessing.block.GrindstoneFeed;
import com.thecascadian.universaloreprocessing.block.SluiceBlock;
import com.thecascadian.universaloreprocessing.block.SlurryCauldronBlock;
import com.thecascadian.universaloreprocessing.config.OreProcessingConfig;
import com.thecascadian.universaloreprocessing.guide.Hints;
import com.thecascadian.universaloreprocessing.item.FormItem;
import com.thecascadian.universaloreprocessing.ladder.Form;
import com.thecascadian.universaloreprocessing.ladder.LadderTables;
import com.thecascadian.universaloreprocessing.material.MaterialRegistry;
import com.thecascadian.universaloreprocessing.network.Feedback;
import com.thecascadian.universaloreprocessing.registry.RegistryHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

/**
 * Game-bus hooks that add the ladder verbs to vanilla blocks and entities
 * without replacing them: Grind on the grindstone, the first Stir into a water
 * cauldron, washing over a Sluice, and the falling heavy block stroke.
 */
@EventBusSubscriber(modid = UniversalOreProcessing.MODID)
public final class LadderEvents {

    private LadderEvents() {
    }

    // -------------------------------------------------------------------------
    // Grind and Stir
    // -------------------------------------------------------------------------

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Level level = event.getLevel();
        BlockPos pos = event.getPos();
        BlockState state = level.getBlockState(pos);
        ItemStack stack = event.getItemStack();
        Player player = event.getEntity();

        if (state.is(Blocks.GRINDSTONE) && GrindstoneFeed.accepts(stack)) {
            if (level instanceof ServerLevel serverLevel) {
                ItemStack dust = GrindstoneFeed.grind(serverLevel, pos, stack);
                stack.consume(1, player);
                if (!player.getInventory().add(dust))
                    player.drop(dust, false);
            }
            cancel(event, level);
            return;
        }

        if (state.is(Blocks.WATER_CAULDRON) && state.getValue(LayeredCauldronBlock.LEVEL) == LayeredCauldronBlock.MAX_FILL_LEVEL
                && FormItem.formOf(stack) == Form.DUST) {
            if (level instanceof ServerLevel serverLevel) {
                String material = FormItem.materialId(stack);
                stack.consume(1, player);
                SlurryCauldronBlock.fill(serverLevel, pos, material, RegistryHandler.SLURRY_CAULDRON.get().defaultBlockState());
            }
            cancel(event, level);
            return;
        }

        hintMistake(state, stack, player);
    }

    /** Explains the near misses: the right station with the wrong form, or the right form at the wrong station. */
    private static void hintMistake(BlockState state, ItemStack stack, Player player) {
        Form form = FormItem.formOf(stack);
        boolean raw = form == null && MaterialRegistry.current().inputFor(stack.getItem()) != null;
        if (state.is(Blocks.GRINDSTONE)) {
            if (raw)
                Hints.tell(player, "grind.raw");
            else if (form == Form.DUST || form == Form.SHARDS)
                Hints.tell(player, "grind.done");
        } else if (state.is(Blocks.CAULDRON) || state.is(Blocks.WATER_CAULDRON)) {
            if (form == Form.DUST)
                Hints.tell(player, "cauldron.not_full");
            else if (form == Form.CLUMPS || raw)
                Hints.tell(player, "cauldron.too_coarse");
        }
    }

    private static void cancel(PlayerInteractEvent.RightClickBlock event, Level level) {
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.sidedSuccess(level.isClientSide));
    }

    // -------------------------------------------------------------------------
    // Wash
    // -------------------------------------------------------------------------

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Post event) {
        // cheapest checks first: almost every entity leaves at the instanceof
        if (!(event.getEntity() instanceof ItemEntity item) || !item.isInWater())
            return;
        // every item in a Sluice is carried, so raw ore and byproducts move with the rest; only ladder forms wash
        // both sides: the client simulates the motion itself so the tumble animates smoothly
        SluiceBlock.handleItem(item.level(), item);
    }

    // -------------------------------------------------------------------------
    // Strike by falling heavy block
    // -------------------------------------------------------------------------

    @SubscribeEvent
    public static void onEntityLeave(EntityLeaveLevelEvent event) {
        if (!(event.getEntity() instanceof FallingBlockEntity falling) || !(event.getLevel() instanceof ServerLevel level))
            return;
        if (falling.getRemovalReason() != Entity.RemovalReason.DISCARDED)
            return;
        if (!falling.getBlockState().is(RegistryHandler.HEAVY_TAG))
            return;
        if (!OreProcessingConfig.get(OreProcessingConfig.COMMON.heavyBlockStrikes))
            return;

        BlockPos landed = falling.blockPosition();
        if (level.getBlockEntity(landed.below()) instanceof StrokeConsumer consumer)
            consumer.accept(new Stroke(landed, 1.0F));
    }

    // -------------------------------------------------------------------------
    // Datapack tables and tooltips
    // -------------------------------------------------------------------------

    @SubscribeEvent
    public static void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new LadderTables());
    }

    @SubscribeEvent
    public static void onDatapackSync(OnDatapackSyncEvent event) {
        event.getRelevantPlayers().forEach(Feedback::syncRatios);
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        MaterialRegistry.InputEntry entry = MaterialRegistry.current().inputFor(event.getItemStack().getItem());
        if (entry != null)
            FormItem.appendLadderLines(Form.RAW, entry.materialId(), event.getToolTip());
    }
}
