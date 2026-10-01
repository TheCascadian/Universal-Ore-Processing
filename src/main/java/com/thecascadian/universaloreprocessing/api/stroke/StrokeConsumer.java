package com.thecascadian.universaloreprocessing.api.stroke;

/**
 * Implemented by anything that performs one action per received {@link Stroke}.
 * This package depends on nothing outside Minecraft so it can be extracted
 * into a shared library unchanged.
 */
@FunctionalInterface
public interface StrokeConsumer {

    /** Performs one action for the stroke; returns true if the stroke was used. */
    boolean accept(Stroke stroke);
}
