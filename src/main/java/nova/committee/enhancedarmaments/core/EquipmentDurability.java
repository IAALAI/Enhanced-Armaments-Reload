package nova.committee.enhancedarmaments.core;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/** Applies the equipment-level durability bonus without compounding prior bonuses. */
public final class EquipmentDurability {
    public static final int BONUS_PERCENT_PER_LEVEL = 10;

    private EquipmentDurability() {
    }

    public static int getScaledMaxDamage(int baseMaxDamage, int level) {
        if (baseMaxDamage <= 0) {
            return 0;
        }
        long multiplier = 100L + (long) Math.max(1, level) * BONUS_PERCENT_PER_LEVEL;
        long saturationThreshold = ((long) Integer.MAX_VALUE * 100L - 50L) / baseMaxDamage;
        if (multiplier >= saturationThreshold) {
            return Integer.MAX_VALUE;
        }
        long scaled = ((long) baseMaxDamage * multiplier + 50L) / 100L;
        return (int) Math.min(Integer.MAX_VALUE, Math.max(baseMaxDamage, scaled));
    }

    /**
     * Synchronizes the per-stack maximum durability and preserves the current damage ratio.
     * The prototype component is always used as the base so repeated calls cannot compound.
     */
    public static boolean synchronize(ItemStack stack, CompoundTag nbt) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        int baseMaxDamage = stack.getPrototype().getOrDefault(DataComponents.MAX_DAMAGE, 0);
        if (baseMaxDamage <= 0 || !stack.has(DataComponents.DAMAGE)) {
            return false;
        }
        int targetMaxDamage = getScaledMaxDamage(baseMaxDamage, Experience.getLevel(nbt));
        int currentMaxDamage = stack.getMaxDamage();
        if (targetMaxDamage == currentMaxDamage) {
            return false;
        }

        int currentDamage = stack.getDamageValue();
        int targetDamage = currentMaxDamage <= 0
                ? 0
                : (int) Math.round((double) currentDamage * targetMaxDamage / currentMaxDamage);
        stack.set(DataComponents.MAX_DAMAGE, targetMaxDamage);
        stack.setDamageValue(Math.min(targetDamage, targetMaxDamage));
        return true;
    }
}
