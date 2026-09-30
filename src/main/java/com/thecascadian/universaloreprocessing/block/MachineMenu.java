package com.thecascadian.universaloreprocessing.block;

import com.thecascadian.universaloreprocessing.registry.RegistryHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * Menu shared by every machine. The server side wraps the block entity's
 * handler and data; the client side builds an empty handler with the same slot
 * rules and receives contents and progress through the standard sync.
 */
public class MachineMenu extends AbstractContainerMenu {

    private static final int MACHINE_SLOTS = MachineBlockEntity.SLOT_COUNT;
    private static final int INVENTORY_END = MACHINE_SLOTS + 27;
    private static final int HOTBAR_END = INVENTORY_END + 9;

    private final MachineKind kind;
    private final BlockPos pos;
    private final ContainerData data;
    private final ContainerLevelAccess access;

    public MachineMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(containerId, inventory, buf.readBlockPos(), buf.readEnum(MachineKind.class),
                MachineBlockEntity.createHandler(MachineKind.CRUSHER, () -> {
                }), new SimpleContainerData(MachineBlockEntity.DATA_COUNT));
    }

    public MachineMenu(int containerId, Inventory inventory, MachineBlockEntity machine) {
        this(containerId, inventory, machine.getBlockPos(), machine.kind(), machine.items(), machine.data());
    }

    private MachineMenu(int containerId, Inventory inventory, BlockPos pos, MachineKind kind,
            ItemStackHandler handler, ContainerData data) {
        super(RegistryHandler.MACHINE_MENU.get(), containerId);
        this.kind = kind;
        this.pos = pos;
        this.data = data;
        this.access = ContainerLevelAccess.create(inventory.player.level(), pos);

        addSlot(new SlotItemHandler(handler, MachineBlockEntity.SLOT_INPUT, 28, 44) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return kind.accepts(stack);
            }
        });
        addSlot(new SlotItemHandler(handler, MachineBlockEntity.SLOT_OUTPUT, 132, 44));
        addSlot(new SlotItemHandler(handler, MachineBlockEntity.SLOT_FUEL, 80, 82) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isFuel(stack) && usesFuel();
            }

            @Override
            public boolean isActive() {
                return usesFuel();
            }
        });
        addSlot(new SlotItemHandler(handler, MachineBlockEntity.SLOT_REAGENT, 28, 72) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return kind.acceptsReagent(stack);
            }

            @Override
            public boolean isActive() {
                return kind.hasReagentSlot();
            }
        });
        addSlot(new SlotItemHandler(handler, MachineBlockEntity.SLOT_BYPRODUCT, 132, 72) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public boolean isActive() {
                return kind.hasByproductSlot();
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 120 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 178));
        }
        addDataSlots(data);
    }

    public MachineKind kind() {
        return kind;
    }

    /** Fuel is only used when the machines are not powered by FE. */
    private boolean usesFuel() {
        return data.get(MachineBlockEntity.DATA_ENERGY_MODE) == 0;
    }

    public int get(int index) {
        return data.get(index);
    }

    private static boolean isFuel(ItemStack stack) {
        return !stack.isEmpty() && stack.getBurnTime(RecipeType.SMELTING) > 0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem())
            return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(stack, MACHINE_SLOTS, HOTBAR_END, true))
                return ItemStack.EMPTY;
        } else if (kind.accepts(stack)) {
            if (!moveItemStackTo(stack, MachineBlockEntity.SLOT_INPUT, MachineBlockEntity.SLOT_INPUT + 1, false))
                return ItemStack.EMPTY;
        } else if (kind.acceptsReagent(stack)) {
            if (!moveItemStackTo(stack, MachineBlockEntity.SLOT_REAGENT, MachineBlockEntity.SLOT_REAGENT + 1, false))
                return ItemStack.EMPTY;
        } else if (isFuel(stack) && usesFuel()) {
            if (!moveItemStackTo(stack, MachineBlockEntity.SLOT_FUEL, MachineBlockEntity.SLOT_FUEL + 1, false))
                return ItemStack.EMPTY;
        } else if (index < INVENTORY_END) {
            if (!moveItemStackTo(stack, INVENTORY_END, HOTBAR_END, false))
                return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, MACHINE_SLOTS, INVENTORY_END, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == original.getCount())
            return ItemStack.EMPTY;

        slot.onTake(player, stack);
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, player.level().getBlockState(pos).getBlock());
    }
}
