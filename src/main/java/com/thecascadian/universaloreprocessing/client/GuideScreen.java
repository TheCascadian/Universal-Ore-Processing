package com.thecascadian.universaloreprocessing.client;

import com.thecascadian.universaloreprocessing.UniversalOreProcessing;
import com.thecascadian.universaloreprocessing.guide.GuideContent;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * The guide book. A chapter list sits on the left and the chosen chapter is laid
 * out into pages on the right, with page buttons and mouse wheel paging. Text
 * wraps across pages; item rows and diagrams are kept whole and show the item
 * tooltip on hover.
 */
public class GuideScreen extends Screen {

    private static final int WIDTH = 300;
    private static final int HEIGHT = 196;
    private static final int LIST_WIDTH = 104;
    private static final int LINE_HEIGHT = 10;
    private static final int CELL = 18;
    private static final int GAP = 4;
    private static final int PAGE_HEIGHT = HEIGHT - 34;

    private static final int PAPER = 0xFFE9DCBC;
    private static final int PAPER_EDGE = 0xFF8C6B3F;
    private static final int LIST_BG = 0xFF5B4327;
    private static final int CELL_BG = 0x26000000;
    private static final int TEXT = 0xFF2B2015;

    /** One laid out line of a page. */
    private sealed interface Line permits TextLine, StackLine, Gap {
        int height();
    }

    private record TextLine(FormattedCharSequence text) implements Line {
        @Override
        public int height() {
            return LINE_HEIGHT;
        }
    }

    /** A row of item cells; in a flow an empty stack is an arrow, in a diagram it is an empty cell. */
    private record StackLine(List<ItemStack> stacks, boolean diagram) implements Line {
        @Override
        public int height() {
            return CELL;
        }
    }

    private record Gap() implements Line {
        @Override
        public int height() {
            return GAP;
        }
    }

    private final List<GuideContent.Chapter> chapters;
    private List<List<Line>> pages = List.of();
    private int chapter;
    private int page;
    private Button previous;
    private Button next;

    public GuideScreen(List<GuideContent.Chapter> chapters) {
        super(Component.translatable("item." + UniversalOreProcessing.MODID + ".guide"));
        this.chapters = chapters;
    }

    private int left() {
        return (width - WIDTH) / 2;
    }

    private int top() {
        return (height - HEIGHT) / 2;
    }

    @Override
    protected void init() {
        int x = left() + 6;
        int buttonHeight = Math.min(16, (HEIGHT - 12) / Math.max(1, chapters.size()));
        for (int i = 0; i < chapters.size(); i++) {
            int index = i;
            addRenderableWidget(Button.builder(chapters.get(i).title(), button -> select(index))
                    .bounds(x, top() + 6 + i * buttonHeight, LIST_WIDTH - 10, buttonHeight - 1).build());
        }
        int bottom = top() + HEIGHT - 22;
        int pageX = left() + LIST_WIDTH + 6;
        previous = addRenderableWidget(Button.builder(Component.literal("<"), button -> turn(-1))
                .bounds(pageX, bottom, 22, 16).build());
        next = addRenderableWidget(Button.builder(Component.literal(">"), button -> turn(1))
                .bounds(left() + WIDTH - 28, bottom, 22, 16).build());
        select(chapter);
    }

    private void select(int index) {
        chapter = Math.max(0, Math.min(index, chapters.size() - 1));
        page = 0;
        paginate();
        updateButtons();
    }

    private void paginate() {
        int textWidth = WIDTH - LIST_WIDTH - 24;
        List<List<Line>> result = new ArrayList<>();
        List<Line> current = new ArrayList<>();
        int used = 0;

        for (GuideContent.Element element : chapters.get(chapter).elements()) {
            List<Line> block = new ArrayList<>();
            // item rows and diagrams move to the next page whole; text may split between lines
            boolean whole = true;
            switch (element) {
                case GuideContent.Text text -> {
                    for (FormattedCharSequence line : font.split(text.text(), textWidth))
                        block.add(new TextLine(line));
                    whole = false;
                }
                case GuideContent.Flow flow -> block.add(new StackLine(flow.stacks(), false));
                case GuideContent.Diagram diagram -> {
                    for (List<ItemStack> row : diagram.rows())
                        block.add(new StackLine(row, true));
                    for (FormattedCharSequence line : font.split(diagram.caption(), textWidth))
                        block.add(new TextLine(line));
                }
            }
            block.add(new Gap());

            int blockHeight = block.stream().mapToInt(Line::height).sum();
            if (whole && used > 0 && used + blockHeight > PAGE_HEIGHT) {
                result.add(current);
                current = new ArrayList<>();
                used = 0;
            }
            for (Line line : block) {
                if (!whole && used > 0 && used + line.height() > PAGE_HEIGHT) {
                    result.add(current);
                    current = new ArrayList<>();
                    used = 0;
                }
                // a gap never opens a page
                if (line instanceof Gap && used == 0)
                    continue;
                current.add(line);
                used += line.height();
            }
        }
        if (!current.isEmpty() || result.isEmpty())
            result.add(current);
        pages = result;
    }

    private void turn(int delta) {
        int target = Math.max(0, Math.min(page + delta, pages.size() - 1));
        if (target != page) {
            page = target;
            updateButtons();
        }
    }

    private void updateButtons() {
        if (previous != null)
            previous.active = page > 0;
        if (next != null)
            next.active = page < pages.size() - 1;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        turn(scrollY < 0 ? 1 : -1);
        return true;
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(graphics, mouseX, mouseY, partialTick);
        int left = left();
        int top = top();
        graphics.fill(left - 2, top - 2, left + WIDTH + 2, top + HEIGHT + 2, PAPER_EDGE);
        graphics.fill(left, top, left + LIST_WIDTH, top + HEIGHT, LIST_BG);
        graphics.fill(left + LIST_WIDTH, top, left + WIDTH, top + HEIGHT, PAPER);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int left = left();
        int top = top();
        int textX = left + LIST_WIDTH + 8;
        int y = top + 8;
        ItemStack hovered = ItemStack.EMPTY;

        for (Line line : pages.get(page)) {
            switch (line) {
                case TextLine text -> graphics.drawString(font, text.text(), textX, y, TEXT, false);
                case StackLine row -> {
                    for (int i = 0; i < row.stacks().size(); i++) {
                        ItemStack stack = row.stacks().get(i);
                        int x = textX + i * CELL;
                        if (row.diagram())
                            graphics.fill(x, y, x + CELL - 1, y + CELL - 1, CELL_BG);
                        if (stack.isEmpty()) {
                            if (!row.diagram())
                                graphics.drawString(font, "→", x + 5, y + 5, TEXT, false);
                            continue;
                        }
                        graphics.renderItem(stack, x + 1, y + 1);
                        if (mouseX >= x && mouseX < x + CELL && mouseY >= y && mouseY < y + CELL)
                            hovered = stack;
                    }
                }
                case Gap gap -> {
                }
            }
            y += line.height();
        }

        Component counter = Component.literal((page + 1) + " / " + pages.size());
        graphics.drawString(font, counter, left + LIST_WIDTH + (WIDTH - LIST_WIDTH) / 2 - font.width(counter) / 2,
                top + HEIGHT - 18, TEXT, false);

        if (!hovered.isEmpty())
            graphics.renderTooltip(font, hovered, mouseX, mouseY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
