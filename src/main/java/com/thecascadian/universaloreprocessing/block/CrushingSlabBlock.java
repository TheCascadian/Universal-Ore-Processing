package com.thecascadian.universaloreprocessing.block;

import com.mojang.serialization.MapCodec;
import com.thecascadian.universaloreprocessing.guide.Hints;
import com.thecascadian.universaloreprocessing.item.FormItem;
import com.thecascadian.universaloreprocessing.ladder.Form;
import com.thecascadian.universaloreprocessing.material.MaterialRegistry;
import com.thecascadian.universaloreprocessing.registry.RegistryHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A heavy stone basin. Right-click places one crushable item on its floor,
 * a hammer strikes it, and the last strikes before it breaks show as crack
 * stages on the basin. The full-height collision lets falling blocks land on
 * top and deliver their own strike.
 */
public class CrushingSlabBlock extends BaseEntityBlock {

    public static final MapCodec<CrushingSlabBlock> CODEC = simpleCodec(CrushingSlabBlock::new);
    public static final int CRACK_STAGES = 4;
    public static final IntegerProperty CRACK = IntegerProperty.create("crack", 0, CRACK_STAGES);

    public CrushingSlabBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(CRACK, 0));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CRACK);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrushingSlabBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof CrushingSlabBlockEntity slab))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        if (stack.is(RegistryHandler.HAMMERS_TAG)) {
            if (slab.isEmpty()) {
                Hints.tell(player, "slab.empty");
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
            if (level instanceof ServerLevel serverLevel) {
                slab.strike(serverLevel);
                stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        if (MaterialRegistry.current().inputFor(stack.getItem()) != null) {
            if (!slab.isEmpty()) {
                Hints.tell(player, "slab.occupied", slab.item().getHoverName());
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
            if (!level.isClientSide) {
                slab.place(stack.copyWithCount(1));
                stack.consume(1, player);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        Form form = FormItem.formOf(stack);
        if (form != null)
            Hints.tell(player, "slab.already_crushed", Component.translatable(form.nextKey()));
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof CrushingSlabBlockEntity slab) || slab.isEmpty())
            return InteractionResult.PASS;
        if (!level.isClientSide) {
            ItemStack taken = slab.take();
            if (!player.getInventory().add(taken))
                player.drop(taken, false);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof CrushingSlabBlockEntity slab)
            Block.popResource(level, pos, slab.take());
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
