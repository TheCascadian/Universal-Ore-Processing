package com.thecascadian.universaloreprocessing.data;

import com.thecascadian.universaloreprocessing.UniversalOreProcessing;
import com.thecascadian.universaloreprocessing.guide.Hints;
import com.thecascadian.universaloreprocessing.registry.RegistryHandler;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class OreProcessingLanguageProvider extends LanguageProvider {

    public OreProcessingLanguageProvider(PackOutput output) {
        super(output, UniversalOreProcessing.MODID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add("itemGroup." + UniversalOreProcessing.MODID, "Universal Ore Processing");

        add(RegistryHandler.CRUSHING_SLAB.get(), "Crushing Slab");
        add(RegistryHandler.SLUICE.get(), "Sluice");
        add(RegistryHandler.SLURRY_CAULDRON.get(), "Slurry Cauldron");
        add(RegistryHandler.HAMMER.get(), "Hammer");
        add(RegistryHandler.TRIP_HAMMER.get(), "Trip Hammer");
        add(RegistryHandler.QUERN.get(), "Quern");
        add(RegistryHandler.STIRRING_PADDLE.get(), "Stirring Paddle");
        add(RegistryHandler.SETTLING_TANK.get(), "Settling Tank");
        add(RegistryHandler.PANNING_TRAY.get(), "Panning Tray");
        add(RegistryHandler.GUIDE.get(), "Ore Processing Guide");

        add(RegistryHandler.CLUMPS.get(), "Clumps");
        add(RegistryHandler.DUST.get(), "Dust");
        add(RegistryHandler.SHARDS.get(), "Shards");
        add(RegistryHandler.CLUMPS.get().getDescriptionId() + ".named", "%s Clumps");
        add(RegistryHandler.DUST.get().getDescriptionId() + ".named", "%s Dust");
        add(RegistryHandler.SHARDS.get().getDescriptionId() + ".named", "%s Shards");

        add("tooltip.universaloreprocessing.smelts", "Smelts into %s %s");
        add("tooltip.universaloreprocessing.next.strike", "Next: strike on a Crushing Slab");
        add("tooltip.universaloreprocessing.next.grind", "Next: grind on a grindstone or Quern");
        add("tooltip.universaloreprocessing.next.stir", "Next: stir in a water cauldron or Settling Tank");
        add("tooltip.universaloreprocessing.next.none", "Final form: smelt it");
        add("tooltip.universaloreprocessing.washed", "Washed");
        add("tooltip.universaloreprocessing.guide", "Right-click to read");

        hints();
        guide();
    }

    private void hint(String key, String text) {
        add(Hints.PREFIX + key, text);
    }

    private void hints() {
        hint("slab.empty", "The slab is empty. Place raw ore on it first, then strike.");
        hint("slab.occupied", "The slab already holds %s. Take it off with an empty hand.");
        hint("slab.already_crushed", "Already crushed. %s");
        hint("grind.raw", "Too hard to grind. Strike it into clumps on a Crushing Slab first.");
        hint("grind.done", "Already ground. Stir dust into a full water cauldron.");
        hint("cauldron.not_full", "Fill the cauldron to the brim with water first.");
        hint("cauldron.too_coarse", "Too coarse to suspend in water. Grind it into dust first.");
        hint("slurry.settling", "Settling: stage %s of %s. Leave it undisturbed.");
        hint("slurry.other_material", "This holds %s slurry. Let it settle before adding %s.");
        hint("slurry.full", "It holds all the dust it can (%s). Stir it now.");
        hint("slurry.wrong_form", "Only dust can be stirred into slurry.");
        hint("slurry.stirred", "Stirred %s of %s times.");
        hint("slurry.needs_stir", "Stir the slurry with a stick or a Stirring Paddle.");
        hint("sluice.facing", "Sluices in a row must all face the same way.");
        hint("sluice.dry", "A Sluice only carries ore when waterlogged. Pour a water bucket into it.");
        hint("trip_hammer.no_slab", "A Trip Hammer strikes the block directly below it. Place it on a Crushing Slab.");
        hint("quern.other_material", "The quern still holds %s. Grind those first.");
        hint("quern.empty", "The quern is empty. Add clumps, then turn it.");
        hint("paddle.no_vessel", "A Stirring Paddle stirs the block below it. Place it on a cauldron or Settling Tank.");
        hint("paddle.nothing", "There is no unstirred slurry below the paddle.");
        hint("tank.no_water", "Fill the tank with a water bucket first.");
        hint("tank.full", "The tank holds all the dust it can. Stir it now.");
        hint("tank.no_dust", "Add dust to the water before stirring.");
        hint("tray.no_water", "Stand in water to pan.");
        hint("tray.no_load", "Hold clumps or dust in your other hand to pan them.");
        hint("tray.washed", "That stack has already been washed.");
    }

    private void guide(String key, String text) {
        add("guide." + UniversalOreProcessing.MODID + "." + key, text);
    }

    private void guide() {
        guide("chapter.intro", "The Ladder");
        guide("chapter.strike", "Striking");
        guide("chapter.grind", "Grinding");
        guide("chapter.stir", "Stirring");
        guide("chapter.settle", "Settling");
        guide("chapter.wash", "Washing");
        guide("chapter.smelt", "Smelting");
        guide("chapter.trouble", "Troubleshooting");
        guide("mistakes", "Common mistakes");

        guide("intro.1", "Ore can be smelted as it comes, or carried up a ladder of physical steps first. Each step is done by hand in the world, and each one raises what the ore smelts into.");
        guide("intro.2", "Strike raw ore into clumps, grind clumps into dust, stir dust into slurry, and let the slurry settle into shards. Any form can be smelted at any time; the higher the form, the more metal it gives.");
        guide("intro.3", "Every step has a machine that does the same work in bulk or from redstone. The machines are introduced in the chapter of the step they serve.");
        guide("intro.4", "Hover over an item in this book to see its name. When something does not work, a short note appears above the hotbar saying why; the last chapter lists them all.");

        guide("strike.1", "Place a raw ore or an ore block on a Crushing Slab, then strike it with a Hammer.");
        guide("strike.2", "It breaks after %s strikes. The floor of the slab wears from smooth stone toward cobblestone during the last strikes, so you can see how close it is. The clumps pop out on top of the slab.");
        guide("strike.waste", "Ore is never pure metal. For every %s items broken, the rock around the metal is left behind as one %s, popped out with the clumps.");
        guide("strike.3", "Set a Trip Hammer directly on top of a Crushing Slab. Each redstone pulse drops the head for one strike. A clock or a lever flicked by hand both work.");
        guide("strike.diagram.trip_hammer", "A lever beside the Trip Hammer, the Crushing Slab below it.");
        guide("strike.4", "A falling anvil also strikes a slab it lands on, one strike per fall.");
        guide("strike.diagram.anvil", "An anvil dropped onto a Crushing Slab.");
        guide("strike.mistake.1", "Striking an empty slab: place the ore first.");
        guide("strike.mistake.2", "Putting a Trip Hammer beside the slab: it must sit directly on top.");
        guide("strike.mistake.3", "Putting clumps back on the slab: they are already crushed. Grind them.");

        guide("grind.1", "Right-click a vanilla grindstone with clumps. Each click grinds one clump into one dust. A hopper above the grindstone feeds it, and the dust drops out below.");
        guide("grind.2", "Clumps that were washed stay washed as dust.");
        guide("grind.waste", "For every %s clumps ground, one %s of rock comes out with the dust.");
        guide("grind.3", "Fill a Quern with up to a stack of clumps, by hand or from a hopper above. Turn it with an empty hand or a redstone pulse; the peg on the top stone moves a quarter turn and one dust leaves through the spout.");
        guide("grind.diagram.quern", "A hopper above feeds clumps; a lever turns the stone; a hopper below catches the dust. Without a container below, the dust spills from the spout.");
        guide("grind.4", "A Quern holds one material at a time. Grind it empty before switching.");
        guide("grind.mistake.1", "Grinding raw ore: it is too hard. Strike it into clumps first.");
        guide("grind.mistake.2", "Adding a second material to a Quern that still holds the first.");
        guide("grind.mistake.3", "Turning an empty Quern: nothing comes out.");

        guide("stir.1", "Fill a cauldron to the brim with water and drop in dust. Add more dust of the same material, then stir with a stick.");
        guide("stir.2", "A cauldron holds up to %s dust. It turns to slurry after %s stirs.");
        guide("stir.3", "A Stirring Paddle rests on the rim of a cauldron or Settling Tank. Each redstone pulse, or a right-click with an empty hand, swings the paddle once and gives one stir.");
        guide("stir.diagram.paddle", "A Stirring Paddle on a cauldron, worked by a lever.");
        guide("stir.mistake.1", "Adding dust to a cauldron that is not completely full of water.");
        guide("stir.mistake.2", "Adding clumps or raw ore: only dust stays suspended.");
        guide("stir.mistake.3", "Mixing two materials in one cauldron.");

        guide("settle.1", "Once stirred, the slurry settles on its own in about %s seconds. The surface clears, then crystals grow on it in three visible stages.");
        guide("settle.2", "When the third stage of crystals shows, right-click to collect the shards. The cauldron is left empty.");
        guide("settle.waste", "The finest rock sinks under the crystals. For every %s dust settled you also collect one %s with the shards.");
        guide("settle.3", "A Settling Tank works like a cauldron that holds %s dust. Fill it with a water bucket, add dust, stir it, and wait. Hoppers may add dust from the side or top and take the shards and clay out from below.");
        guide("settle.diagram.tank", "A hopper feeds dust from the side, a paddle stirs from above, and a hopper below collects the shards and clay.");
        guide("settle.4", "The water is used up by the slurry. Refill the tank for the next batch.");
        guide("settle.mistake.1", "Collecting too early: wait for the third crystal stage.");
        guide("settle.mistake.2", "Adding dust to a dry tank: fill it with water first.");
        guide("settle.mistake.3", "Adding dust after stirring: start a new batch once the shards are collected.");

        guide("wash.1", "Washing is optional. Lay Sluices in a straight row, all facing the same way, and pour a water bucket into each one. Drop clumps or dust in: they sink, crawl downstream and tumble over each riffle.");
        guide("wash.diagram.row", "Four Sluices in a row, each holding water. Items travel the way the Sluices face.");
        guide("wash.2", "As an item clears the last riffle of the row it is washed, and may give byproducts. Longer rows raise the chance, up to %s Sluices.");
        guide("wash.byproducts", "Base chances for %s, per item per Sluice: %s.");
        guide("wash.byproducts.none", "%s has no byproducts in the current tables.");
        guide("wash.3", "Before you have a Sluice row, stand in water with clumps or dust in your other hand and hold use with a Panning Tray. It washes the stack as a row of one Sluice.");
        guide("wash.mistake.1", "Leaving the Sluices dry: they only carry ore when waterlogged.");
        guide("wash.mistake.2", "Turning one Sluice the other way: the row ends where the direction changes.");
        guide("wash.mistake.3", "Washing the same stack twice: each stack is washed only once.");

        guide("smelt.1", "Every form smelts in a furnace or blast furnace. Higher forms give more:");
        guide("smelt.ratio", "%s: %s each");
        guide("smelt.2", "Fractions are never lost to chance. A stack keeps a running total, so four clumps always give exactly five ingots.");
        guide("smelt.3", "Shards are the end of the ladder. Smelt them.");

        guide("trouble.intro", "Each note below matches a message shown above the hotbar.");
        guide("trouble.1.problem", "Nothing happens when I strike the slab.");
        guide("trouble.1.fix", "Place raw ore or an ore block on the slab first. Only one item rests on it at a time.");
        guide("trouble.2.problem", "The grindstone or Quern will not take my ore.");
        guide("trouble.2.fix", "Only clumps can be ground. Strike raw ore on a Crushing Slab first.");
        guide("trouble.3.problem", "Dust will not go into the cauldron.");
        guide("trouble.3.fix", "The cauldron must be full of water, and all its dust must be one material.");
        guide("trouble.4.problem", "The slurry will not give shards.");
        guide("trouble.4.fix", "It needs to be stirred first, then left until the third crystal stage shows.");
        guide("trouble.5.problem", "Items float straight over my Sluices.");
        guide("trouble.5.fix", "Each Sluice must be waterlogged, and all must face the same way.");
        guide("trouble.6.problem", "My Trip Hammer or Stirring Paddle only makes a dull knock.");
        guide("trouble.6.fix", "It works on the block directly below it, and that block needs something to work on.");
        guide("trouble.7.problem", "The Panning Tray does nothing.");
        guide("trouble.7.fix", "Stand in water and hold unwashed clumps or dust in your other hand.");
        guide("trouble.8.problem", "A form will not smelt.");
        guide("trouble.8.fix", "Check that ladder smelting is enabled in the config, and that the material is not excluded.");
    }
}
