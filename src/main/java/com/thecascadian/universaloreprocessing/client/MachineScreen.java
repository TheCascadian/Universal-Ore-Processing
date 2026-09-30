package com.thecascadian.universaloreprocessing.client;

import com.thecascadian.universaloreprocessing.UniversalOreProcessing;
import com.thecascadian.universaloreprocessing.block.MachineBlockEntity;
import com.thecascadian.universaloreprocessing.block.MachineKind;
import com.thecascadian.universaloreprocessing.block.MachineMenu;
import com.thecascadian.universaloreprocessing.block.Status;
import com.thecascadian.universaloreprocessing.config.ClientConfig;
import com.thecascadian.universaloreprocessing.item.MaterialItem.Stage;
import com.thecascadian.universaloreprocessing.item.Tooltips;
import com.thecascadian.universaloreprocessing.process.ProcessRule;
import com.thecascadian.universaloreprocessing.process.ProcessRules;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import java.util.ArrayList;
import java.util.List;

/**
 * Screen for every machine, drawn with flat fills so it needs no texture. A tier colored
 * title band, labelled slots, an emblem that is unique to the machine and lights up while
 * it works, a progress bar, and a status line that says in plain words what the machine
 * is doing or what it is missing. Hovering over any bar or empty slot explains it.
 */
public class MachineScreen extends AbstractContainerScreen<MachineMenu> {

    private static final int FRAME = 0xFF14161A;
    private static final int PANEL = 0xFF30353D;
    private static final int PANEL_LIGHT = 0xFF3B4150;
    private static final int PANEL_DARK = 0xFF23272E;
    private static final int SLOT_EDGE = 0xFF0E1013;
    private static final int SLOT_FILL = 0xFF555C69;
    private static final int TRACK = 0xFF1A1D22;
    private static final int TEXT = 0xFFE8EAEE;
    private static final int TEXT_DIM = 0xFF9AA1AD;
    private static final int OK = 0xFF7BE07B;
    private static final int WARN = 0xFFF2C14E;
    private static final int FLAME = 0xFFE8892B;
    private static final int ENERGY = 0xFFD64545;
    private static final int WATER = 0xFF3F76E4;
    private static final int ACCENT = 0xFFE8892B;

    private static final int[] TIER_COLORS = {
            0xFF7C8796, 0xFF60A860, 0xFFD68236, 0xFF9660C8, 0xFFDEC446,
            0xFF50BEC8, 0xFF46A896, 0xFFD66EAA, 0xFFC84646};

    private static final String LANG = "gui." + UniversalOreProcessing.MODID + ".";

    private static final int BAR_TOP = 40;
    private static final int BAR_HEIGHT = 52;

    public MachineScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 202;
        this.inventoryLabelY = 109;
        this.titleLabelY = 4;
    }

    private static Component text(String key, Object... args) {
        return Component.translatable(LANG + key, args);
    }

    private int tierColor() {
        return TIER_COLORS[Math.min(menu.kind().tier(), TIER_COLORS.length - 1)];
    }

    // -------------------------------------------------------------------------
    // Drawing
    // -------------------------------------------------------------------------

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        renderHints(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        int left = leftPos;
        int top = topPos;
        int color = tierColor();

        graphics.fill(left, top, left + imageWidth, top + imageHeight, FRAME);
        graphics.fill(left + 1, top + 1, left + imageWidth - 1, top + imageHeight - 1, PANEL);
        graphics.fill(left + 1, top + 1, left + imageWidth - 1, top + 15, color);
        graphics.fill(left + 1, top + 15, left + imageWidth - 1, top + 16, PANEL_DARK);
        graphics.fill(left + 4, top + 119, left + imageWidth - 4, top + imageHeight - 4, PANEL_DARK);

        graphics.fill(left + 22, top + 38, left + 154, top + 100, PANEL_DARK);
        graphics.fill(left + 22, top + 99, left + 154, top + 100, PANEL_LIGHT);

        for (Slot slot : menu.slots) {
            if (!slot.isActive())
                continue;
            drawSlot(graphics, left + slot.x, top + slot.y);
        }

        drawArrow(graphics, left + 56, top + 44);
        drawPower(graphics, left, top);
        MachineKind kind = menu.kind();
        boolean energy = menu.get(MachineBlockEntity.DATA_ENERGY_MODE) == 1;
        UiIcons.draw(graphics, energy ? UiIcons.BOLT : UiIcons.FLAME, left + 10, top + BAR_TOP + BAR_HEIGHT + 4, 1,
                energy ? ENERGY : FLAME);
        if (kind.usesWater()) {
            drawBar(graphics, left + 158, top + BAR_TOP, 8, BAR_HEIGHT,
                    menu.get(MachineBlockEntity.DATA_FLUID_PERMILLE), WATER);
            UiIcons.draw(graphics, UiIcons.DROP, left + 158, top + BAR_TOP + BAR_HEIGHT + 4, 1, WATER);
        }
        if (kind.hasReagentSlot())
            UiIcons.draw(graphics, UiIcons.FLASK, left + 48, top + 76, 1, TEXT_DIM);
        if (kind.hasByproductSlot())
            UiIcons.draw(graphics, UiIcons.RUBBLE, left + 121, top + 76, 1, TEXT_DIM);
        if (!energy)
            UiIcons.draw(graphics, UiIcons.FLAME, left + 100, top + 86, 1, TEXT_DIM);
        drawStatus(graphics, left + 81, top + 64);

        // information marker at the right end of the title band
        graphics.fill(left + imageWidth - 14, top + 3, left + imageWidth - 4, top + 13, 0x55000000);
        graphics.drawString(font, "i", left + imageWidth - 10, top + 4, 0xFFFFFFFF, false);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(font, title, titleLabelX, titleLabelY, 0xFFFFFFFF, true);
        graphics.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, TEXT_DIM, false);
    }

    private void drawSlot(GuiGraphics graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, SLOT_EDGE);
        graphics.fill(x, y, x + 16, y + 16, SLOT_FILL);
        graphics.fill(x, y, x + 16, y + 1, PANEL_DARK);
        graphics.fill(x, y, x + 1, y + 16, PANEL_DARK);
    }

    private static int dim(int argb) {
        int r = ((argb >> 16) & 0xFF) * 2 / 5;
        int g = ((argb >> 8) & 0xFF) * 2 / 5;
        int b = (argb & 0xFF) * 2 / 5;
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    /** Progress arrow between the input and output; fills from the left as the work advances. */
    private void drawArrow(GuiGraphics graphics, int x, int y) {
        int max = Math.max(1, menu.get(MachineBlockEntity.DATA_MAX_PROGRESS));
        int filled = Math.min(64, menu.get(MachineBlockEntity.DATA_PROGRESS) * 64 / max);
        for (int col = 0; col < 64; col++) {
            int half = col < 52 ? 3 : 8 - (col - 52) * 2 / 3;
            graphics.fill(x + col, y + 8 - half, x + col + 1, y + 8 + half, col < filled ? OK : TRACK);
        }
    }

    private void drawPower(GuiGraphics graphics, int left, int top) {
        if (menu.get(MachineBlockEntity.DATA_ENERGY_MODE) == 1) {
            drawBar(graphics, left + 10, top + BAR_TOP, 8, BAR_HEIGHT,
                    menu.get(MachineBlockEntity.DATA_ENERGY_PERMILLE), ENERGY);
            return;
        }
        int duration = Math.max(1, menu.get(MachineBlockEntity.DATA_BURN_DURATION));
        int permille = menu.get(MachineBlockEntity.DATA_BURN_TIME) * 1000 / duration;
        drawBar(graphics, left + 10, top + BAR_TOP, 8, BAR_HEIGHT, permille, FLAME);
    }

    /** Vertical bar filled from the bottom; fill is given in permille. */
    private void drawBar(GuiGraphics graphics, int x, int y, int width, int height, int permille, int color) {
        int filled = Math.max(0, Math.min(height, height * permille / 1000));
        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, SLOT_EDGE);
        graphics.fill(x, y, x + width, y + height, TRACK);
        graphics.fill(x, y + height - filled, x + width, y + height, color);
    }

    // -------------------------------------------------------------------------
    // Status
    // -------------------------------------------------------------------------

    private Status status() {
        return Status.byOrdinal(menu.get(MachineBlockEntity.DATA_STATUS));
    }

    private Component inputHint() {
        MachineKind kind = menu.kind();
        switch (kind) {
            case CRUSHER:
                return text("input.crusher");
            case SMELTER:
                return text("input.smelter");
            case WASHER:
                return Tooltips.stageName(Stage.CRUSHED);
            default:
                break;
        }
        ProcessRule rule = ProcessRules.get(kind);
        List<Component> names = new ArrayList<>();
        if (rule != null) {
            for (Stage stage : rule.inputs())
                names.add(Tooltips.stageName(stage));
        }
        return Tooltips.join(names);
    }

    private Component reagentHint() {
        ProcessRule rule = ProcessRules.get(menu.kind());
        return rule != null && rule.reagent() != null ? Tooltips.reagentName(rule.reagent()) : Component.empty();
    }

    private Component statusText() {
        return switch (status()) {
            case WORKING -> text("status.working");
            case NO_INPUT -> text("status.no_input", inputHint());
            case REJECTED -> text("status.rejected", inputHint());
            case OUTPUT_FULL -> text("status.output_full");
            case NEEDS_WATER -> text("status.needs_water");
            case NEEDS_REAGENT -> text("status.needs_reagent", reagentHint());
            case NEEDS_FUEL -> text("status.needs_fuel");
            case NEEDS_POWER -> text("status.needs_power");
            case DISABLED -> text("status.disabled");
        };
    }

    /** A single icon shown only while the machine is stopped; its colour tells the reason. */
    private void drawStatus(GuiGraphics graphics, int x, int y) {
        Status status = status();
        if (status == Status.WORKING)
            return;
        UiIcons.draw(graphics, UiIcons.of(status), x, y, 2, UiIcons.colorOf(status));
    }

    // -------------------------------------------------------------------------
    // Hover hints
    // -------------------------------------------------------------------------

    private boolean over(int mouseX, int mouseY, int x, int y, int w, int h) {
        return mouseX >= leftPos + x && mouseX < leftPos + x + w && mouseY >= topPos + y && mouseY < topPos + y + h;
    }

    private List<Component> extras;
    private int hintId = -1;
    private long hintSince;

    private void renderHints(GuiGraphics graphics, int mouseX, int mouseY) {
        Component hint = null;
        extras = null;
        int id = -1;
        if (over(mouseX, mouseY, 0, 0, imageWidth, 16)) {
            id = 14;
            extras = new ArrayList<>();
            extras.add(title.copy().withStyle(net.minecraft.ChatFormatting.WHITE));
            extras.add(Tooltips.machineBlurb(menu.kind()));
            Tooltips.machineDetails(menu.kind(), extras);
        }
        if (id == 14) {
            // the title band takes priority over everything below it
        } else if (hoveredSlot != null && hoveredSlot.isActive() && !hoveredSlot.hasItem()
                && hoveredSlot.index < MachineBlockEntity.SLOT_COUNT) {
            id = hoveredSlot.index;
            hint = switch (hoveredSlot.index) {
                case MachineBlockEntity.SLOT_INPUT -> text("hint.input", inputHint());
                case MachineBlockEntity.SLOT_OUTPUT -> text("hint.output");
                case MachineBlockEntity.SLOT_FUEL -> text("hint.fuel");
                case MachineBlockEntity.SLOT_REAGENT -> text("hint.reagent", reagentHint());
                default -> text("hint.byproduct");
            };
        } else {
            boolean energy = menu.get(MachineBlockEntity.DATA_ENERGY_MODE) == 1;
            if (over(mouseX, mouseY, 10, BAR_TOP, 8, BAR_HEIGHT)) {
                int permille = energy ? menu.get(MachineBlockEntity.DATA_ENERGY_PERMILLE)
                        : menu.get(MachineBlockEntity.DATA_BURN_TIME) * 1000
                                / Math.max(1, menu.get(MachineBlockEntity.DATA_BURN_DURATION));
                id = 10;
                hint = text(energy ? "hint.energy" : "hint.burn", permille / 10);
            } else if (menu.kind().usesWater() && over(mouseX, mouseY, 158, BAR_TOP, 8, BAR_HEIGHT)) {
                id = 11;
                hint = text("hint.water", menu.get(MachineBlockEntity.DATA_FLUID_PERMILLE) / 10);
            } else if (over(mouseX, mouseY, 81, 64, 14, 14)) {
                id = 12;
                hint = statusText();
            } else if (over(mouseX, mouseY, 56, 44, 64, 16)) {
                int max = Math.max(1, menu.get(MachineBlockEntity.DATA_MAX_PROGRESS));
                int percent = menu.get(MachineBlockEntity.DATA_PROGRESS) * 100 / max;
                id = 13;
                hint = text("hint.progress", percent, max / 20);
            }
        }

        if (id != hintId) {
            hintId = id;
            hintSince = System.currentTimeMillis();
        }
        if ((hint == null && extras == null) || !ClientConfig.guiHints(menu.kind()))
            return;
        if (System.currentTimeMillis() - hintSince < ClientConfig.hintDelayMs())
            return;
        if (extras != null)
            graphics.renderComponentTooltip(font, extras, mouseX, mouseY);
        else
            graphics.renderTooltip(font, hint, mouseX, mouseY);
    }
}
