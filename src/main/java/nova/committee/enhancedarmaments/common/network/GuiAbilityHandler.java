package nova.committee.enhancedarmaments.common.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import nova.committee.enhancedarmaments.core.Ability;
import nova.committee.enhancedarmaments.core.Experience;
import nova.committee.enhancedarmaments.init.handler.CombatStateManager;
import nova.committee.enhancedarmaments.util.EAUtil;
import nova.committee.enhancedarmaments.util.NBTUtil;

public final class GuiAbilityHandler {
    private GuiAbilityHandler() {
    }

    public static void handle(ServerPlayer player, String abilityId, int selectedSlot) {
        if (selectedSlot != player.getInventory().getSelectedSlot()) {
            return;
        }
        ItemStack stack = player.getInventory().getSelectedItem();
        if (stack.isEmpty()) {
            return;
        }
        CompoundTag nbt = NBTUtil.loadStackNBT(stack);
        if (!Experience.isEnabled(nbt)) {
            return;
        }
        Ability ability = Ability.byId(abilityId).orElse(null);
        if (ability == null || !ability.isEnabled() || ability.getMaxLevel() <= 0) {
            return;
        }
        boolean categoryMatches = (EAUtil.canEnhanceWeapon(stack) && "weapon".equals(ability.getCategory()))
                || (EAUtil.canEnhanceArmor(stack) && "armor".equals(ability.getCategory()));
        if (!categoryMatches) {
            return;
        }
        if (!ability.hasCapacityForNextLevel(nbt)) {
            return;
        }
        int requiredLevels = ability.getExperienceLevelCost(nbt);
        if (requiredLevels > player.experienceLevel && !player.isCreative()) {
            return;
        }

        boolean changed = false;
        if (ability.hasAbility(nbt)) {
            if (ability.canUpgradeLevel(nbt)) {
                ability.setLevel(nbt, ability.getLevel(nbt) + 1);
                changed = true;
            }
        } else {
            ability.addAbility(nbt);
            changed = true;
        }
        if (changed) {
            if (!player.isCreative()) {
                player.giveExperienceLevels(-requiredLevels);
            }
            nbt.remove("TOKENS");
            CombatStateManager.primeAbilityCooldown(player, ability, nbt);
            NBTUtil.saveStackNBT(stack, nbt);
        }
    }
}
