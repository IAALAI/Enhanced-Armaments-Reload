package nova.committee.enhancedarmaments.common.network;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import nova.committee.enhancedarmaments.core.Ability;
import nova.committee.enhancedarmaments.core.Experience;
import nova.committee.enhancedarmaments.util.EAUtil;
import nova.committee.enhancedarmaments.util.NBTUtil;

public final class GuiAbilityHandler {
    private GuiAbilityHandler() { }
    public static void handle(ServerPlayer player, String abilityId, int selectedSlot) {
        if (selectedSlot != player.getInventory().getSelectedSlot()) return;
        ItemStack stack = player.getInventory().getSelectedItem();
        if (!EAUtil.canEnhance(stack)) return;
        CompoundTag nbt = NBTUtil.loadStackNBT(stack);
        Ability ability = Ability.byId(abilityId).orElse(null);
        if (!Experience.isEnabled(nbt) || ability == null || !ability.isEnabled()) return;
        if (!(EAUtil.canEnhanceWeapon(stack) && "weapon".equals(ability.getCategory()))
                && !(EAUtil.canEnhanceArmor(stack) && "armor".equals(ability.getCategory()))) return;
        if (ability.hasAbility(nbt)) {
            if (!ability.canUpgradeLevel(nbt) || Experience.getAbilityTokens(nbt) < ability.getTier()) return;
            ability.setLevel(nbt, ability.getLevel(nbt) + 1);
            Experience.setAbilityTokens(nbt, Experience.getAbilityTokens(nbt) - ability.getTier());
        } else {
            if (!ability.hasEnoughExp(player, nbt)) return;
            ability.addAbility(nbt);
            // Preserve the legacy post-purchase XP deduction (including its count-based formula).
            if (!player.isCreative()) player.giveExperienceLevels(-ability.getExpLevel(nbt) + 1);
        }
        NBTUtil.saveStackNBT(stack, nbt);
    }
}
