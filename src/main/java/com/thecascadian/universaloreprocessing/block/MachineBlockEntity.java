package com.thecascadian.universaloreprocessing.block;

import com.thecascadian.universaloreprocessing.config.OreProcessingConfig;
import com.thecascadian.universaloreprocessing.item.MaterialItem;
import com.thecascadian.universaloreprocessing.material.MaterialRegistry;
import com.thecascadian.universaloreprocessing.recipe.Yields;
import com.thecascadian.universaloreprocessing.registry.RegistryHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
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
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.RangedWrapper;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Block entity shared by the crusher, washer and smelter. Holds an input,
 * output and fuel slot, optional water tank and FE buffer, and advances a
 * progress counter while the stage's recipe has a valid result and power.
 */
public class MachineBlockEntity extends BlockEntity implements MenuProvider {

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_OUTPUT = 1;
    public static final int SLOT_FUEL = 2;

    public static final int DATA_PROGRESS = 0;
    public static final int DATA_MAX_PROGRESS = 1;
    public static final int DATA_BURN_TIME = 2;
    public static final int DATA_BURN_DURATION = 3;
    public static final int DATA_FLUID_PERMILLE = 4;
    public static final int DATA_ENERGY_PERMILLE = 5;
    public static final int DATA_ENERGY_MODE = 6;
    public static final int DATA_COUNT = 7;

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
        this.inputView = new RangedWrapper(items, SLOT_INPUT, SLOT_INPUT + 1);
        this.outputView = new RangedWrapper(items, SLOT_OUTPUT, SLOT_OUTPUT + 1);
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
        return new ItemStackHandler(3) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return switch (slot) {
                    case SLOT_INPUT -> kind.accepts(stack);
                    case SLOT_FUEL -> burnTimeOf(stack) > 0;
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
                items.getStackInSlot(SLOT_FUEL));
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
        return kind == MachineKind.WASHER ? tank : null;
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
        ItemStack result = resultFor(level, items.getStackInSlot(SLOT_INPUT));
        boolean canWork = !result.isEmpty() && hasRoomFor(result) && hasWater();
        boolean active = canWork && drawPower();

        if (active) {
            progress++;
            if (progress >= processTime()) {
                finish(result);
                progress = 0;
            }
            setChanged();
        } else if (progress > 0) {
            progress = Math.max(0, progress - 2);
            setChanged();
        }

        if (state.getValue(MachineBlock.LIT) != active) {
            level.setBlock(pos, state.setValue(MachineBlock.LIT, active), Block.UPDATE_ALL);
        }
    }


    private ItemStack resultFor(Level level, ItemStack input) {
        if (input.isEmpty())
            return ItemStack.EMPTY;

        SingleRecipeInput recipeInput = new SingleRecipeInput(input);
        return switch (kind) {
            case CRUSHER -> assembleFromRecipe(level, RegistryHandler.CRUSH_TYPE.get(), recipeInput);
            case WASHER -> assembleFromRecipe(level, RegistryHandler.WASH_TYPE.get(), recipeInput);
            case SMELTER -> smelt(input);
        };
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
        if (input.isEmpty() || !OreProcessingConfig.get(OreProcessingConfig.COMMON.smeltEnabled))
            return ItemStack.EMPTY;
        if (!MachineKind.SMELTER.accepts(input))
            return ItemStack.EMPTY;

        String materialId = MaterialItem.materialId(input);
        Optional<MaterialRegistry.Material> material = MaterialRegistry.current().get(materialId);
        if (material.isEmpty())
            return ItemStack.EMPTY;

        double multiplier = OreProcessingConfig.get(OreProcessingConfig.COMMON.smelterYieldMultiplier);
        return new ItemStack(material.get().output(), Yields.scale(1, multiplier));
    }

    private boolean hasRoomFor(ItemStack result) {
        ItemStack output = items.getStackInSlot(SLOT_OUTPUT);
        if (output.isEmpty())
            return true;
        if (!ItemStack.isSameItemSameComponents(output, result))
            return false;
        return output.getCount() + result.getCount() <= output.getMaxStackSize();
    }

    private boolean hasWater() {
        if (kind != MachineKind.WASHER)
            return true;
        return tank.getFluidAmount() >= waterCost();
    }

    private int waterCost() {
        return OreProcessingConfig.get(OreProcessingConfig.COMMON.washerWaterPerOperation);
    }

    private void finish(ItemStack result) {
        items.extractItem(SLOT_INPUT, 1, false);

        ItemStack output = items.getStackInSlot(SLOT_OUTPUT);
        if (output.isEmpty()) {
            items.setStackInSlot(SLOT_OUTPUT, result);
        } else {
            output.grow(result.getCount());
        }

        if (kind == MachineKind.WASHER)
            tank.drain(waterCost(), IFluidHandler.FluidAction.EXECUTE);
        setChanged();
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
            return energy.drain(OreProcessingConfig.get(OreProcessingConfig.COMMON.energyPerTick));

        if (burnTime <= 0 && !ignite())
            return false;
        burnTime--;
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

    private int processTime() {
        OreProcessingConfig.Common config = OreProcessingConfig.COMMON;
        return switch (kind) {
            case CRUSHER -> OreProcessingConfig.get(config.crusherTicks);
            case WASHER -> OreProcessingConfig.get(config.washerTicks);
            case SMELTER -> OreProcessingConfig.get(config.smelterTicks);
        };
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
        items.deserializeNBT(registries, tag.getCompound("Items"));
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
