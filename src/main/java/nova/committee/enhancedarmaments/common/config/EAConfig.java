package nova.committee.enhancedarmaments.common.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import net.fabricmc.loader.api.FabricLoader;
import nova.committee.enhancedarmaments.Static;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class EAConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("enhancedarmaments/common.json");

    public static File DATA = new File();

    private EAConfig() {
    }

    public static void load() {
        try {
            Files.createDirectories(FILE.getParent());
            if (Files.notExists(FILE)) {
                DATA = new File();
                save();
                return;
            }
            try (Reader reader = Files.newBufferedReader(FILE, StandardCharsets.UTF_8)) {
                File parsed = GSON.fromJson(reader, File.class);
                if (parsed != null) {
                    DATA = parsed;
                }
            }
        } catch (IOException e) {
            Static.LOGGER.error("Failed to load config, using defaults", e);
            DATA = new File();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(FILE, StandardCharsets.UTF_8)) {
                GSON.toJson(DATA, writer);
            }
        } catch (IOException e) {
            Static.LOGGER.error("Failed to save config", e);
        }
    }

    public static final class File {
        @SerializedName("maxLevel")
        public int maxLevel = 10;
        @SerializedName("level1Experience")
        public int level1Experience = 100;
        @SerializedName("experienceMultiplier")
        public double experienceMultiplier = 1.8;
        @SerializedName("showDurabilityInTooltip")
        public boolean showDurabilityInTooltip = true;
        @SerializedName("showExpGetInChat")
        public boolean showExpGetInChat = true;
        @SerializedName("onlyModdedItems")
        public boolean onlyModdedItems = false;
        @SerializedName("itemBlacklist")
        public List<String> itemBlacklist = new ArrayList<>();
        @SerializedName("itemWhitelist")
        public List<String> itemWhitelist = new ArrayList<>();
        @SerializedName("extraItems")
        public List<String> extraItems = new ArrayList<>();

        public boolean fireAbility = true;
        public boolean frostAbility = true;
        public boolean poisonAbility = true;
        public boolean innateAbility = true;
        public boolean bombasticAbility = true;
        public boolean criticalpointAbility = true;
        public boolean illuminationAbility = true;
        public boolean etherealAbility = true;
        public boolean bloodthirstAbility = true;
        public boolean moltenAbility = true;
        public boolean frozenAbility = true;
        public boolean toxicAbility = true;
        public boolean eternalGuardianAbility = true;
        public boolean adrenalineAbility = true;
        public boolean frostwalkerAbility = true;
        public boolean beastialAbility = true;
        public boolean remedialAbility = true;
        public boolean hardenedAbility = true;

        public double firechance = 4;
        public double frostchance = 4;
        public double poisonchance = 4;
        public double innatechance = 4;
        public double bombasticchance = 4;
        public double criticalpointchance = 20;
        public double etherealchance = 4;
        public double moltenchance = 4;
        public double frostwalkerchance = 4;
        public double frozenchance = 4;
        public double toxicchance = 4;
        public double adrenalinechance = 4;
        public double hardenedchance = 4;

        /** Long-run proc rate for each armor piece's rarity damage reduction. */
        public double armorReductionProcChance = 0.20D;
    }
}
