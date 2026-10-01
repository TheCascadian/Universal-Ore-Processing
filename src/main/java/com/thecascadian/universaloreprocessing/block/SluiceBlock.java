package com.thecascadian.universaloreprocessing.block;

import com.mojang.serialization.MapCodec;
import com.thecascadian.universaloreprocessing.config.OreProcessingConfig;
import com.thecascadian.universaloreprocessing.item.FormItem;
import com.thecascadian.universaloreprocessing.ladder.Form;
import com.thecascadian.universaloreprocessing.ladder.LadderTables;
import com.thecascadian.universaloreprocessing.network.Feedback;
import com.thecascadian.universaloreprocessing.registry.RegistryHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Optional;

/**
 * A low wooden slope with riffles, laid in waterlogged rows. It has no block
 * entity: items in it are slowed and carried downstream, and the item reaching
 * the last Sluice of a row is washed once. Washing is the only random step of the
 * ladder; longer rows raise each byproduct chance up to the configured cap.
 */
public class SluiceBlock extends HorizontalDirectionalBlock implements SimpleWaterloggedBlock {

    public static final MapCodec<SluiceBlock> CODEC = simpleCodec(SluiceBlock::new);
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    // facing points downstream; the shape steps down toward it
    private static final VoxelShape[] SHAPES = new VoxelShape[4];

    static {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            VoxelShape high = switch (direction) {
                case NORTH -> Block.box(0, 0, 8, 16, 6, 16);
                case SOUTH -> Block.box(0, 0, 0, 16, 6, 8);
                case WEST -> Block.box(8, 0, 0, 16, 6, 16);
                default -> Block.box(0, 0, 0, 8, 6, 16);
            };
            SHAPES[direction.get2DDataValue()] = Shapes.or(Block.box(0, 0, 0, 16, 3, 16), high);
        }
    }

    // items over a sluice lose this much horizontal speed per tick, so they settle against the riffles
    private static final double DRAG = 0.55D;
    // still water has no current, so the slope itself carries items downstream; settles near 0.045 blocks per tick
    private static final double CARRY = 0.02D;

    public SluiceBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(WATERLOGGED, false));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, WATERLOGGED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        boolean water = context.getLevel().getFluidState(context.getClickedPos()).getType() == Fluids.WATER;
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection())
                .setValue(WATERLOGGED, water);
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                     LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getValue(WATERLOGGED))
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(FACING).get2DDataValue()];
    }

    /**
     * Called from the item entity tick hook for ladder items that are in water.
     * Looks up the sluice under the item, applies drag and the downstream carry,
     * and washes the stack at the end of a row.
     */
    public static void handleItem(ServerLevel level, ItemEntity entity) {
        BlockPos pos = entity.blockPosition();
        BlockState state = level.getBlockState(pos);
        if (!state.is(RegistryHandler.SLUICE.get())) {
            pos = pos.below();
            state = level.getBlockState(pos);
            if (!state.is(RegistryHandler.SLUICE.get()))
                return;
        }

        Direction downstream = state.getValue(FACING);
        Vec3 motion = entity.getDeltaMovement();
        entity.setDeltaMovement(
                motion.x * DRAG + downstream.getStepX() * CARRY,
                motion.y,
                motion.z * DRAG + downstream.getStepZ() * CARRY);

        if (level.getBlockState(pos.relative(downstream)).is(RegistryHandler.SLUICE.get()))
            return;

        ItemStack stack = entity.getItem();
        Form form = FormItem.formOf(stack);
        if (form == null || !form.washable() || FormItem.isWashed(stack))
            return;

        int row = rowLength(level, pos, downstream);
        wash(level, entity, stack, row);
    }

    private static int rowLength(ServerLevel level, BlockPos end, Direction downstream) {
        int max = OreProcessingConfig.get(OreProcessingConfig.COMMON.sluiceMaxRow);
        int length = 1;
        BlockPos cursor = end.relative(downstream.getOpposite());
        while (length < max) {
            BlockState state = level.getBlockState(cursor);
            if (!state.is(RegistryHandler.SLUICE.get()) || state.getValue(FACING) != downstream)
                break;
            length++;
            cursor = cursor.relative(downstream.getOpposite());
        }
        return length;
    }

    private static void wash(ServerLevel level, ItemEntity entity, ItemStack stack, int row) {
        String material = FormItem.materialId(stack);
        double cap = OreProcessingConfig.get(OreProcessingConfig.COMMON.sluiceByproductCap);
        RandomSource random = level.getRandom();

        ItemStack washed = stack.copy();
        washed.set(RegistryHandler.WASHED_COMPONENT.get(), true);
        entity.setItem(washed);

        for (LadderTables.Byproduct byproduct : LadderTables.byproductsFor(material)) {
            Optional<Item> item = resolve(byproduct);
            if (item.isEmpty())
                continue;
            double chance = Math.min(cap, byproduct.chance() * row);
            int count = 0;
            for (int i = 0; i < stack.getCount(); i++) {
                if (random.nextDouble() < chance)
                    count++;
            }
            while (count > 0) {
                ItemStack out = new ItemStack(item.get(), Math.min(count, item.get().getDefaultMaxStackSize()));
                count -= out.getCount();
                ItemEntity spawned = new ItemEntity(level, entity.getX(), entity.getY(), entity.getZ(), out);
                spawned.setDeltaMovement(entity.getDeltaMovement());
                level.addFreshEntity(spawned);
            }
        }
        Feedback.play(level, entity.position(), Feedback.Verb.WASH, material);
    }

    private static Optional<Item> resolve(LadderTables.Byproduct byproduct) {
        if (!byproduct.tag()) {
            Item item = BuiltInRegistries.ITEM.get(byproduct.id());
            return item == Items.AIR ? Optional.empty() : Optional.of(item);
        }
        TagKey<Item> tag = TagKey.create(Registries.ITEM, byproduct.id());
        return BuiltInRegistries.ITEM.getTag(tag)
                .flatMap(set -> set.stream().findFirst())
                .map(Holder::value);
    }
}
