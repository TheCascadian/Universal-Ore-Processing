package com.thecascadian.universaloreprocessing.client;

import java.util.Map;

/** 8 by 8 emblem bitmaps drawn in the middle of each machine screen, one per machine. '#' is the main color, 'o' the accent. */
final class MachineGlyphs {

    private static final String[] FALLBACK = {"........", ".######.", ".#....#.", ".#.##.#.", ".#.##.#.", ".#....#.", ".######.", "........"};

    private static final Map<String, String[]> GLYPHS = Map.ofEntries(
            Map.entry("ore_crusher", new String[]{"#.#.#.#.", "#.#.#.#.", "########", "#......#", ".#.oo.#.", "..#oo#..", "...##...", "........"}),
            Map.entry("ore_washer", new String[]{"........", "..o..o..", ".o..o..o", "........", "..o..o..", ".o..o..o", "########", "........"}),
            Map.entry("ore_smelter", new String[]{"...o....", "..ooo...", ".ooooo..", ".#ooo#..", "########", "#......#", "#.####.#", "########"}),
            Map.entry("density_classifier", new String[]{"........", "########", "........", "######..", "........", "####....", "........", "........"}),
            Map.entry("flotation_cell", new String[]{"..o..o..", ".o.oo..o", "..o...o.", ".o..o.o.", "########", "#......#", "#......#", "########"}),
            Map.entry("magnetic_separator", new String[]{"##....##", "##....##", "##....##", "##....##", "###..###", ".######.", "..####..", "........"}),
            Map.entry("blast_furnace", new String[]{"...##...", "..####..", ".######.", "########", "#......#", "#.####.#", "#.####.#", "########"}),
            Map.entry("oxidation_converter", new String[]{"...##...", "..#..#..", ".#....#.", ".#....#.", ".#....#.", "..#..#..", "...##...", "..####.."}),
            Map.entry("thermal_retort", new String[]{"########", "#......#", "#.####.#", "#.#..#.#", "#.####.#", "#......#", "########", "..####.."}),
            Map.entry("pressure_autoclave", new String[]{"..####..", ".######.", "########", "##o##o##", "########", "########", ".######.", "..####.."}),
            Map.entry("phase_extractor", new String[]{"#######.", "#.....#.", "#######.", "..#.....", "..#####.", "..#...#.", "..#####.", "........"}),
            Map.entry("precipitation_array", new String[]{"#..#..#.", "#..#..#.", "#..#..#.", "#..#..#.", "o..o..o.", "o..o..o.", ".o..o..o", "........"}),
            Map.entry("electrorefining_cell", new String[]{"#......#", "#......#", "#..oo..#", "#..oo..#", "#..oo..#", "########", "########", "........"}),
            Map.entry("molten_salt_electrolyzer", new String[]{"..#..#..", "..#..#..", "########", "#oooooo#", "#oooooo#", "#oooooo#", "########", "........"}),
            Map.entry("vacuum_outgasser", new String[]{"########", "#......#", "#.####.#", "#.#..#.#", "#.####.#", "#......#", "########", "...##..."}),
            Map.entry("arc_remelter", new String[]{"...##...", "...##...", "..o##o..", ".o.##.o.", "...##...", "########", "#......#", "########"}),
            Map.entry("fractionation_column", new String[]{"..####..", "..#..#..", "########", "..#..#..", "########", "..#..#..", "########", "..####.."}),
            Map.entry("volatile_vaporizer", new String[]{"..o..o..", ".o..o..o", "..o..o..", "########", "#......#", "#.o..o.#", "#......#", "########"}),
            Map.entry("vapor_deposition_furnace", new String[]{"........", "..####..", ".#....#.", ".#.##.#.", ".#.##.#.", ".#....#.", "########", "........"}),
            Map.entry("crystal_puller", new String[]{"...##...", "...##...", "...##...", "..####..", ".######.", ".######.", "..####..", "........"}),
            Map.entry("graphitizer", new String[]{"########", "#.#.#.##", "########", "##.#.#.#", "########", "#.#.#.##", "########", "........"}),
            Map.entry("centrifuge_cascade", new String[]{".#.#.#.#", ".#.#.#.#", ".#.#.#.#", ".#.#.#.#", ".#.#.#.#", ".#.#.#.#", "########", "........"}),
            Map.entry("hot_cell", new String[]{"########", "#......#", "#.o..o.#", "#..oo..#", "#..oo..#", "#.o..o.#", "#......#", "########"}));

    private MachineGlyphs() {
    }

    static String[] get(String machineId) {
        return GLYPHS.getOrDefault(machineId, FALLBACK);
    }
}
