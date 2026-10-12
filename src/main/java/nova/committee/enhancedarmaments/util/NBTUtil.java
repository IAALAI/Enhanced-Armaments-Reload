package nova.committee.enhancedarmaments.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.UUID;

/**
 * 26.x 用 {@link DataComponents#CUSTOM_DATA} 承接旧版 ItemStack NBT。
 * 玩法字段（LEVEL / EXPERIENCE / RARITY / ABILITIES）仍写在同一份 CompoundTag 里。
 */
public final class NBTUtil {
    public static final String ITEM_ID = "EA_ITEM_ID";
    private static final String[] ENHANCEMENT_KEYS = {
            "EA_ENABLED", "LEVEL", "EXPERIENCE", "TOKENS", "RARITY", "ABILITIES",
            "EA_ARMOR_REDUCTION_PRD_FAILURES", "EA_ETERNAL_GUARD_UNTIL",
            "EA_ETERNAL_GUARD_LOCKED", "EA_ETERNAL_GUARD_TRIGGER_DAMAGE",
            "EA_REMEDIAL_NEXT_TICK", ITEM_ID
    };
    public static final String FUSION_PREFIX = "EA_FUSION_";

    private NBTUtil() {
    }

    public static CompoundTag loadStackNBT(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null || data.isEmpty() ? new CompoundTag() : data.copyTag();
    }

    public static void saveStackNBT(ItemStack stack, CompoundTag nbt) {
        if (nbt == null || nbt.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
            return;
        }
        CustomData.set(DataComponents.CUSTOM_DATA, stack, nbt);
    }

    public static void clearEnhancementData(ItemStack stack) {
        CompoundTag nbt = loadStackNBT(stack);
        boolean changed = clearEnhancementData(nbt);
        if (changed) {
            saveStackNBT(stack, nbt);
        }
    }

    /** Clears this mod's item data while preserving unrelated custom data. */
    public static boolean clearEnhancementData(CompoundTag nbt) {
        if (nbt == null) {
            return false;
        }
        boolean changed = false;
        for (String key : ENHANCEMENT_KEYS) {
            if (nbt.contains(key)) {
                nbt.remove(key);
                changed = true;
            }
        }
        changed |= removeKeysWithPrefix(nbt, "EA_ICD_");
        changed |= removeKeysWithPrefix(nbt, FUSION_PREFIX);
        return changed;
    }

    /** Removes combat-only state written by older builds while preserving progression data. */
    public static boolean clearLegacyCombatState(CompoundTag nbt) {
        if (nbt == null) {
            return false;
        }
        boolean changed = false;
        for (String key : new String[]{
                "TOKENS",
                "EA_ARMOR_REDUCTION_PRD_FAILURES", "EA_ETERNAL_GUARD_UNTIL",
                "EA_ETERNAL_GUARD_LOCKED", "EA_ETERNAL_GUARD_TRIGGER_DAMAGE",
                "EA_REMEDIAL_NEXT_TICK"
        }) {
            if (nbt.contains(key)) {
                nbt.remove(key);
                changed = true;
            }
        }
        return removeKeysWithPrefix(nbt, "EA_ICD_") || changed;
    }

    private static boolean removeKeysWithPrefix(CompoundTag nbt, String prefix) {
        boolean changed = false;
        for (String key : nbt.keySet().toArray(String[]::new)) {
            if (key.startsWith(prefix)) {
                nbt.remove(key);
                changed = true;
            }
        }
        return changed;
    }

    public static String ensureItemId(CompoundTag nbt) {
        String id = nbt.getStringOr(ITEM_ID, "");
        if (id.isEmpty()) {
            id = UUID.randomUUID().toString();
            nbt.putString(ITEM_ID, id);
        }
        return id;
    }

    public static String getItemId(CompoundTag nbt) {
        return nbt == null ? "" : nbt.getStringOr(ITEM_ID, "");
    }
}
