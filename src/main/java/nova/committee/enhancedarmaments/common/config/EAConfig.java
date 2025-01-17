package nova.committee.enhancedarmaments.common.config;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.registries.ForgeRegistries;
import nova.committee.enhancedarmaments.Static;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

public class EAConfig {
    public static final CFile CONFIG;
    public static final ForgeConfigSpec SPEC;
    public static int maxLevel;
    public static int level1Experience;
    public static double experienceMultiplier;
    public static boolean showDurabilityInTooltip;
    public static List<Item> itemBlacklist;
    public static List<Item> itemWhitelist;
    public static List<Item> extraItems;
    public static boolean onlyModdedItems;
    public static boolean fireAbility;
    public static boolean frostAbility;
    public static boolean poisonAbility;
    public static boolean innateAbility;
    public static boolean bombasticAbility;
    public static boolean criticalpointAbility;
    public static boolean illuminationAbility;
    public static boolean etherealAbility;
    public static boolean bloodthirstAbility;
    public static boolean moltenAbility;
    public static boolean frozenAbility;
    public static boolean toxicAbility;
    public static boolean adrenalineAbility;
    public static boolean frostwalkerAbility;
    public static boolean beastialAbility;
    public static boolean remedialAbility;
    public static boolean hardenedAbility;
    public static double firechance;
    public static double frostchance;
    public static double poisonchance;
    public static double innatechance;
    public static double bombasticchance;
    public static double criticalpointchance;
    public static double moltenchance;
    public static double frostwalkerchance;
    public static double frozenchance;
    public static double toxicchance;
    public static double adrenalinechance;
    public static double hardenedchance;


    static {
        final Pair<CFile, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(CFile::new);
        SPEC = specPair.getRight();
        CONFIG = specPair.getLeft();
    }

    public static void load() {
        maxLevel = CONFIG.maxLevel.get();
        level1Experience = CONFIG.level1Experience.get();
        experienceMultiplier = CONFIG.experienceMultiplier.get();
        showDurabilityInTooltip = CONFIG.showDurabilityInTooltip.get();
        onlyModdedItems = CONFIG.onlyModdedItems.get();

        itemBlacklist = parseItemList(CONFIG.itemBlacklist.get());
        itemWhitelist = parseItemList(CONFIG.itemWhitelist.get());
        extraItems = parseItemList(CONFIG.extraItems.get());

        fireAbility = CONFIG.fireAbility.get();
        frostAbility = CONFIG.frostAbility.get();
        poisonAbility = CONFIG.poisonAbility.get();
        innateAbility = CONFIG.innateAbility.get();
        bombasticAbility = CONFIG.bombasticAbility.get();
        criticalpointAbility = CONFIG.criticalpointAbility.get();
        illuminationAbility = CONFIG.illuminationAbility.get();
        etherealAbility = CONFIG.etherealAbility.get();
        bloodthirstAbility = CONFIG.bloodthirstAbility.get();
        moltenAbility = CONFIG.moltenAbility.get();
        frozenAbility = CONFIG.frozenAbility.get();
        toxicAbility = CONFIG.toxicAbility.get();
        adrenalineAbility = CONFIG.adrenalineAbility.get();
        frostwalkerAbility = CONFIG.frostwalkerAbility.get();
        beastialAbility = CONFIG.beastialAbility.get();
        remedialAbility = CONFIG.remedialAbility.get();
        hardenedAbility = CONFIG.hardenedAbility.get();
        fireAbility = CONFIG.fireAbility.get();

        firechance = CONFIG.firechance.get();
        frostchance = CONFIG.frostchance.get();
        poisonchance = CONFIG.poisonchance.get();
        innatechance = CONFIG.innatechance.get();
        bombasticchance = CONFIG.bombasticchance.get();
        criticalpointchance = CONFIG.criticalpointchance.get();
        moltenchance = CONFIG.moltenchance.get();
        frostwalkerchance = CONFIG.frostwalkerchance.get();
        frozenchance = CONFIG.frozenchance.get();
        toxicchance = CONFIG.toxicchance.get();
        adrenalinechance = CONFIG.adrenalinechance.get();
        hardenedchance = CONFIG.hardenedchance.get();


    }

    private static List<Item> parseItemList(List<String> lst) {
        List<Item> exp = new ArrayList<>(lst.size());
        for (String s : lst) {
            Item i = ForgeRegistries.ITEMS.getValue(new ResourceLocation(s));
            if (i == null || i == Items.AIR) {
                Static.LOGGER.error("Invalid config entry {} will be ignored from blacklist.", s);
                continue;
            }
            exp.add(i);
        }
        return exp;
    }

    public static class CFile {
        public ForgeConfigSpec.ConfigValue<Integer> maxLevel;
        public ForgeConfigSpec.ConfigValue<Integer> level1Experience;
        public ForgeConfigSpec.ConfigValue<Double> experienceMultiplier;

        public ForgeConfigSpec.BooleanValue showDurabilityInTooltip;
        public ForgeConfigSpec.ConfigValue<List<String>> itemBlacklist;
        public ForgeConfigSpec.ConfigValue<List<String>> itemWhitelist;
        public ForgeConfigSpec.ConfigValue<List<String>> extraItems;
        public ForgeConfigSpec.BooleanValue onlyModdedItems;

        //weapon
        public ForgeConfigSpec.BooleanValue fireAbility;
        public ForgeConfigSpec.BooleanValue frostAbility;
        public ForgeConfigSpec.BooleanValue poisonAbility;
        public ForgeConfigSpec.BooleanValue innateAbility;
        public ForgeConfigSpec.BooleanValue bombasticAbility;
        public ForgeConfigSpec.BooleanValue criticalpointAbility;
        public ForgeConfigSpec.BooleanValue illuminationAbility;
        public ForgeConfigSpec.BooleanValue etherealAbility;
        public ForgeConfigSpec.BooleanValue bloodthirstAbility;

        //armor
        public ForgeConfigSpec.BooleanValue moltenAbility;
        public ForgeConfigSpec.BooleanValue frozenAbility;
        public ForgeConfigSpec.BooleanValue toxicAbility;
        public ForgeConfigSpec.BooleanValue adrenalineAbility;
        public ForgeConfigSpec.BooleanValue frostwalkerAbility;
        public ForgeConfigSpec.BooleanValue beastialAbility;
        public ForgeConfigSpec.BooleanValue remedialAbility;
        public ForgeConfigSpec.BooleanValue hardenedAbility;

        public ForgeConfigSpec.ConfigValue<Double> firechance;
        public ForgeConfigSpec.ConfigValue<Double> frostchance;
        public ForgeConfigSpec.ConfigValue<Double> poisonchance;
        public ForgeConfigSpec.ConfigValue<Double> innatechance;
        public ForgeConfigSpec.ConfigValue<Double> bombasticchance;
        public ForgeConfigSpec.ConfigValue<Double> criticalpointchance;
        public ForgeConfigSpec.ConfigValue<Double> moltenchance;
        public ForgeConfigSpec.ConfigValue<Double> frostwalkerchance;
        public ForgeConfigSpec.ConfigValue<Double> frozenchance;
        public ForgeConfigSpec.ConfigValue<Double> toxicchance;
        public ForgeConfigSpec.ConfigValue<Double> adrenalinechance;
        public ForgeConfigSpec.ConfigValue<Double> hardenedchance;



        public CFile(ForgeConfigSpec.Builder builder) {
            buildMain(builder);
            buildMisc(builder);
            buildAbilities(builder);
            buildAbilityChance(builder);
        }

        private void buildMain(ForgeConfigSpec.Builder builder) {
            builder.push("experience");

            maxLevel = builder
                    .comment("Sets the maximum level cap for weapons and armor. Default: 10")
                    .define("maxLevel", 10);

            level1Experience = builder
                    .comment("The experience amount needed for the first level(1). Default: 100")
                    .define("level1Experience", 100);

            experienceMultiplier = builder
                    .comment("The experience multiplier for each level based on the first level experience. Default: 1.8")
                    .define("experienceMultiplier", 1.8);

            builder.pop();
        }

        private void buildMisc(ForgeConfigSpec.Builder builder) {
            builder.push("miscellaneous");

            showDurabilityInTooltip = builder
                    .comment("Determines whether or not durability will be displayed in tooltips. Default: true")
                    .define("showDurabilityInTooltip", true);

            itemBlacklist = builder
                    .comment("Items in this blacklist will not gain the leveling systems. Useful for very powerful items or potential conflicts. Style should be 'modid:item'")
                    .define("itemBlacklist", new ArrayList<>());

            itemWhitelist = builder
                    .comment("This is item whitelist, basically. If you don't want a whitelist, just leave this empty. If you want a whitelist, fill it with items you want. Style should be 'modid:item'")
                    .define("itemWhitelist", new ArrayList<>());

            extraItems = builder
                    .comment("This is an extra item list to add custom support for such modded items. Be careful on this, it may crash if the item can't be enhanced. Style should be 'modid:item'")
                    .define("extraItems", new ArrayList<>());

            onlyModdedItems = builder
                    .comment("Determines if the vanilla items won't be affected by this mod. Default: false")
                    .define("onlyModdedItems", false);

            builder.pop();
        }

        private void buildAbilities(ForgeConfigSpec.Builder builder) {
            builder.push("abilities");

            // weapons
            fireAbility = builder
                    .comment("Determines whether or not the specific ability will be present in-game. Default: true")
                    .define("fireAbility", true);

            frostAbility = builder
                    .comment("Determines whether or not the specific ability will be present in-game. Default: true")
                    .define("frostAbility", true);

            poisonAbility = builder
                    .comment("Determines whether or not the specific ability will be present in-game. Default: true")
                    .define("poisonAbility", true);

            innateAbility = builder
                    .comment("Determines whether or not the specific ability will be present in-game. Default: true")
                    .define("innateAbility", true);

            bombasticAbility = builder
                    .comment("Determines whether or not the specific ability will be present in-game. Default: true")
                    .define("bombasticAbility", true);

            criticalpointAbility = builder
                    .comment("Determines whether or not the specific ability will be present in-game. Default: true")
                    .define("criticalpointAbility", true);

            illuminationAbility = builder
                    .comment("Determines whether or not the specific ability will be present in-game. Default: true")
                    .define("illuminationAbility", true);

            etherealAbility = builder
                    .comment("Determines whether or not the specific ability will be present in-game. Default: true")
                    .define("etherealAbility", true);

            bloodthirstAbility = builder
                    .comment("Determines whether or not the specific ability will be present in-game. Default: true")
                    .define("bloodthirstAbility", true);

            //armor
            moltenAbility = builder
                    .comment("Determines whether or not the specific ability will be present in-game. Default: true")
                    .define("moltenAbility", true);

            frozenAbility = builder
                    .comment("Determines whether or not the specific ability will be present in-game. Default: true")
                    .define("frozenAbility", true);

            moltenAbility = builder
                    .comment("Determines whether or not the specific ability will be present in-game. Default: true")
                    .define("moltenAbility", true);

            frostwalkerAbility = builder
                    .comment("Determines whether or not the specific ability will be present in-game. Default: true")
                    .define("frostwalkerAbility", true);

            toxicAbility = builder
                    .comment("Determines whether or not the specific ability will be present in-game. Default: true")
                    .define("toxicAbility", true);

            adrenalineAbility = builder
                    .comment("Determines whether or not the specific ability will be present in-game. Default: true")
                    .define("adrenalineAbility", true);

            beastialAbility = builder
                    .comment("Determines whether or not the specific ability will be present in-game. Default: true")
                    .define("beastialAbility", true);

            remedialAbility = builder
                    .comment("Determines whether or not the specific ability will be present in-game. Default: true")
                    .define("remedialAbility", true);

            hardenedAbility = builder
                    .comment("Determines whether or not the specific ability will be present in-game. Default: true")
                    .define("hardenedAbility", true);

            builder.pop();
        }

        private void buildAbilityChance(ForgeConfigSpec.Builder builder) {
            builder.push("abilitychances");

            firechance = builder
                    .comment("Determines how rare the Fire ability will occur. (Higher values=lower occurance) Default: 4")
                    .define("firechance", 4D);

            frostchance = builder
                    .comment("Determines how rare the Frost ability will occur. (Higher values=lower occurance) Default: 4")
                    .define("frostchance", 4d);

            poisonchance = builder
                    .comment("Determines how rare the Poison ability will occur. (Higher values=lower occurance) Default: 4")
                    .define("poisonchance", 4d);

            innatechance = builder
                    .comment("Determines how rare the Innate ability will occur. (Higher values=lower occurance) Default: 4")
                    .define("innatechance", 4d);

            bombasticchance = builder
                    .comment("Determines how rare the Bombasitc ability will occur. (Higher values=lower occurance) Default: 4")
                    .define("bombasticchance", 4d);

            criticalpointchance = builder
                    .comment("Determines how rare the Critical Point ability will occur. (Higher values=lower occurance) Default: 4")
                    .define("criticalpointchance", 4d);

            moltenchance = builder
                    .comment("Determines how rare the Molten ability will occur. (Higher values=lower occurance) Default: 4")
                    .define("moltenchance", 4d);

            frostwalkerchance = builder
                    .comment("Determines how rare the Molten ability will occur. (Higher values=lower occurance) Default: 4")
                    .define("moltenchance", 4d);

            frozenchance = builder
                    .comment("Determines how rare the Frozen ability will occur. (Higher values=lower occurance) Default: 4")
                    .define("frozenchance", 4d);

            toxicchance = builder
                    .comment("Determines how rare the Toxic ability will occur. (Higher values=lower occurance) Default: 4")
                    .define("toxicchance", 4d);

            adrenalinechance = builder
                    .comment("Determines how rare the Adrinalin ability will occur. (Higher values=lower occurance) Default: 4")
                    .define("adrenalinechance", 4d);

            hardenedchance = builder
                    .comment("Determines how rare the Harden ability will occur. (Higher values=lower occurance) Default: 4")
                    .define("hardenedchance", 4d);

            builder.pop();
        }

    }
}
