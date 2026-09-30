package com.thecascadian.universaloreprocessing.client;

import com.thecascadian.universaloreprocessing.block.Status;
import net.minecraft.client.gui.GuiGraphics;

/** Small pixel pictograms for the machine screens, so no text is needed to read a machine's state. */
final class UiIcons {


    private static final String[] PLAY = {"#......", "###....", "#####..", "#######", "#####..", "###....", "#......"};
    private static final String[] HOURGLASS = {"#######", ".#...#.", "..#.#..", "...#...", "..#.#..", ".#...#.", "#######"};
    private static final String[] CROSS = {"##...##", ".##.##.", "..###..", "...#...", "..###..", ".##.##.", "##...##"};
    private static final String[] FULL = {"#######", "#.....#", "#.###.#", "#.###.#", "#.###.#", "#.....#", "#######"};
    private static final String[] DROP = {"...#...", "..###..", ".#####.", ".#####.", "#######", "#######", ".#####."};
    private static final String[] FLASK = {"..###..", "...#...", "...#...", "..###..", ".#####.", "#######", ".#####."};
    private static final String[] FLAME = {"...#...", "..##...", "..###..", ".#####.", "#######", "#######", ".#####."};
    private static final String[] BOLT = {"...###.", "..###..", ".###...", "#######", "...###.", "..##...", ".#....."};
    private static final String[] OFF = {".#####.", "#....##", "#...#.#", "#..#..#", "#.#...#", "##....#", ".#####."};

    static final int GREEN = 0xFF7BE07B;
    static final int AMBER = 0xFFF2C14E;
    static final int RED = 0xFFE85A5A;
    static final int BLUE = 0xFF4F8BF0;
    static final int ORANGE = 0xFFE8892B;
    static final int GREY = 0xFF8A919D;

    private UiIcons() {
    }

    static void draw(GuiGraphics graphics, String[] rows, int x, int y, int scale, int color) {
        for (int gy = 0; gy < rows.length; gy++) {
            for (int gx = 0; gx < rows[gy].length(); gx++) {
                if (rows[gy].charAt(gx) == '#') {
                    int px = x + gx * scale;
                    int py = y + gy * scale;
                    graphics.fill(px, py, px + scale, py + scale, color);
                }
            }
        }
    }

    static String[] of(Status status) {
        return switch (status) {
            case WORKING -> PLAY;
            case NO_INPUT -> HOURGLASS;
            case REJECTED -> CROSS;
            case OUTPUT_FULL -> FULL;
            case NEEDS_WATER -> DROP;
            case NEEDS_REAGENT -> FLASK;
            case NEEDS_FUEL -> FLAME;
            case NEEDS_POWER -> BOLT;
            case DISABLED -> OFF;
        };
    }

    static int colorOf(Status status) {
        return switch (status) {
            case WORKING -> GREEN;
            case NO_INPUT, DISABLED -> GREY;
            case REJECTED, NEEDS_POWER -> RED;
            case NEEDS_WATER -> BLUE;
            case NEEDS_FUEL -> ORANGE;
            case OUTPUT_FULL, NEEDS_REAGENT -> AMBER;
        };
    }
}
