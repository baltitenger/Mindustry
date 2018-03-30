package io.anuke.mindustry.content.blocks;

import io.anuke.mindustry.content.Items;
import io.anuke.mindustry.content.Liquids;
import io.anuke.mindustry.graphics.Fx;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.blocks.types.production.Cultivator;
import io.anuke.mindustry.world.blocks.types.production.Drill;
import io.anuke.mindustry.world.blocks.types.production.Fracker;
import io.anuke.mindustry.world.blocks.types.production.SolidPump;

public class ProductionBlocks {
    public static final Block

    ironDrill = new Drill("irondrill") {{
        tier = 1;
        drillTime = 400;
    }},

    reinforcedDrill = new Drill("reinforceddrill") {{
        tier = 2;
        drillTime = 360;
    }},

    steelDrill = new Drill("steeldrill") {{
        tier = 3;
        drillTime = 320;
    }},

    titaniumDrill = new Drill("titaniumdrill") {{
        tier = 4;
        drillTime = 280;
    }},

    laserdrill = new Drill("laserdrill") {{
        drillTime = 220;
        size = 2;
        powerUse = 0.2f;
        hasPower = true;
        tier = 5;
        updateEffect = Fx.pulverizeMedium;
        drillEffect = Fx.mineBig;
    }},

    nucleardrill = new Drill("nucleardrill") {{
        drillTime = 170;
        size = 3;
        powerUse = 0.32f;
        hasPower = true;
        tier = 5;
        updateEffect = Fx.pulverizeMedium;
        drillEffect = Fx.mineBig;
    }},

    plasmadrill = new Drill("plasmadrill") {{
        inputLiquid = Liquids.plasma;
        liquidRequired = true;
        drillTime = 110;
        size = 4;
        powerUse = 0.16f;
        hasLiquids = true;
        hasPower = true;
        tier = 5;
        updateEffect = Fx.pulverizeMedium;
        drillEffect = Fx.mineBig;
    }},

    waterextractor = new SolidPump("waterextractor") {{
        result = Liquids.water;
        powerUse = 0.2f;
        pumpAmount = 0.1f;
        size = 2;
        liquidCapacity = 30f;
    }},

    oilextractor = new Fracker("oilextractor") {{
        result = Liquids.oil;
        inputLiquid = Liquids.water;
        updateEffect = Fx.pulverize;
        updateEffectChance = 0.05f;
        inputLiquidUse = 0.3f;
        powerUse = 0.6f;
        pumpAmount = 0.06f;
        size = 3;
        liquidCapacity = 30f;
    }},

    cultivator = new Cultivator("cultivator") {{
        result = Items.biomatter;
        inputLiquid = Liquids.water;
        liquidUse = 0.2f;
        drillTime = 260;
        size = 2;
        hasLiquids = true;
        hasPower = true;
    }};
}