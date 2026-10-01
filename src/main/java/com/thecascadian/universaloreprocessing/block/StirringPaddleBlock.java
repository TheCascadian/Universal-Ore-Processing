package com.thecascadian.universaloreprocessing.block;

import com.mojang.serialization.MapCodec;
import com.thecascadian.universaloreprocessing.guide.Hints;
import com.thecascadian.universaloreprocessing.network.Feedback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A crossbar resting on the rim of a cauldron or Settling Tank, with a paddle
 * hanging into it. A redstone pulse or an empty hand gives one stir; the
 * paddle swings to the other side each time.
 */
public class StirringPaddleBlock extends HorizontalDirectionalBlock {

    public static final MapCodec<StirringPaddleBlock> CODEC = simpleCodec(StirringPaddleBlock::new);
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty SWING = BooleanProperty.create("swing");

    private static final VoxelShape BAR_X = Shapes.or(Block.box(0, 0, 6, 16, 4, 10), Block.box(7, 4, 7, 9, 8, 9));
    private static final VoxelShape BAR_Z = Shapes.or(Block.box(6, 0, 0, 10, 4, 16), Block.box(7, 4, 7, 9, 8, 9));

    public StirringPaddleBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, false)
                .setValue(SWING, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED, SWING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection())
                .setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FACING).getAxis() == Direction.Axis.Z ? BAR_X : BAR_Z;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (placer instanceof Player player && !isVessel(level.getBlockState(pos.below())))
            Hints.tell(player, "paddle.no_vessel");
    }

    private static boolean isVessel(BlockState state) {
        return state.getBlock() instanceof SlurryCauldronBlock || state.getBlock() instanceof SettlingTankBlock
                || state.is(Blocks.CAULDRON) || state.is(Blocks.WATER_CAULDRON);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        if (level instanceof ServerLevel serverLevel && !stir(serverLevel, pos, state, player))
            Hints.tell(player, "paddle.nothing");
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
        BlockState next = state.setValue(POWERED, powered);
        level.setBlock(pos, next, Block.UPDATE_CLIENTS);
        if (powered)
            stir(serverLevel, pos, next, null);
    }

    /** Swings the paddle and stirs whatever vessel is below; returns false when it holds nothing to stir. */
    private static boolean stir(ServerLevel level, BlockPos pos, BlockState state, Player player) {
        level.setBlock(pos, state.setValue(SWING, !state.getValue(SWING)), Block.UPDATE_CLIENTS);
        BlockPos below = pos.below();
        if (SlurryCauldronBlock.stir(level, below, player) || SettlingTankBlock.stir(level, below, player))
            return true;
        Feedback.play(level, Vec3.atBottomCenterOf(pos), Feedback.Verb.THUD, null);
        return false;
    }
}
