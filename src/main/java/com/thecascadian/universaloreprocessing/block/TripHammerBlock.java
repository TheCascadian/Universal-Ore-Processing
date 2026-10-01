package com.thecascadian.universaloreprocessing.block;

import com.mojang.serialization.MapCodec;
import com.thecascadian.universaloreprocessing.api.stroke.Stroke;
import com.thecascadian.universaloreprocessing.api.stroke.StrokeConsumer;
import com.thecascadian.universaloreprocessing.guide.Hints;
import com.thecascadian.universaloreprocessing.network.Feedback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A timber frame with a weighted head, set on top of a Crushing Slab. Each
 * rising redstone edge drops the head, which delivers one stroke to whatever
 * stroke consumer sits below, and a scheduled tick lifts it again. It holds
 * nothing and has no block entity.
 */
public class TripHammerBlock extends HorizontalDirectionalBlock {

    public static final MapCodec<TripHammerBlock> CODEC = simpleCodec(TripHammerBlock::new);
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    // the head is down for a few ticks after each blow, so the drop is visible
    public static final BooleanProperty DOWN = BooleanProperty.create("down");

    private static final int LIFT_TICKS = 4;

    // posts along the east and west edges, a beam across the top, and the head in its current position
    private static final VoxelShape FRAME_X = Shapes.or(Block.box(0, 0, 6, 2, 16, 10), Block.box(14, 0, 6, 16, 16, 10),
            Block.box(0, 14, 6, 16, 16, 10));
    private static final VoxelShape FRAME_Z = Shapes.or(Block.box(6, 0, 0, 10, 16, 2), Block.box(6, 0, 14, 10, 16, 16),
            Block.box(6, 14, 0, 10, 16, 16));
    private static final VoxelShape HEAD_UP = Block.box(4, 8, 4, 12, 13, 12);
    private static final VoxelShape HEAD_DOWN = Block.box(4, 0, 4, 12, 5, 12);

    public TripHammerBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(POWERED, false)
                .setValue(DOWN, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED, DOWN);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection())
                .setValue(POWERED, context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // the frame posts sit on the left and right of the player who placed it
        VoxelShape frame = state.getValue(FACING).getAxis() == Direction.Axis.Z ? FRAME_X : FRAME_Z;
        return Shapes.or(frame, state.getValue(DOWN) ? HEAD_DOWN : HEAD_UP);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        if (placer instanceof Player player && !(level.getBlockEntity(pos.below()) instanceof StrokeConsumer))
            Hints.tell(player, "trip_hammer.no_slab");
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
        if (powered) {
            next = next.setValue(DOWN, true);
            strike(serverLevel, pos);
            level.scheduleTick(pos, this, LIFT_TICKS);
        }
        level.setBlock(pos, next, Block.UPDATE_CLIENTS);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(DOWN))
            level.setBlock(pos, state.setValue(DOWN, false), Block.UPDATE_CLIENTS);
    }

    private static void strike(ServerLevel level, BlockPos pos) {
        if (level.getBlockEntity(pos.below()) instanceof StrokeConsumer consumer
                && consumer.accept(new Stroke(pos, 1.0F)))
            return;
        // nothing to strike: the head lands on bare stone or wood
        Feedback.play(level, Vec3.atBottomCenterOf(pos), Feedback.Verb.THUD, null);
    }
}
