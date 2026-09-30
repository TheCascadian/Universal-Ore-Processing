package com.thecascadian.universaloreprocessing.material;

import java.util.Locale;
import java.util.Optional;

/**
 * A physical or chemical property that decides which refining stations accept a
 * material, following the separation principle of each station: density,
 * hydrophobicity, magnetic susceptibility, reducibility, solubility and so on.
 */
public enum MaterialTrait {
    DENSE,
    SULFIDE,
    MAGNETIC,
    OXIDE,
    REFRACTORY,
    LEACHABLE,
    EXTRACTABLE,
    PRECIPITABLE,
    ELECTRO,
    ELECTROLYSIS,
    STRUCTURAL,
    SUPERALLOY,
    HYDROCARBON,
    CARBONYL,
    SEMICONDUCTOR,
    CRYSTAL,
    SINTERABLE,
    ISOTOPIC,
    RADIOACTIVE,
    NOBLE;

    public static Optional<MaterialTrait> byName(String name) {
        try {
            return Optional.of(valueOf(name.trim().toUpperCase(Locale.ROOT)));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
