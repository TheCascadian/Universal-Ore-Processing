package com.thecascadian.universaloreprocessing.block;

import com.thecascadian.universaloreprocessing.config.OreProcessingConfig;
import com.thecascadian.universaloreprocessing.process.Hazard;
import com.thecascadian.universaloreprocessing.process.Plan;
import com.thecascadian.universaloreprocessing.process.ProcessRules;
import com.thecascadian.universaloreprocessing.recipe.Yields;
import com.thecascadian.universaloreprocessing.registry.RegistryHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;
import net.neoforged.neoforge.items.wrapper.RangedWrapper;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Block entity shared by every machine. Holds an input, output, fuel, reagent and
 * byproduct slot, optional water tank and FE buffer, and advances a progress
 * counter while the station has a valid plan, its reagent and water, and power.
 */
public class MachineBlockEntity extends BlockEntity implements MenuProvider {

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int SLOT_FUEL = 2;
    public static final int SLOT_REAGENT = 3;
    public static final int SLOT_BYPRODUCT = 4;
    public static final int SLOT_COUNT = 5;

    private static final int HAZARD_INTERVAL = 40;

    public static final int DATA_PROGRESS = 0;
    public static final int DATA_MAX_PROGRESS = 1;
    public static final int DATA_BURN_TIME = 2;
    public static final int DATA_BURN_DURATION = 3;
    public static final int DATA_FLUID_PERMILLE = 4;
    public static final int DATA_ENERGY_PERMILLE = 5;
    public static final int DATA_ENERGY_MODE = 6;
    public static final int DATA_STATUS = 7;
    public static final int DATA_COUNT = 8;

    private static final int TANK_CAPACITY = 4000;
    private static final int ENERGY_MAX_RECEIVE = 2000;

    /** FE buffer that only the machine itself can drain. */
    private static final class Energy extends EnergyStorage {
        Energy(int capacity) {
            super(capacity, ENERGY_MAX_RECEIVE, 0);
        }

        void set(int value) {
            this.energy = Math.max(0, Math.min(value, capacity));
        }

        boolean drain(int amount) {
            if (energy < amount)
                return false;
            energy -= amount;
            return true;
        }
    }

    private final MachineKind kind;
    private final ItemStackHandler items;
    private final IItemHandler inputView;
    private final IItemHandler outputView;
    private final IItemHandler fuelView;
    private final FluidTank tank;
    private final Energy energy;

    private int progress;
    private Status status = Status.NO_INPUT;
    private int burnTime;
    private int burnDuration;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case DATA_PROGRESS -> progress;
                case DATA_MAX_PROGRESS -> processTime();
                case DATA_BURN_TIME -> burnTime;
                case DATA_BURN_DURATION -> burnDuration;
                case DATA_FLUID_PERMILLE -> tank.getFluidAmount() * 1000 / TANK_CAPACITY;
                case DATA_ENERGY_PERMILLE -> (int) ((long) energy.getEnergyStored() * 1000L
                        / Math.max(1, energy.getMaxEnergyStored()));
                case DATA_ENERGY_MODE -> useEnergy() ? 1 : 0;
                case DATA_STATUS -> status.ordinal();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            // values are derived from machine state and are read-only for the menu
        }

        @Override
        public int getCount() {
            return DATA_COUNT;
        }
    };

    public MachineBlockEntity(BlockPos pos, BlockState state) {
        super(RegistryHandler.MACHINE_BLOCK_ENTITY.get(), pos, state);
        this.kind = ((MachineBlock) state.getBlock()).kind();
        this.items = createHandler(kind, this::setChanged);
        // the top face feeds the input and reagent slots, the bottom face drains both output slots
        this.inputView = new CombinedInvWrapper(new RangedWrapper(items, SLOT_INPUT, SLOT_INPUT + 1),
                new RangedWrapper(items, SLOT_REAGENT, SLOT_REAGENT + 1));
        this.outputView = new CombinedInvWrapper(new RangedWrapper(items, SLOT_OUTPUT, SLOT_OUTPUT + 1),
                new RangedWrapper(items, SLOT_BYPRODUCT, SLOT_BYPRODUCT + 1));
        this.fuelView = new RangedWrapper(items, SLOT_FUEL, SLOT_FUEL + 1);
        this.tank = new FluidTank(TANK_CAPACITY, stack -> stack.getFluid() == Fluids.WATER) {
            @Override
            protected void onContentsChanged() {
                setChanged();
            }
        };
        this.energy = new Energy(OreProcessingConfig.get(OreProcessingConfig.COMMON.energyCapacity));
    }

    /** Shared with the client-side menu so slot rules match on both sides. */
    public static ItemStackHandler createHandler(MachineKind kind, Runnable onChanged) {
        return new ItemStackHandler(SLOT_COUNT) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return switch (slot) {
                    case SLOT_INPUT -> kind.accepts(stack);
                    case SLOT_FUEL -> burnTimeOf(stack) > 0;
                    case SLOT_REAGENT -> kind.acceptsReagent(stack);
                    default -> false;
                };
            }

            @Override
            protected void onContentsChanged(int slot) {
                onChanged.run();
            }
        };
    }

    public MachineKind kind() {
        return kind;
    }

    public ItemStackHandler items() {
        return items;
    }

    public ContainerData data() {
        return data;
    }

    public List<ItemStack> contents() {
        return List.of(items.getStackInSlot(SLOT_INPUT), items.getStackInSlot(SLOT_OUTPUT),
                items.getStackInSlot(SLOT_FUEL), items.getStackInSlot(SLOT_REAGENT),
                items.getStackInSlot(SLOT_BYPRODUCT));
    }

    // -------------------------------------------------------------------------
    // Capabilities
    // -------------------------------------------------------------------------

    @Nullable
    public IItemHandler itemHandler(@Nullable Direction side) {
        if (side == null)
            return items;
        return switch (side) {
            case UP -> inputView;
            case DOWN -> outputView;
            default -> fuelView;
        };
    }

    @Nullable
    public IFluidHandler fluidHandler() {
        return kind.usesWater() ? tank : null;
    }

    @Nullable
    public EnergyStorage energyStorage() {
        return useEnergy() ? energy : null;
    }

    public FluidTank tank() {
        return tank;
    }

    // -------------------------------------------------------------------------
    // Ticking
    // -------------------------------------------------------------------------

    public static void serverTick(Level level, BlockPos pos, BlockState state, MachineBlockEntity machine) {
        machine.tick(level, pos, state);
    }

    private void tick(Level level, BlockPos pos, BlockState state) {
        ItemStack input = items.getStackInSlot(SLOT_INPUT);
        Plan plan = planFor(level, input);
        Status blocker = blocker(input, plan);
        boolean active = blocker == null && drawPower();
        if (blocker != null)
            status = blocker;
        else
            status = active ? Status.WORKING : (useEnergy() ? Status.NEEDS_POWER : Status.NEEDS_FUEL);

        if (active) {
            progress++;
            if (progress >= processTime()) {
                finish(plan);
                progress = 0;
            }
            setChanged();
            if (level.getGameTime() % HAZARD_INTERVAL == 0)
                emitHazard(level, pos);
        } else if (progress > 0) {
            progress = Math.max(0, progress - 2);
            setChanged();
        }

        if (state.getValue(MachineBlock.LIT) != active) {
            level.setBlock(pos, state.setValue(MachineBlock.LIT, active), Block.UPDATE_ALL);
        }
    }

    /** The first thing that stops the machine from starting an operation, or null if nothing does. */
    @Nullable
    private Status blocker(ItemStack input, @Nullable Plan plan) {
        if (!OreProcessingConfig.enabled(kind))
            return Status.DISABLED;
        if (input.isEmpty())
            return Status.NO_INPUT;
        if (plan == null)
            return Status.REJECTED;
        if (!hasRoomFor(plan))
            return Status.OUTPUT_FULL;
        if (!hasWater(plan))
            return Status.NEEDS_WATER;
        if (!hasReagent(plan))
            return Status.NEEDS_REAGENT;
        return null;
    }

    /** Null means the input cannot be processed at all, an empty primary stack means a failed roll. */
    @Nullable
    private Plan planFor(Level level, ItemStack input) {
        if (input.isEmpty())
            return null;

        SingleRecipeInput recipeInput = new SingleRecipeInput(input);
        return switch (kind) {
            case CRUSHER -> {
                Plan recycled = ProcessRules.recycle(input);
                yield recycled != null ? recycled
                        : planOf(assembleFromRecipe(level, RegistryHandler.CRUSH_TYPE.get(), recipeInput), 0);
            }
            case WASHER -> planOf(assembleFromRecipe(level, RegistryHandler.WASH_TYPE.get(), recipeInput),
                    ProcessRules.waterCost(kind));
            case SMELTER -> planOf(smelt(input), 0);
            default -> ProcessRules.plan(kind, input);
        };
    }

    @Nullable
    private static Plan planOf(ItemStack result, int water) {
        return result.isEmpty() ? null : Plan.of(result, water);
    }

    private static <R extends Recipe<SingleRecipeInput>> ItemStack assembleFromRecipe(
            Level level, RecipeType<R> type, SingleRecipeInput input) {
        Optional<RecipeHolder<R>> holder = level.getRecipeManager().getRecipeFor(type, input, level);
        if (holder.isEmpty())
            return ItemStack.EMPTY;
        return holder.get().value().assemble(input, level.registryAccess());
    }

    /**
     * Final smelting is machine logic rather than a recipe: vanilla furnaces offer no
     * hook for dynamic recipes without a mixin, so the smelter resolves the output itself.
     */
    public static ItemStack smelt(ItemStack input) {
        if (!MachineKind.SMELTER.accepts(input))
            return ItemStack.EMPTY;
        return ProcessRules.smelt(input);
    }

    private boolean hasRoomFor(Plan plan) {
        return canStore(SLOT_OUTPUT, plan.primary()) && canStore(SLOT_BYPRODUCT, plan.secondary());
    }

    private boolean canStore(int slot, ItemStack result) {
        if (result.isEmpty())
            return true;
        ItemStack held = items.getStackInSlot(slot);
        if (held.isEmpty())
            return true;
        if (!ItemStack.isSameItemSameComponents(held, result))
            return false;
        return held.getCount() + result.getCount() <= held.getMaxStackSize();
    }

    private boolean hasWater(Plan plan) {
        return plan.water() <= 0 || tank.getFluidAmount() >= plan.water();
    }

    private boolean hasReagent(Plan plan) {
        if (plan.reagent() == null)
            return true;
        ItemStack held = items.getStackInSlot(SLOT_REAGENT);
        return held.is(plan.reagent().item()) && held.getCount() >= plan.reagentCount();
    }

    private void finish(Plan plan) {
        items.extractItem(SLOT_INPUT, 1, false);
        store(SLOT_OUTPUT, plan.primary());
        store(SLOT_BYPRODUCT, plan.secondary());

        if (plan.water() > 0)
            tank.drain(plan.water(), IFluidHandler.FluidAction.EXECUTE);
        if (plan.reagent() != null && OreProcessingConfig.get(OreProcessingConfig.COMMON.consumeReagents)
                && Yields.chance(plan.reagentUse()))
            items.extractItem(SLOT_REAGENT, plan.reagentCount(), false);
        setChanged();
    }

    private void store(int slot, ItemStack result) {
        if (result.isEmpty())
            return;
        ItemStack held = items.getStackInSlot(slot);
        if (held.isEmpty()) {
            items.setStackInSlot(slot, result.copy());
        } else {
            held.grow(result.getCount());
        }
    }

    private void emitHazard(Level level, BlockPos pos) {
        if (!OreProcessingConfig.get(OreProcessingConfig.COMMON.hazardsEnabled))
            return;
        Hazard hazard = ProcessRules.hazard(kind);
        if (hazard == Hazard.NONE)
            return;

        double radius = OreProcessingConfig.get(OreProcessingConfig.COMMON.hazardRadius);
        for (Player player : level.getEntitiesOfClass(Player.class, new AABB(pos).inflate(radius))) {
            switch (hazard) {
                case TOXIC -> player.addEffect(new MobEffectInstance(MobEffects.POISON, 100));
                case HEAT -> player.igniteForSeconds(2.0F);
                case RADIATION -> player.addEffect(new MobEffectInstance(MobEffects.WITHER, 100));
                default -> {
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Power
    // -------------------------------------------------------------------------

    private static boolean useEnergy() {
        return OreProcessingConfig.get(OreProcessingConfig.COMMON.useEnergy);
    }

    /** Pays for one tick of work with FE or fuel; false means the machine stalls this tick. */
    private boolean drawPower() {
        if (useEnergy())
            return energy.drain(energyCost());

        if (burnTime <= 0 && !ignite())
            return false;
        burnTime = Math.max(0, burnTime - burnRate());
        return true;
    }

    private boolean ignite() {
        ItemStack fuel = items.getStackInSlot(SLOT_FUEL);
        int ticks = burnTimeOf(fuel);
        if (ticks <= 0)
            return false;

        Item remainder = fuel.getItem().getCraftingRemainingItem();
        items.extractItem(SLOT_FUEL, 1, false);
        if (items.getStackInSlot(SLOT_FUEL).isEmpty() && remainder != null)
            items.setStackInSlot(SLOT_FUEL, new ItemStack(remainder));

        burnTime = ticks;
        burnDuration = ticks;
        return true;
    }

    private static int burnTimeOf(ItemStack stack) {
        if (stack.isEmpty())
            return 0;
        return stack.getBurnTime(RecipeType.SMELTING);
    }

    /** FE drawn per working tick, scaled by the station's power factor. */
    private int energyCost() {
        int base = OreProcessingConfig.get(OreProcessingConfig.COMMON.energyPerTick);
        return Math.max(1, (int) Math.round(base * ProcessRules.powerFactor(kind)));
    }

    /** Fuel burn ticks used up per working tick. */
    private int burnRate() {
        return Math.max(1, (int) Math.round(ProcessRules.powerFactor(kind)));
    }

    private int processTime() {
        return OreProcessingConfig.ticks(kind);
    }

    // -------------------------------------------------------------------------
    // Persistence and menu
    // -------------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Items", items.serializeNBT(registries));
        tag.put("Tank", tank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("Energy", energy.getEnergyStored());
        tag.putInt("Progress", progress);
        tag.putInt("BurnTime", burnTime);
        tag.putInt("BurnDuration", burnDuration);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        // saves from before the reagent and byproduct slots hold three slots, so the size is pinned first
        CompoundTag saved = tag.getCompound("Items").copy();
        saved.putInt("Size", SLOT_COUNT);
        items.deserializeNBT(registries, saved);
        tank.readFromNBT(registries, tag.getCompound("Tank"));
        energy.set(tag.getInt("Energy"));
        progress = tag.getInt("Progress");
        burnTime = tag.getInt("BurnTime");
        burnDuration = tag.getInt("BurnDuration");
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MachineMenu(containerId, inventory, this);
    }

    /** Used by tests to fill the tank without a bucket. */
    public void fillWater(int amount) {
        tank.fill(new FluidStack(Fluids.WATER, amount), IFluidHandler.FluidAction.EXECUTE);
    }
}
