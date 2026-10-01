package com.thecascadian.universaloreprocessing.block;

import com.mojang.serialization.MapCodec;
import com.thecascadian.universaloreprocessing.config.OreProcessingConfig;
import com.thecascadian.universaloreprocessing.guide.Hints;
import com.thecascadian.universaloreprocessing.item.FormItem;
import com.thecascadian.universaloreprocessing.ladder.Form;
import com.thecascadian.universaloreprocessing.network.Feedback;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.Tags;

/**
 * A hooped wooden vat: the bulk form of the slurry cauldron. It is filled
 * with a water bucket, takes up to a stack of one dust, is stirred like a
 * cauldron, and settles through the same three visible stages. Hoppers may
 * add dust from above and take the shards out from below.
 *
 * <p>Stages: 0 empty, 1 water, 2 murky, 3 stirred, 4 and 5 settling, 6 ready.
 */
public class SettlingTankBlock extends BaseEntityBlock {

    public static final MapCodec<SettlingTankBlock> CODEC = simpleCodec(SettlingTankBlock::new);
    public static final int EMPTY = 0;
    public static final int WATER = 1;
    public static final int MURKY = 2;
    public static final int STIRRED = 3;
    public static final int READY = 6;
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", EMPTY, READY);
    public static final int CAPACITY = 64;

    private static final VoxelShape INSIDE = box(2, 2, 2, 14, 16, 14);
    private static final VoxelShape SHAPE = Shapes.join(Shapes.block(), INSIDE, BooleanOp.ONLY_FIRST);

    public SettlingTankBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(STAGE, EMPTY));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STAGE);
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
    protected VoxelShape getInteractionShape(BlockState state, BlockGetter level, BlockPos pos) {
        return INSIDE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SettlingTankBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof SettlingTankBlockEntity tank))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        int stage = state.getValue(STAGE);

        if (stage == READY) {
            if (level instanceof ServerLevel serverLevel)
                harvest(serverLevel, pos, player, tank);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        if (stack.is(Items.WATER_BUCKET) && stage == EMPTY) {
            if (!level.isClientSide) {
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.BUCKET)));
                level.setBlockAndUpdate(pos, state.setValue(STAGE, WATER));
                level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (stack.is(Items.BUCKET) && stage == WATER) {
            if (!level.isClientSide) {
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.WATER_BUCKET)));
                level.setBlockAndUpdate(pos, state.setValue(STAGE, EMPTY));
                level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }

        Form form = FormItem.formOf(stack);
        boolean stick = stack.is(Tags.Items.RODS_WOODEN);
        if (stage > MURKY) {
            if (form != null || stick)
                Hints.tell(player, "slurry.settling", stage - STIRRED, READY - STIRRED);
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (form == Form.DUST) {
            if (stage == EMPTY) {
                Hints.tell(player, "tank.no_water");
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
            if (!tank.canTake(stack)) {
                Hints.tell(player, tank.count() >= CAPACITY ? "tank.full" : "slurry.other_material",
                        FormItem.materialName(tank.material()), FormItem.materialName(FormItem.materialId(stack)));
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
            if (level instanceof ServerLevel serverLevel)
                stack.setCount(addDust(serverLevel, pos, tank, stack, false).getCount());
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (form != null) {
            Hints.tell(player, "slurry.wrong_form");
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (stick) {
            if (level instanceof ServerLevel serverLevel && !stir(serverLevel, pos, player))
                Hints.tell(player, stage == EMPTY ? "tank.no_water" : "tank.no_dust");
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        int stage = state.getValue(STAGE);
        if (stage != READY || !(level.getBlockEntity(pos) instanceof SettlingTankBlockEntity tank)) {
            if (stage == EMPTY)
                Hints.tell(player, "tank.no_water");
            else if (stage == WATER)
                Hints.tell(player, "tank.no_dust");
            else if (stage == MURKY)
                Hints.tell(player, "slurry.needs_stir");
            else
                Hints.tell(player, "slurry.settling", stage - STIRRED, READY - STIRRED);
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel serverLevel)
            harvest(serverLevel, pos, player, tank);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    /** Adds dust while the tank holds water or unstirred slurry; returns what did not fit. */
    static ItemStack addDust(ServerLevel level, BlockPos pos, SettlingTankBlockEntity tank, ItemStack stack,
            boolean simulate) {
        BlockState state = level.getBlockState(pos);
        int stage = state.getValue(STAGE);
        if ((stage != WATER && stage != MURKY) || !tank.canTake(stack))
            return stack;
        int moved = Math.min(CAPACITY - tank.count(), stack.getCount());
        if (!simulate) {
            tank.add(FormItem.materialId(stack), moved);
            if (stage == WATER)
                level.setBlockAndUpdate(pos, state.setValue(STAGE, MURKY));
            Feedback.play(level, surface(pos), Feedback.Verb.STIR, tank.material());
        }
        return stack.copyWithCount(stack.getCount() - moved);
    }

    /** One stir, by stick or Stirring Paddle; returns false when there is no unstirred slurry. */
    public static boolean stir(ServerLevel level, BlockPos pos, Player player) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof SettlingTankBlock block) || state.getValue(STAGE) != MURKY
                || !(level.getBlockEntity(pos) instanceof SettlingTankBlockEntity tank))
            return false;
        Feedback.play(level, surface(pos), Feedback.Verb.STIR, tank.material());
        int needed = OreProcessingConfig.get(OreProcessingConfig.COMMON.stirsPerSlurry);
        int stirs = tank.stir();
        if (stirs >= needed) {
            level.setBlockAndUpdate(pos, state.setValue(STAGE, STIRRED));
            level.scheduleTick(pos, block, SlurryCauldronBlock.stageTicks());
        } else if (player != null) {
            Hints.tell(player, "slurry.stirred", stirs, needed);
        }
        return true;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int stage = state.getValue(STAGE);
        if (stage < STIRRED || stage == READY)
            return;
        level.setBlockAndUpdate(pos, state.setValue(STAGE, stage + 1));
        if (level.getBlockEntity(pos) instanceof SettlingTankBlockEntity tank) {
            Feedback.play(level, surface(pos), Feedback.Verb.SETTLE, tank.material());
            if (stage + 1 == READY)
                tank.settleClay(level);
        }
        if (stage + 1 < READY)
            level.scheduleTick(pos, this, SlurryCauldronBlock.stageTicks());
    }

    private static void harvest(ServerLevel level, BlockPos pos, Player player, SettlingTankBlockEntity tank) {
        String material = tank.material();
        ItemStack shards = tank.takeShards(tank.count());
        ItemStack clay = tank.takeClay(Integer.MAX_VALUE);
        for (ItemStack out : new ItemStack[] {shards, clay}) {
            if (!out.isEmpty() && !player.getInventory().add(out))
                player.drop(out, false);
        }
        Feedback.play(level, surface(pos), Feedback.Verb.SETTLE, material);
    }

    /** Called by the block entity when the last shard and clay have been taken out. */
    static void emptied(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof SettlingTankBlock)
            level.setBlockAndUpdate(pos, state.setValue(STAGE, EMPTY));
    }

    static Vec3 surface(BlockPos pos) {
        return Vec3.atLowerCornerOf(pos).add(0.5D, 14.0D / 16.0D, 0.5D);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        // dust and slurry are lost with the water, as in a cauldron; finished shards are not
        if (!state.is(newState.getBlock()) && state.getValue(STAGE) == READY
                && level.getBlockEntity(pos) instanceof SettlingTankBlockEntity tank) {
            Block.popResource(level, pos, tank.takeShards(tank.count()));
            Block.popResource(level, pos, tank.takeClay(Integer.MAX_VALUE));
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
