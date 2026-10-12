package nova.committee.enhancedarmaments.core;

import com.google.gson.JsonObject;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.RandomSource;
import nova.committee.enhancedarmaments.util.RandomCollection;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Rarity {
    private static final List<String> TIER_NAMES = List.of(
            "basic", "uncommon", "rare", "ultra_rare", "legendary", "archaic"
    );
    public static final Rarity DEFAULT = new Rarity("default", "", 0, 0, 0);
    public static final List<Rarity> RARITIES = new ArrayList<>();
    public static final RandomCollection<Rarity> RANDOM_RARITIES = new RandomCollection<>();

    private final String name;
    private final ChatFormatting color;
    private final int hex;
    private final double weight;
    private final double effect;
    private boolean enabled = true;

    public Rarity(String name, String color, int hex, double weight, double effect) {
        this.name = name;
        this.color = parseColor(color);
        this.hex = hex;
        this.weight = weight;
        this.effect = effect;
    }

    public static Optional<Rarity> getRandomRarity(RandomSource random) {
        return RANDOM_RARITIES.next(random);
    }

    public static boolean hasRandomRarities() {
        return !RANDOM_RARITIES.isEmpty();
    }

    public static Rarity getRarity(CompoundTag nbt) {
        if (nbt == null || !nbt.contains("RARITY")) {
            return DEFAULT;
        }
        String stored = nbt.getStringOr("RARITY", "");
        for (Rarity rarity : RARITIES) {
            if (rarity.getName().equalsIgnoreCase(stored)) {
                return rarity;
            }
        }
        return DEFAULT;
    }

    public static void setRarity(CompoundTag nbt, String rarityName) {
        nbt.putString("RARITY", rarityName);
    }

    public static int getTier(CompoundTag nbt) {
        if (nbt == null) {
            return 0;
        }
        String stored = nbt.getStringOr("RARITY", "");
        for (int i = 0; i < TIER_NAMES.size(); i++) {
            if (TIER_NAMES.get(i).equalsIgnoreCase(stored)) {
                return i + 1;
            }
        }
        return 0;
    }

    public static String getNameForTier(int tier) {
        return tier >= 1 && tier <= TIER_NAMES.size() ? TIER_NAMES.get(tier - 1) : "default";
    }

    public static int getTierCount() {
        return TIER_NAMES.size();
    }

    public void setRarity(CompoundTag nbt) {
        nbt.putString("RARITY", name);
    }

    public String getName() {
        return name;
    }

    public ChatFormatting getColor() {
        return color == null ? ChatFormatting.WHITE : color;
    }

    public int getHex() {
        return hex;
    }

    public double getEffect() {
        return effect;
    }

    public double getWeight() {
        return weight;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    private static ChatFormatting parseColor(String color) {
        if (color == null || color.isEmpty()) {
            return ChatFormatting.WHITE;
        }
        try {
            return ChatFormatting.valueOf(color.trim().toUpperCase().replace(' ', '_'));
        } catch (IllegalArgumentException ignored) {
            return ChatFormatting.WHITE;
        }
    }

    public static Rarity loadFromJson(JsonObject json) {
        String name1 = GsonHelper.getAsString(json, "name");
        String color1 = GsonHelper.getAsString(json, "color");
        int hex1 = GsonHelper.getAsInt(json, "hex");
        double weight1 = GsonHelper.getAsDouble(json, "weight", 0.25D);
        double effect1 = GsonHelper.getAsDouble(json, "effect", 0.3D);
        Rarity rarity = new Rarity(name1, color1, hex1, weight1, effect1);
        rarity.setEnabled(GsonHelper.getAsBoolean(json, "enabled", true));
        return rarity;
    }

    public static void clear() {
        RARITIES.clear();
        RANDOM_RARITIES.clear();
    }
}
