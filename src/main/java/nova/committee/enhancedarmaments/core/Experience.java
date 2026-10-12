package nova.committee.enhancedarmaments.core;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import nova.committee.enhancedarmaments.common.config.EAConfig;

public final class Experience {
    private Experience() {
    }

    public static int getNextLevel(Player player, ItemStack stack, CompoundTag nbt, int currentLevel, int experience) {
        int newLevel = currentLevel;
        while (currentLevel < EAConfig.DATA.maxLevel && experience >= getMaxLevelExp(currentLevel)) {
            newLevel = currentLevel + 1;
            currentLevel++;
            if (PlayerNotificationSettings.get(player).showEquipmentLevelUp()) {
                player.sendSystemMessage(Component.literal(stack.getDisplayName().getString() + ChatFormatting.GRAY + " "
                        + Component.translatable("enhancedarmaments.misc.level.leveledup").getString() + " "
                        + ChatFormatting.GOLD + newLevel + ChatFormatting.GRAY + "!"));
            }
        }
        return newLevel;
    }

    /** Calculates an equipment level without messages or other side effects. */
    public static int getLevelForExperience(int experience) {
        int level = 1;
        while (level < EAConfig.DATA.maxLevel && experience >= getMaxLevelExp(level)) {
            level++;
        }
        return level;
    }

    public static int getLevel(CompoundTag nbt) {
        return nbt != null ? Math.max(nbt.getIntOr("LEVEL", 1), 1) : 1;
    }

    public static boolean canLevelUp(CompoundTag nbt) {
        return getLevel(nbt) < EAConfig.DATA.maxLevel;
    }

    public static void setLevel(CompoundTag nbt, int level) {
        if (nbt == null) {
            return;
        }
        if (level > 1) {
            nbt.putInt("LEVEL", level);
        } else {
            nbt.remove("LEVEL");
        }
    }

    public static int getNeededExpForNextLevel(CompoundTag nbt) {
        return getMaxLevelExp(getLevel(nbt)) - getExperience(nbt);
    }

    public static int getExperience(CompoundTag nbt) {
        return nbt != null ? nbt.getIntOr("EXPERIENCE", 0) : 0;
    }

    public static void setExperience(CompoundTag nbt, int experience) {
        if (nbt == null) {
            return;
        }
        if (experience > 0) {
            nbt.putInt("EXPERIENCE", experience);
        } else {
            nbt.remove("EXPERIENCE");
        }
    }

    public static int getMaxLevelExp(int level) {
        double value = EAConfig.DATA.level1Experience;
        for (int i = 1; i < level; i++) {
            value *= EAConfig.DATA.experienceMultiplier;
        }
        return (int) value;
    }

    public static void enable(CompoundTag nbt, boolean value) {
        if (nbt == null) {
            return;
        }
        if (value) {
            nbt.putBoolean("EA_ENABLED", true);
        } else {
            nbt.remove("EA_ENABLED");
        }
    }

    public static boolean isEnabled(CompoundTag nbt) {
        return nbt != null && nbt.getBooleanOr("EA_ENABLED", false);
    }
}
