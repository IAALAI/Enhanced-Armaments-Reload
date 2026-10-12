package nova.committee.enhancedarmaments.core;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import nova.committee.enhancedarmaments.common.config.EAConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public enum Ability {
    FIRE("weapon", "active", () -> EAConfig.DATA.fireAbility, ChatFormatting.RED, 0xFF5555, 3),
    FROST("weapon", "active", () -> EAConfig.DATA.frostAbility, ChatFormatting.AQUA, 0x55FFFF, 3),
    POISON("weapon", "active", () -> EAConfig.DATA.poisonAbility, ChatFormatting.DARK_GREEN, 0x00AA00, 3),
    INNATE("weapon", "active", () -> EAConfig.DATA.innateAbility, ChatFormatting.DARK_RED, 0xAA0000, 3),
    BOMBASTIC("weapon", "active", () -> EAConfig.DATA.bombasticAbility, ChatFormatting.GRAY, 0xAAAAAA, 3),
    CRITICAL_POINT("weapon", "active", () -> EAConfig.DATA.criticalpointAbility, ChatFormatting.DARK_GRAY, 0x555555, 3),
    ILLUMINATION("weapon", "passive", () -> EAConfig.DATA.illuminationAbility, ChatFormatting.YELLOW, 0xFFFF55, 1),
    ETHEREAL("weapon", "passive", () -> EAConfig.DATA.etherealAbility, ChatFormatting.GREEN, 0x55FF55, 2),
    BLOODTHIRST("weapon", "passive", () -> EAConfig.DATA.bloodthirstAbility, ChatFormatting.DARK_PURPLE, 0xAA00AA, 1),
    MOLTEN("armor", "active", () -> EAConfig.DATA.moltenAbility, ChatFormatting.RED, 0xFF5555, 2),
    FROZEN("armor", "active", () -> EAConfig.DATA.frozenAbility, ChatFormatting.AQUA, 0x55FFFF, 2),
    TOXIC("armor", "active", () -> EAConfig.DATA.toxicAbility, ChatFormatting.DARK_GREEN, 0x00AA00, 2),
    ETERNAL_GUARDIAN("armor", "active", () -> EAConfig.DATA.eternalGuardianAbility, ChatFormatting.GRAY, 0xAAAAAA, 1),
    BEASTIAL("armor", "passive", () -> EAConfig.DATA.beastialAbility, ChatFormatting.DARK_RED, 0xAA0000, 3),
    REMEDIAL("armor", "passive", () -> EAConfig.DATA.remedialAbility, ChatFormatting.LIGHT_PURPLE, 0xFF55FF, 3),
    HARDENED("armor", "passive", () -> EAConfig.DATA.hardenedAbility, ChatFormatting.GOLD, 0xFFAA00, 3),
    ADRENALINE("armor", "passive", () -> EAConfig.DATA.adrenalineAbility, ChatFormatting.GREEN, 0x55FF55, 3),
    GALLOP("armor", "passive", () -> true, ChatFormatting.WHITE, 0xFFFFFF, 3),
    FROSTWALKER("armor", "passive", () -> EAConfig.DATA.frostwalkerAbility, ChatFormatting.DARK_BLUE, 0x55FF55, 0);

    public static final ArrayList<Ability> WEAPON_ABILITIES = new ArrayList<>();
    public static final ArrayList<Ability> ARMOR_ABILITIES = new ArrayList<>();
    public static final ArrayList<Ability> ALL_ABILITIES = new ArrayList<>();
    public static final int CAPACITY_PER_LEVEL = 1;
    private static final Map<String, Ability> BY_ID = new HashMap<>();

    static {
        for (Ability ability : values()) {
            BY_ID.put(ability.name(), ability);
        }
        refreshLists();
    }

    public static Optional<Ability> byId(String id) {
        return Optional.ofNullable(id == null ? null : BY_ID.get(id));
    }

    private final String category;
    private final String type;
    private final Enabled enabled;
    private final ChatFormatting color;
    private final int hex;
    private final int maxlevel;

    Ability(String category, String type, Enabled enabled, ChatFormatting color, int hex, int maxlevel) {
        this.category = category;
        this.type = type;
        this.enabled = enabled;
        this.color = color;
        this.hex = hex;
        this.maxlevel = maxlevel;
    }

    public static void refreshLists() {
        WEAPON_ABILITIES.clear();
        ARMOR_ABILITIES.clear();
        ALL_ABILITIES.clear();
        for (Ability ability : values()) {
            ALL_ABILITIES.add(ability);
            if (!ability.isEnabled()) {
                continue;
            }
            if ("weapon".equals(ability.category)) {
                WEAPON_ABILITIES.add(ability);
            } else if ("armor".equals(ability.category)) {
                ARMOR_ABILITIES.add(ability);
            }
        }
    }

    public boolean isEnabled() {
        return enabled.get();
    }

    public boolean hasAbility(CompoundTag nbt) {
        return nbt != null && nbt.contains("ABILITIES") && nbt.getCompoundOrEmpty("ABILITIES").getIntOr(toString(), 0) > 0;
    }

    public void addAbility(CompoundTag nbt) {
        CompoundTag child = nbt.contains("ABILITIES") ? nbt.getCompoundOrEmpty("ABILITIES") : new CompoundTag();
        child.putInt("count", child.getIntOr("count", 0) + 1);
        child.putInt(toString(), 1);
        nbt.put("ABILITIES", child);
    }

    public void removeAbility(CompoundTag nbt) {
        if (!nbt.contains("ABILITIES")) {
            return;
        }
        CompoundTag child = nbt.getCompoundOrEmpty("ABILITIES");
        int count = child.getIntOr("count", 0);
        if (count > 0) {
            child.putInt("count", count - 1);
        }
        child.remove(toString());
        nbt.put("ABILITIES", child);
    }

    public boolean hasEnoughExp(Player player, CompoundTag nbt) {
        return getExperienceLevelCost(nbt) <= player.experienceLevel || player.isCreative();
    }

    /** Unlocking keeps the escalating price. Every upgrade costs one player experience level. */
    public int getExperienceLevelCost(CompoundTag nbt) {
        return hasAbility(nbt) ? 1 : getExpLevel(nbt);
    }

    public int getExpLevel(CompoundTag nbt) {
        int purchased = 0;
        for (Ability ability : values()) {
            if (ability.hasAbility(nbt)) {
                purchased++;
            }
        }
        long base = 1L + getMaxLevel();
        long cost = purchased == 0 ? base : base * (purchased + 1L) - 1L;
        return cost >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.max(0L, cost);
    }

    /** Every purchased ability level occupies exactly one point of capacity. */
    public static int getUsedCapacity(CompoundTag nbt) {
        long used = 0L;
        for (Ability ability : values()) {
            used += Math.max(0, ability.getLevel(nbt));
            if (used >= Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            }
        }
        return (int) used;
    }

    public static int getCapacity(CompoundTag nbt) {
        long capacity = (long) Experience.getLevel(nbt) * 2L;
        return capacity >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) capacity;
    }

    public boolean hasCapacityForNextLevel(CompoundTag nbt) {
        return (long) getUsedCapacity(nbt) + CAPACITY_PER_LEVEL <= getCapacity(nbt);
    }

    public void setLevel(CompoundTag nbt, int level) {
        CompoundTag child = nbt.getCompoundOrEmpty("ABILITIES");
        child.putInt(toString(), level);
        nbt.put("ABILITIES", child);
    }

    public int getLevel(CompoundTag nbt) {
        if (nbt == null) {
            return 0;
        }
        return nbt.getCompoundOrEmpty("ABILITIES").getIntOr(toString(), 0);
    }

    public boolean canUpgradeLevel(CompoundTag nbt) {
        return getLevel(nbt) < this.maxlevel;
    }

    public int getMaxLevel() {
        return maxlevel;
    }

    public String getColor() {
        return color.toString();
    }

    public int getHex() {
        return hex;
    }

    public String getName(CompoundTag nbt) {
        String base = Component.translatable("enhancedarmaments.ability." + this).getString();
        int level = getLevel(nbt);
        if (level == 2) {
            return base + " II";
        }
        if (level == 3) {
            return base + " III";
        }
        return base;
    }

    public String getType() {
        return type;
    }

    public String getCategory() {
        return category;
    }

    @FunctionalInterface
    private interface Enabled {
        boolean get();
    }
}
