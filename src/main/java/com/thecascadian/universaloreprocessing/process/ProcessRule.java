package com.thecascadian.universaloreprocessing.process;

import com.thecascadian.universaloreprocessing.item.MaterialItem.Stage;
import com.thecascadian.universaloreprocessing.material.MaterialTrait;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.Set;

/**
 * The fixed transformation one refining station performs: which stages it takes,
 * which materials qualify, what it consumes and what it returns.
 *
 * @param inputs         stages accepted in the input slot
 * @param traits         a material qualifies when it has at least one of these, empty meaning any
 * @param reagent        item consumed from the reagent slot per operation, or null
 * @param reagentCount   how many reagent items one operation needs
 * @param reagentUse     chance that the reagent is actually used up, below one for catalysts such as seed crystals
 * @param water          millibuckets of water drained per operation
 * @param primary        main output
 * @param secondary      byproduct output, or null
 * @param ticks          default ticks per operation
 * @param power          energy draw and fuel burn rate relative to the base machines
 * @param hazard         danger to nearby players while working
 */
public record ProcessRule(Set<Stage> inputs, Set<MaterialTrait> traits, @Nullable Reagent reagent, int reagentCount,
        double reagentUse, int water, Output primary, @Nullable Output secondary, int ticks, double power,
        Hazard hazard) {

    /**
     * One output of a station: either a material stage item or a plain reagent item.
     *
     * @param stage  the stage item to produce, or null for a reagent output
     * @param reagent the reagent item to produce, or null for a stage output
     * @param count  base item count
     * @param chance chance that the output appears, used for byproducts
     * @param noble  the stage item carries a precious metal of the pack instead of the input material
     */
    public record Output(@Nullable Stage stage, @Nullable Reagent reagent, int count, double chance, boolean noble) {

        public static Output stage(Stage stage, int count) {
            return new Output(stage, null, count, 1.0D, false);
        }

        public static Output stage(Stage stage, int count, double chance) {
            return new Output(stage, null, count, chance, false);
        }

        public static Output noble(Stage stage, int count, double chance) {
            return new Output(stage, null, count, chance, true);
        }

        public static Output reagent(Reagent reagent, int count, double chance) {
            return new Output(null, reagent, count, chance, false);
        }
    }

    public static Builder builder(Stage first, Stage... rest) {
        return new Builder(EnumSet.of(first, rest));
    }

    public static final class Builder {
        private final Set<Stage> inputs;
        private Set<MaterialTrait> traits = Set.of();
        private Reagent reagent;
        private int reagentCount = 1;
        private double reagentUse = 1.0D;
        private int water;
        private Output primary;
        private Output secondary;
        private int ticks = 200;
        private double power = 1.0D;
        private Hazard hazard = Hazard.NONE;

        private Builder(Set<Stage> inputs) {
            this.inputs = inputs;
        }

        public Builder traits(MaterialTrait first, MaterialTrait... rest) {
            this.traits = EnumSet.of(first, rest);
            return this;
        }

        public Builder reagent(Reagent reagent, int count) {
            this.reagent = reagent;
            this.reagentCount = count;
            return this;
        }

        public Builder reagentUse(double chance) {
            this.reagentUse = chance;
            return this;
        }

        public Builder water(int millibuckets) {
            this.water = millibuckets;
            return this;
        }

        public Builder primary(Output output) {
            this.primary = output;
            return this;
        }

        public Builder secondary(Output output) {
            this.secondary = output;
            return this;
        }

        public Builder ticks(int ticks) {
            this.ticks = ticks;
            return this;
        }

        public Builder power(double power) {
            this.power = power;
            return this;
        }

        public Builder hazard(Hazard hazard) {
            this.hazard = hazard;
            return this;
        }

        public ProcessRule build() {
            return new ProcessRule(inputs, traits, reagent, reagentCount, reagentUse, water, primary, secondary,
                    ticks, power, hazard);
        }
    }
}
