package com.thecascadian.universaloreprocessing.item;

import com.thecascadian.universaloreprocessing.block.SluiceBlock;
import com.thecascadian.universaloreprocessing.guide.Hints;
import com.thecascadian.universaloreprocessing.ladder.Form;
import com.thecascadian.universaloreprocessing.network.Feedback;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * The hand version of the Sluice. Standing in water with unwashed clumps or
 * dust in the other hand, hold use to swirl the tray; when the pan finishes,
 * the stack is washed as by a row of one Sluice.
 */
public class PanningTrayItem extends Item {

    private static final int PAN_TICKS = 32;

    public PanningTrayItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack tray = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND)
            return InteractionResultHolder.pass(tray);
        String problem = problem(player);
        if (problem != null) {
            Hints.tell(player, problem);
            return InteractionResultHolder.fail(tray);
        }
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(tray);
    }

    /** The hint key for why panning cannot start, or null when it can. */
    private static String problem(Player player) {
        if (!player.isInWater())
            return "tray.no_water";
        ItemStack load = player.getOffhandItem();
        Form form = FormItem.formOf(load);
        if (form == null || !form.washable())
            return "tray.no_load";
        if (FormItem.isWashed(load))
            return "tray.washed";
        return null;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return PAN_TICKS;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BRUSH;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (!(entity instanceof Player player) || !level.isClientSide || remaining % 4 != 0)
            return;
        // splashes swirl round the tray in front of the player
        double angle = remaining * 0.8D;
        Vec3 front = player.getEyePosition().add(player.getLookAngle().scale(0.6D)).add(0.0D, -0.5D, 0.0D);
        level.addParticle(ParticleTypes.SPLASH, front.x + Math.cos(angle) * 0.2D, front.y, front.z + Math.sin(angle) * 0.2D,
                0.0D, 0.05D, 0.0D);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (!(level instanceof ServerLevel serverLevel) || !(entity instanceof Player player) || problem(player) != null)
            return stack;
        ItemStack load = player.getOffhandItem();
        String material = FormItem.materialId(load);
        player.setItemInHand(InteractionHand.OFF_HAND, SluiceBlock.washed(load));
        for (ItemStack byproduct : SluiceBlock.byproducts(serverLevel, material, load.getCount(), 1)) {
            if (!player.getInventory().add(byproduct))
                player.drop(byproduct, false);
        }
        Feedback.play(serverLevel, player.position().add(0.0D, 0.5D, 0.0D), Feedback.Verb.PAN, material);
        stack.hurtAndBreak(1, player, LivingEntity.getSlotForHand(InteractionHand.MAIN_HAND));
        return stack;
    }
}
