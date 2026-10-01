package com.thecascadian.universaloreprocessing.api.stroke;

import net.minecraft.core.BlockPos;

/**
 * One discrete physical impact delivered to a block. A stroke carries only
 * where it came from and how hard it was; the receiver decides what it means.
 *
 * @param origin the block position the impact came from
 * @param force  impact strength, 1.0 being one ordinary strike
 */
public record Stroke(BlockPos origin, float force) {
}
