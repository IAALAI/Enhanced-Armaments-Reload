package nova.committee.enhancedarmaments.core;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import nova.committee.enhancedarmaments.common.config.EAConfig;

import java.util.ArrayList;
import java.util.Optional;

public enum Ability {
    // weapon
    // active
    FIRE("weapon", "active", () -> EAConfig.DATA.fireAbility, ChatFormatting.RED, 0xFF5555, 1, 3),
    FROST("weapon", "active", () -> EAConfig.DATA.frostAbility, ChatFormatting.AQUA, 0x55FFFF, 1, 3),
    POISON("weapon", "active", () -> EAConfig.DATA.poisonAbility, ChatFormatting.DARK_GREEN, 0x00AA00, 1, 3),
    INNATE("weapon", "active", () -> EAConfig.DATA.innateAbility, ChatFormatting.DARK_RED, 0xAA0000, 2, 3),
    BOMBASTIC("weapon", "active", () -> EAConfig.DATA.bombasticAbility, ChatFormatting.GRAY, 0xAAAAAA, 3, 3),
    CRITICAL_POINT("weapon", "active", () -> EAConfig.DATA.criticalpointAbility, ChatFormatting.DARK_GRAY, 0x555555, 3, 3),
    // passive
    ILLUMINATION("weapon", "passive", () -> EAConfig.DATA.illuminationAbility, ChatFormatting.YELLOW, 0xFFFF55, 2, 1),
    ETHEREAL("weapon", "passive", () -> EAConfig.DATA.etherealAbility, ChatFormatting.GREEN, 0x55FF55, 2, 2),
    BLOODTHIRST("weapon", "passive", () -> EAConfig.DATA.bloodthirstAbility, ChatFormatting.DARK_PURPLE, 0xAA00AA, 3, 2),

    // armor
    // active
    MOLTEN("armor", "active", () -> EAConfig.DATA.moltenAbility, ChatFormatting.RED, 0xFF5555, 2, 2),
    FROZEN("armor", "active", () -> EAConfig.DATA.frozenAbility, ChatFormatting.AQUA, 0x55FFFF, 2, 2),
    TOXIC("armor", "active", () -> EAConfig.DATA.toxicAbility, ChatFormatting.DARK_GREEN, 0x00AA00, 2, 2),
    // passive
    BEASTIAL("armor", "passive", () -> EAConfig.DATA.beastialAbility, ChatFormatting.DARK_RED, 0xAA0000, 2, 1),
    REMEDIAL("armor", "passive", () -> EAConfig.DATA.remedialAbility, ChatFormatting.LIGHT_PURPLE, 0xFF55FF, 2, 2),
    HARDENED("armor", "passive", () -> EAConfig.DATA.hardenedAbility, ChatFormatting.GRAY, 0xAAAAAA, 3, 1),
    ADRENALINE("armor", "passive", () -> EAConfig.DATA.adrenalineAbility, ChatFormatting.GREEN, 0x55FF55, 3, 1),
    FROSTWALKER("armor", "passive", () -> EAConfig.DATA.frostwalkerAbility, ChatFormatting.DARK_BLUE, 0x55FF55, 3, 2),
    GALLOP("armor", "passive", () -> true, ChatFormatting.BLUE, 0xFF5555, 3, 2);


    public static final ArrayList<Ability> WEAPON_ABILITIES = new ArrayList<Ability>();
    public static final ArrayList<Ability> ARMOR_ABILITIES = new ArrayList<Ability>();
    public static final ArrayList<Ability> ALL_ABILITIES = new ArrayList<Ability>();
    public static int WEAPON_ABILITIES_COUNT = 0;
    public static int ARMOR_ABILITIES_COUNT = 0;

    static { refreshLists(); }

    public static void refreshLists() {
        WEAPON_ABILITIES.clear(); ARMOR_ABILITIES.clear(); ALL_ABILITIES.clear();
        WEAPON_ABILITIES_COUNT = 0; ARMOR_ABILITIES_COUNT = 0;
        for (int i = 0; i < Ability.values().length; i++) {
            Ability.ALL_ABILITIES.add(Ability.values()[i]);
            if (Ability.values()[i].getCategory().equals("weapon") && Ability.values()[i].isEnabled()) {
                Ability.WEAPON_ABILITIES.add(Ability.values()[i]);
                Ability.WEAPON_ABILITIES_COUNT++;
            } else if (Ability.values()[i].getCategory().equals("armor") && Ability.values()[i].isEnabled()) {
                Ability.ARMOR_ABILITIES.add(Ability.values()[i]);
                Ability.ARMOR_ABILITIES_COUNT++;
            }
        }
    }

    private final String category;
    private final String type;
    private final java.util.function.BooleanSupplier enabled;
    private final String color;
    private final int hex;
    private final int tier;
    private final int maxlevel;

    Ability(String category, String type, java.util.function.BooleanSupplier enabled, Object color, int hex, int tier, int maxlevel) {
        this.category = category;
        this.type = type;
        this.enabled = enabled;
        this.color = color.toString();
        this.hex = hex;
        this.tier = tier;
        this.maxlevel = maxlevel;
    }

    /**
     * 如果物品有能力，则返回 true。
     *
     * @param nbt nbt
     * @return
     */
    public boolean hasAbility(CompoundTag nbt) {
        return nbt != null && nbt.contains("ABILITIES") && nbt.getCompoundOrEmpty("ABILITIES").getIntOr(toString(), 0) > 0;
    }

    /**
     * 将指定的能力添加到物品中
     *
     * @param nbt nbt
     */
    public void addAbility(CompoundTag nbt) {
        var childTag = new CompoundTag();
        if (nbt.contains("ABILITIES")){
            childTag = nbt.getCompoundOrEmpty("ABILITIES");
            childTag.putInt("count", childTag.getIntOr("count", 0) + 1);
            //nbt.putInt("ABILITIES", nbt.getIntOr("ABILITIES", 0) + 1);
        }
        else {
            childTag.putInt("count", 1);
            //nbt.putInt("ABILITIES", 1);
        }
        childTag.putInt(toString(), 1);
        nbt.put("ABILITIES", childTag);

    }

    /**
     * 从堆叠的 NBT 中移除指定的能力
     *
     * @param nbt nbt
     */
    public void removeAbility(CompoundTag nbt) {
        var childTag = new CompoundTag();
        if (nbt.contains("ABILITIES")){
            childTag = nbt.getCompoundOrEmpty("ABILITIES");
            if (childTag.getIntOr("count", 0) > 0){
                childTag.putInt("count", childTag.getIntOr("count", 0) - 1);
            }
            childTag.remove(toString());
            nbt.put("ABILITIES", childTag);
        }



    }

    /**
     * 如果玩家有足够的经验来解锁该能力，则返回真
     *
     * @param nbt nbt
     * @param player player
     * @return
     */
    public boolean hasEnoughExp(Player player, CompoundTag nbt) {
        return getExpLevel(nbt) <= player.experienceLevel || player.isCreative();
    }

    /**
     * 返回需要经验等级的能力
     *
     * @param nbt nbt
     * @return exp
     */
    public int getExpLevel(CompoundTag nbt) {
        int requiredExpLevel = 0;
        var childTag = new CompoundTag();
        if (nbt.contains("ABILITIES")){
            childTag = nbt.getCompoundOrEmpty("ABILITIES");
            requiredExpLevel = (getTier() + getMaxLevel()) * (childTag.getIntOr("count", 0) + 1) - 1;
        }
        else
            requiredExpLevel = getTier() + getMaxLevel();
        return requiredExpLevel;
    }

    /**
     * 设置指定能力的等级
     *
     * @param nbt nbt
     * @param level level
     */
    public void setLevel(CompoundTag nbt, int level) {
        var childTag = nbt.getCompoundOrEmpty("ABILITIES");
        childTag.putInt(toString(), level);
        nbt.put("ABILITIES", childTag);

    }

    /**
     * 返回指定能力的等级。
     *
     * @param nbt nbt
     * @return level
     */
    public int getLevel(CompoundTag nbt) {
        var childTag = new CompoundTag();
        if (nbt != null){
            childTag = nbt.getCompoundOrEmpty("ABILITIES");
            return childTag.getIntOr(toString(), 0);
        }
        else
            return 0;
    }

    public boolean canUpgradeLevel(CompoundTag nbt) {
        return getLevel(nbt) < this.maxlevel;
    }

    public boolean isEnabled() { return enabled.getAsBoolean(); }

    public static Optional<Ability> byId(String id) {
        for (Ability ability : values()) if (ability.name().equals(id)) return Optional.of(ability);
        return Optional.empty();
    }

    public int getTier() {
        return tier;
    }

    public int getMaxLevel() {
        return maxlevel;
    }

    public String getColor() {
        return color;
    }

    public int getHex() {
        return hex;
    }

    public String getName() {
        return this.toString();
    }

    public String getName(CompoundTag nbt) {
        if (getLevel(nbt) == 2)
            return Component.translatable("enhancedarmaments.ability." + this).getString() + " II";
        else if (getLevel(nbt) == 3)
            return Component.translatable("enhancedarmaments.ability." + this).getString() + " III";
        else
            return Component.translatable("enhancedarmaments.ability." + this).getString();
    }

    public String getType() {
        return type;
    }

    public String getTypeName() {
        return Component.translatable("enhancedarmaments.ability.type." + type).getString();
    }

    public String getCategory() {
        return category;
    }
}
