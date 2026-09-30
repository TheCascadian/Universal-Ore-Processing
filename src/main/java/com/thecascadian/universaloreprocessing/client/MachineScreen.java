package com.thecascadian.universaloreprocessing.client;

import com.thecascadian.universaloreprocessing.block.MachineBlockEntity;
import com.thecascadian.universaloreprocessing.block.MachineKind;
import com.thecascadian.universaloreprocessing.block.MachineMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * Screen for all three machines. Drawn with flat fills so the GUI needs no
 * texture: a progress arrow, a flame or FE bar for power, and a water bar on
 * the washer.
 */
public class MachineScreen extends AbstractContainerScreen<MachineMenu> {

    private static final int PANEL = 0xFFC6C6C6;
    private static final int SHADOW = 0xFF555555;
    private static final int SLOT_FILL = 0xFF8B8B8B;
    private static final int TRACK = 0xFF373737;
    private static final int PROGRESS = 0xFFF2F2F2;
    private static final int FLAME = 0xFFE8892B;
    private static final int ENERGY = 0xFFC93C3C;
    private static final int WATER = 0xFF3F76E4;

    public MachineScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int left = leftPos;
        int top = topPos;

        graphics.fill(left, top, left + imageWidth, top + imageHeight, SHADOW);
        graphics.fill(left + 1, top + 1, left + imageWidth - 1, top + imageHeight - 1, PANEL);

        for (Slot slot : menu.slots) {
            int x = left + slot.x;
            int y = top + slot.y;
            graphics.fill(x - 1, y - 1, x + 17, y + 17, SHADOW);
            graphics.fill(x, y, x + 16, y + 16, SLOT_FILL);
        }

        drawProgress(graphics, left + 79, top + 34);
        drawPower(graphics, left, top);
        if (menu.kind() == MachineKind.WASHER)
            drawBar(graphics, left + 30, top + 17, 8, 52, menu.get(MachineBlockEntity.DATA_FLUID_PERMILLE), WATER);
    }

    private void drawProgress(GuiGraphics graphics, int x, int y) {
        int max = Math.max(1, menu.get(MachineBlockEntity.DATA_MAX_PROGRESS));
        int width = menu.get(MachineBlockEntity.DATA_PROGRESS) * 24 / max;
        graphics.fill(x, y, x + 24, y + 17, TRACK);
        graphics.fill(x, y, x + Math.min(24, width), y + 17, PROGRESS);
    }

    private void drawPower(GuiGraphics graphics, int left, int top) {
        if (menu.get(MachineBlockEntity.DATA_ENERGY_MODE) == 1) {
            drawBar(graphics, left + 8, top + 17, 8, 52, menu.get(MachineBlockEntity.DATA_ENERGY_PERMILLE), ENERGY);
            return;
        }
        int duration = Math.max(1, menu.get(MachineBlockEntity.DATA_BURN_DURATION));
        int permille = menu.get(MachineBlockEntity.DATA_BURN_TIME) * 1000 / duration;
        drawBar(graphics, left + 60, top + 36, 8, 14, permille, FLAME);
    }

    /** Vertical bar filled from the bottom; fill is given in permille. */
    private void drawBar(GuiGraphics graphics, int x, int y, int width, int height, int permille, int color) {
        int filled = Math.max(0, Math.min(height, height * permille / 1000));
        graphics.fill(x, y, x + width, y + height, TRACK);
        graphics.fill(x, y + height - filled, x + width, y + height, color);
    }
}
