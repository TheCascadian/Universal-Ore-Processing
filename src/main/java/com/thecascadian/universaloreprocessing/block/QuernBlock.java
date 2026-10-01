package com.thecascadian.universaloreprocessing.block;

import com.mojang.serialization.MapCodec;
import com.thecascadian.universaloreprocessing.guide.Hints;
import com.thecascadian.universaloreprocessing.item.FormItem;
import com.thecascadian.universaloreprocessing.ladder.Form;
import com.thecascadian.universaloreprocessing.material.MaterialRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A hand mill: a stone base with a spout and a turning top stone with a peg.
 * Clumps go in from the top (by hand or hopper); each turn, by an empty hand
 * or a redstone pulse, grinds one clump into one dust, which leaves through
 * the spout into a container below or onto the ground in front. The peg moves
 * a quarter turn each time, so the stone is seen to turn.
 */
public class QuernBlock extends BaseEntityBlock {

    public static final MapCodec<QuernBlock> CODEC = simpleCodec(QuernBlock::new);
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final IntegerProperty TURN = IntegerProperty.create("turn", 0, 3);
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    private static final VoxelShape SHAPE = Shapes.or(Block.box(1, 0, 1, 15, 8, 15), Block.box(2, 8, 2, 14, 13, 14));

    public QuernBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(TURN, 0)
                .setValue(POWERED, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, TURN, POWERED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // the spout faces the player who placed it
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new QuernBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof QuernBlockEntity quern))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;

        Form form = FormItem.formOf(stack);
        if (form == Form.CLUMPS) {
            if (!quern.canTake(stack)) {
                Hints.tell(player, "quern.other_material", quern.contents().getHoverName());
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
            if (!level.isClientSide)
                stack.setCount(quern.insert(stack, false).getCount());
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (form == Form.DUST || form == Form.SHARDS)
            Hints.tell(player, "grind.done");
        else if (form == null && MaterialRegistry.current().inputFor(stack.getItem()) != null)
            Hints.tell(player, "grind.raw");
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof QuernBlockEntity quern))
            return InteractionResult.PASS;
        if (quern.contents().isEmpty()) {
            Hints.tell(player, "quern.empty");
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel serverLevel)
            turn(serverLevel, pos, state, quern);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, BlockPos neighborPos,
            boolean movedByPiston) {
        if (!(level instanceof ServerLevel serverLevel))
            return;
        boolean powered = level.hasNeighborSignal(pos);
        if (powered == state.getValue(POWERED))
            return;
        level.setBlock(pos, state.setValue(POWERED, powered), Block.UPDATE_CLIENTS);
        if (powered && level.getBlockEntity(pos) instanceof QuernBlockEntity quern && !quern.contents().isEmpty())
            turn(serverLevel, pos, level.getBlockState(pos), quern);
    }

    private static void turn(ServerLevel level, BlockPos pos, BlockState state, QuernBlockEntity quern) {
        level.setBlock(pos, state.setValue(TURN, (state.getValue(TURN) + 1) % 4), Block.UPDATE_CLIENTS);
        quern.grindOne(state.getValue(FACING));
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof QuernBlockEntity quern)
            Block.popResource(level, pos, quern.takeAll());
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
