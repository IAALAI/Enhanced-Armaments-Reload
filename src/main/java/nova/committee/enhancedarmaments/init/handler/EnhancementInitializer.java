package nova.committee.enhancedarmaments.init.handler;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import nova.committee.enhancedarmaments.core.Experience;
import nova.committee.enhancedarmaments.core.EquipmentDurability;
import nova.committee.enhancedarmaments.core.Rarity;
import nova.committee.enhancedarmaments.core.RarityFusion;
import nova.committee.enhancedarmaments.util.EAUtil;
import nova.committee.enhancedarmaments.util.NBTUtil;

/** Initializes only the weapon or equipped armor involved in a combat event. */
public final class EnhancementInitializer {
    private EnhancementInitializer() {
    }

    public static CompoundTag initialize(Player player, ItemStack stack) {
        RarityFusion.resolve(stack, player.level().getRandom());
        CompoundTag nbt = NBTUtil.loadStackNBT(stack);
        if (stack.isEmpty() || !EAUtil.canEnhance(stack)) {
            return nbt;
        }

        boolean changed = NBTUtil.clearLegacyCombatState(nbt);
        if (!Experience.isEnabled(nbt)) {
            Experience.enable(nbt, true);
            changed = true;
        }
        if (NBTUtil.getItemId(nbt).isEmpty()) {
            NBTUtil.ensureItemId(nbt);
            changed = true;
        }
        if (Rarity.getRarity(nbt) == Rarity.DEFAULT) {
            var randomRarity = Rarity.getRandomRarity(player.level().getRandom());
            if (randomRarity.isPresent()) {
                randomRarity.get().setRarity(nbt);
                changed = true;
            }
        }
        if (changed) {
            NBTUtil.saveStackNBT(stack, nbt);
        }
        EquipmentDurability.synchronize(stack, nbt);
        return nbt;
    }
}
