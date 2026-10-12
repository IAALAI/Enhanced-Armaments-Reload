package nova.committee.enhancedarmaments.init.handler;

import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.Enchantments;
import nova.committee.enhancedarmaments.common.config.EAConfig;
import nova.committee.enhancedarmaments.core.Ability;
import nova.committee.enhancedarmaments.core.Experience;
import nova.committee.enhancedarmaments.core.Rarity;
import nova.committee.enhancedarmaments.util.EAUtil;
import nova.committee.enhancedarmaments.util.NBTUtil;

public final class LivingUpdateHandler {
    private static int count;
    public static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD};
    private LivingUpdateHandler() { }
    public static void onLevelTick(ServerLevel level) {
        for (Player player : level.players()) {
            for (EquipmentSlot slot : ARMOR_SLOTS) {
                ItemStack stack = player.getItemBySlot(slot);
                if (stack.isEmpty() || !EAUtil.canEnhanceArmor(stack)) continue;
                CompoundTag nbt = NBTUtil.loadStackNBT(stack);
                if (Ability.REMEDIAL.hasAbility(nbt)) {
                    if (count < 120) count++;
                    else { count = 0; player.heal(Ability.REMEDIAL.getLevel(nbt)); }
                }
                if (Ability.FROSTWALKER.hasAbility(nbt) && (int) (Math.random() * EAConfig.DATA.frostwalkerchance) == 0) {
                    level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.FROST_WALKER)
                            .value().runLocationChangedEffects(level, Ability.FROSTWALKER.getLevel(nbt),
                                    new EnchantedItemInUse(stack, slot, player), player);
                }
            }
            // Legacy behavior: equipment gains a rarity while it is in the inventory.
            for (ItemStack stack : player.getInventory().getNonEquipmentItems()) {
                if (!EAUtil.canEnhance(stack)) continue;
                CompoundTag nbt = NBTUtil.loadStackNBT(stack);
                String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
                boolean allowed = !EAConfig.DATA.itemBlacklist.contains(id);
                if (!EAConfig.DATA.itemWhitelist.isEmpty()) allowed = EAConfig.DATA.itemWhitelist.contains(id);
                if (allowed && !Experience.isEnabled(nbt)) {
                    Experience.enable(nbt, true);
                    if (Rarity.getRarity(nbt) == Rarity.DEFAULT) Rarity.getRandomRarity(level.getRandom()).setRarity(nbt);
                    NBTUtil.saveStackNBT(stack, nbt);
                }
            }
        }
    }
}
