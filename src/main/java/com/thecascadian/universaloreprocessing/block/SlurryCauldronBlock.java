package com.thecascadian.universaloreprocessing.block;

import com.mojang.serialization.MapCodec;
import com.thecascadian.universaloreprocessing.config.OreProcessingConfig;
import com.thecascadian.universaloreprocessing.guide.Hints;
import com.thecascadian.universaloreprocessing.item.FormItem;
import com.thecascadian.universaloreprocessing.ladder.Form;
import com.thecascadian.universaloreprocessing.ladder.Waste;
import com.thecascadian.universaloreprocessing.network.Feedback;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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
 * A water cauldron holding slurry. The vanilla cauldron is never replaced:
 * dropping dust into a full water cauldron turns it into this block, and
 * harvesting the shards turns it back into an empty vanilla cauldron.
 *
 * <p>Phases: 0 murky (dust added, unstirred), 1 stirred, 2 to 4 the three
 * visible settling stages; 4 is ready to harvest. Settling uses scheduled
 * block ticks, so nothing runs while the slurry waits.
 */
public class SlurryCauldronBlock extends BaseEntityBlock {

    public static final MapCodec<SlurryCauldronBlock> CODEC = simpleCodec(SlurryCauldronBlock::new);
    public static final int READY = 4;
    public static final IntegerProperty PHASE = IntegerProperty.create("phase", 0, READY);
    public static final int CAPACITY = 8;

    // same outline as the vanilla cauldron
    private static final VoxelShape INSIDE = box(2.0D, 4.0D, 2.0D, 14.0D, 16.0D, 14.0D);
    private static final VoxelShape SHAPE = Shapes.join(Shapes.block(), Shapes.or(
            box(0.0D, 0.0D, 4.0D, 16.0D, 3.0D, 12.0D),
            box(4.0D, 0.0D, 0.0D, 12.0D, 3.0D, 16.0D),
            box(2.0D, 0.0D, 2.0D, 14.0D, 3.0D, 14.0D),
            INSIDE), BooleanOp.ONLY_FIRST);

    public SlurryCauldronBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(PHASE, 0));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(PHASE);
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
        return new SlurryCauldronBlockEntity(pos, state);
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return new ItemStack(Items.CAULDRON);
    }

    /** Turns a full vanilla water cauldron into slurry holding one dust. */
    public static void fill(ServerLevel level, BlockPos pos, String material, BlockState slurry) {
        level.setBlockAndUpdate(pos, slurry);
        if (level.getBlockEntity(pos) instanceof SlurryCauldronBlockEntity cauldron)
            cauldron.start(material);
        Feedback.play(level, surface(pos), Feedback.Verb.STIR, material);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof SlurryCauldronBlockEntity cauldron))
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        int phase = state.getValue(PHASE);

        if (phase == READY) {
            if (level instanceof ServerLevel serverLevel)
                harvest(serverLevel, pos, player, cauldron);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        Form form = FormItem.formOf(stack);
        boolean stick = stack.is(Tags.Items.RODS_WOODEN);
        if (phase != 0) {
            if (form != null || stick)
                Hints.tell(player, "slurry.settling", phase - 1, READY - 1);
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (form == Form.DUST) {
            String material = FormItem.materialId(stack);
            if (!material.equals(cauldron.material())) {
                Hints.tell(player, "slurry.other_material", FormItem.materialName(cauldron.material()),
                        FormItem.materialName(material));
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
            if (cauldron.count() >= CAPACITY) {
                Hints.tell(player, "slurry.full", CAPACITY);
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
            if (level instanceof ServerLevel serverLevel) {
                cauldron.addDust();
                stack.consume(1, player);
                Feedback.play(serverLevel, surface(pos), Feedback.Verb.STIR, cauldron.material());
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (form != null) {
            Hints.tell(player, "slurry.wrong_form");
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (stick) {
            if (level instanceof ServerLevel serverLevel)
                stir(serverLevel, pos, player);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    /**
     * One stir of the unstirred slurry at {@code pos}. Used by a stick and by a
     * Stirring Paddle; {@code player} is null for the paddle. Returns false when
     * there is nothing to stir.
     */
    public static boolean stir(ServerLevel level, BlockPos pos, Player player) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof SlurryCauldronBlock block) || state.getValue(PHASE) != 0
                || !(level.getBlockEntity(pos) instanceof SlurryCauldronBlockEntity cauldron))
            return false;
        Feedback.play(level, surface(pos), Feedback.Verb.STIR, cauldron.material());
        int needed = OreProcessingConfig.get(OreProcessingConfig.COMMON.stirsPerSlurry);
        int stirs = cauldron.stir();
        if (stirs >= needed) {
            level.setBlockAndUpdate(pos, state.setValue(PHASE, 1));
            level.scheduleTick(pos, block, stageTicks());
        } else if (player != null) {
            Hints.tell(player, "slurry.stirred", stirs, needed);
        }
        return true;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
            BlockHitResult hit) {
        int phase = state.getValue(PHASE);
        if (phase != READY || !(level.getBlockEntity(pos) instanceof SlurryCauldronBlockEntity cauldron)) {
            if (phase == 0)
                Hints.tell(player, "slurry.needs_stir");
            else
                Hints.tell(player, "slurry.settling", phase - 1, READY - 1);
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel serverLevel)
            harvest(serverLevel, pos, player, cauldron);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int phase = state.getValue(PHASE);
        if (phase == 0 || phase == READY)
            return;
        level.setBlockAndUpdate(pos, state.setValue(PHASE, phase + 1));
        if (level.getBlockEntity(pos) instanceof SlurryCauldronBlockEntity cauldron)
            Feedback.play(level, surface(pos), Feedback.Verb.SETTLE, cauldron.material());
        if (phase + 1 < READY)
            level.scheduleTick(pos, this, stageTicks());
    }

    private static void harvest(ServerLevel level, BlockPos pos, Player player, SlurryCauldronBlockEntity cauldron) {
        String material = cauldron.material();
        ItemStack shards = FormItem.create(Form.SHARDS, material, cauldron.count());
        // the fine rock settles under the crystals and comes out with them as clay
        ItemStack clay = Waste.CLAY.add(level, pos, cauldron.count());
        level.setBlockAndUpdate(pos, Blocks.CAULDRON.defaultBlockState());
        for (ItemStack out : new ItemStack[] {shards, clay}) {
            if (!out.isEmpty() && !player.getInventory().add(out))
                player.drop(out, false);
        }
        Feedback.play(level, surface(pos), Feedback.Verb.SETTLE, material);
    }

    static int stageTicks() {
        // stirred, then three visible stages: the configured time is split across those three steps
        return Math.max(1, OreProcessingConfig.get(OreProcessingConfig.COMMON.settleTicks) / 3);
    }

    static Vec3 surface(BlockPos pos) {
        return Vec3.atLowerCornerOf(pos).add(0.5D, 15.0D / 16.0D, 0.5D);
    }
}
