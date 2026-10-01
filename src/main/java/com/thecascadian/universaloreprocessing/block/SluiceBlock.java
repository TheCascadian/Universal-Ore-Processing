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
import net.minecraft.world.level.Level;
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
 * A flat wooden box with riffles across its floor, laid in waterlogged rows.
 * It has no block entity: ladder items sink to the floor, crawl downstream and
 * tumble over each riffle, and the item clearing the last riffle of a row is
 * washed once. The motion runs on both sides so the client animates it
 * smoothly; only the wash runs on the server. Washing is the only random step of the
 * ladder; longer rows raise each byproduct chance up to the configured cap.
 */
public class SluiceBlock extends HorizontalDirectionalBlock implements SimpleWaterloggedBlock {

    public static final MapCodec<SluiceBlock> CODEC = simpleCodec(SluiceBlock::new);
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    // facing points downstream; the floor is level and the rails run along the row
    private static final VoxelShape[] SHAPES = new VoxelShape[4];

    static {
        VoxelShape floor = Block.box(0, 0, 0, 16, 3, 16);
        VoxelShape alongZ = Shapes.or(floor, Block.box(0, 3, 0, 1, 8, 16), Block.box(15, 3, 0, 16, 8, 16));
        VoxelShape alongX = Shapes.or(floor, Block.box(0, 3, 0, 16, 8, 1), Block.box(0, 3, 15, 16, 8, 16));
        for (Direction direction : Direction.Plane.HORIZONTAL)
            SHAPES[direction.get2DDataValue()] = direction.getAxis() == Direction.Axis.Z ? alongZ : alongX;
    }

    // upstream face of each riffle, as a fraction of the block measured downstream; mirrors the block model
    private static final double[] RIFFLES = {3.0D / 16.0D, 8.0D / 16.0D, 13.0D / 16.0D};
    // half the width of a dropped item; its leading edge meets a riffle this far ahead of its centre
    private static final double LEAD = 0.125D;

    // items over a sluice lose this much speed along the row per tick
    private static final double DRAG = 0.55D;
    // still water has no current, so the box itself carries items downstream; settles near 0.045 blocks per tick
    private static final double CARRY = 0.02D;
    // water spilling off the sides of a row pushes items outward; this pulls them back to the centre line
    private static final double CENTRE = 0.15D;
    // ore is heavy: it sinks onto the floor rather than floating, and a riffle kicks it up about two pixels
    private static final double SINK = 0.04D;
    private static final double HOP = 0.075D;
    private static final double FALL = 0.035D;
    // ticks between puffs of silt behind an item crawling along the floor
    private static final int SILT_INTERVAL = 5;

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
     * Called on both sides from the item entity tick hook for ladder items that
     * are in water. Moves the item along the sluice under it, tumbles it over
     * the riffles, and on the server washes it at the end of a row.
     */
    public static void handleItem(Level level, ItemEntity entity) {
        BlockPos pos = entity.blockPosition();
        BlockState state = level.getBlockState(pos);
        if (!state.is(RegistryHandler.SLUICE.get())) {
            pos = pos.below();
            state = level.getBlockState(pos);
            if (!state.is(RegistryHandler.SLUICE.get()))
                return;
        }

        Direction downstream = state.getValue(FACING);
        String material = FormItem.materialId(entity.getItem());

        // leading edge of the item, now and last tick, as a fraction of this block measured downstream
        double start = along(downstream, pos.getX() + 0.5D, pos.getZ() + 0.5D) - 0.5D;
        double front = along(downstream, entity.getX(), entity.getZ()) - start + LEAD;
        double previous = along(downstream, entity.xo, entity.zo) - start + LEAD;
        boolean tumble = false;
        for (double riffle : RIFFLES) {
            if (previous < riffle && front >= riffle)
                tumble = true;
        }

        Vec3 motion = entity.getDeltaMovement();
        double vertical;
        if (tumble)
            vertical = HOP;
        else if (entity.onGround())
            vertical = -SINK;
        else
            vertical = Math.max(motion.y - FALL, -SINK * 2.0D);

        // the sluice owns the motion: along the row it slows and carries, across it it centres
        double alongSpeed = (motion.x * downstream.getStepX() + motion.z * downstream.getStepZ()) * DRAG + CARRY;
        if (downstream.getAxis() == Direction.Axis.Z) {
            double across = (pos.getX() + 0.5D - entity.getX()) * CENTRE;
            entity.setDeltaMovement(across, vertical, alongSpeed * downstream.getStepZ());
        } else {
            double across = (pos.getZ() + 0.5D - entity.getZ()) * CENTRE;
            entity.setDeltaMovement(alongSpeed * downstream.getStepX(), vertical, across);
        }

        if (level.isClientSide()) {
            Vec3 at = entity.position();
            if (tumble)
                Feedback.playLocal(level, at.add(0.0D, 0.1D, 0.0D), Feedback.Verb.RIFFLE, material);
            else if (entity.onGround() && entity.tickCount % SILT_INTERVAL == 0)
                Feedback.playLocal(level,
                        at.add(-downstream.getStepX() * LEAD, 0.0D, -downstream.getStepZ() * LEAD),
                        Feedback.Verb.SILT, material);
            return;
        }

        // the wash happens as the item clears the last riffle of the last Sluice in the row
        BlockState next = level.getBlockState(pos.relative(downstream));
        if (next.is(RegistryHandler.SLUICE.get()) && next.getValue(FACING) == downstream)
            return;
        if (front < RIFFLES[RIFFLES.length - 1])
            return;

        ItemStack stack = entity.getItem();
        Form form = FormItem.formOf(stack);
        if (form == null || !form.washable() || FormItem.isWashed(stack))
            return;

        int row = rowLength(level, pos, downstream);
        wash((ServerLevel) level, entity, stack, row);
    }

    private static double along(Direction downstream, double x, double z) {
        return x * downstream.getStepX() + z * downstream.getStepZ();
    }

    private static int rowLength(Level level, BlockPos end, Direction downstream) {
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
