package nova.committee.enhancedarmaments.init.handler;

import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import nova.committee.enhancedarmaments.common.config.EAConfig;
import nova.committee.enhancedarmaments.core.Ability;
import nova.committee.enhancedarmaments.core.Experience;
import nova.committee.enhancedarmaments.core.EquipmentDurability;
import nova.committee.enhancedarmaments.core.PlayerNotificationSettings;
import nova.committee.enhancedarmaments.util.EAUtil;
import nova.committee.enhancedarmaments.util.NBTUtil;

public final class LivingDeathHandler {
    private LivingDeathHandler() {
    }

    public static void onDeath(LivingEntity killed, DamageSource source) {
        if (source.getDirectEntity() instanceof Player player && !(player instanceof FakePlayer)) {
            ItemStack stack = LivingHurtHandler.bowfriendlyhand == null
                    ? player.getMainHandItem()
                    : player.getItemInHand(LivingHurtHandler.bowfriendlyhand);
            applyKillRewards(player, stack, killed);
        } else if (source.getDirectEntity() instanceof AbstractArrow arrow
                && source.getEntity() instanceof Player player
                && !(player instanceof FakePlayer)) {
            applyKillRewards(player, LivingHurtHandler.findOwnedFiredWeapon(player, arrow.getWeaponItem()), killed);
        }
        if (killed instanceof Player player) {
            CombatStateManager.clear(player);
        }
    }

    private static void applyKillRewards(Player player, ItemStack stack, LivingEntity target) {
        if (stack.isEmpty()) {
            return;
        }
        CompoundTag nbt = NBTUtil.loadStackNBT(stack);
        if (!Experience.isEnabled(nbt)) {
            return;
        }
        if (EAUtil.canEnhanceMelee(stack) || EAUtil.canEnhanceRanged(stack)) {
            if (Ability.ETHEREAL.hasAbility(nbt) && roll(EAConfig.DATA.etherealchance)) {
                int repair = Ability.ETHEREAL.getLevel(nbt) * 2 + (EAUtil.canEnhanceRanged(stack) ? 1 : 0);
                stack.setDamageValue(Math.max(0, stack.getDamageValue() - repair));
            }
            if (Ability.BLOODTHIRST.hasAbility(nbt)) {
                float maximumHealing = Ability.BLOODTHIRST.getLevel(nbt) * 2.0F;
                player.heal(Math.min(target.getMaxHealth() * 0.20F, maximumHealing));
            }
            addBonusExperience(player, stack, nbt, target);
            Experience.setLevel(nbt, Experience.getNextLevel(player, stack, nbt, Experience.getLevel(nbt), Experience.getExperience(nbt)));
            EquipmentDurability.synchronize(stack, nbt);
            NBTUtil.saveStackNBT(stack, nbt);
        }
    }

    private static void addBonusExperience(Player player, ItemStack stack, CompoundTag nbt, LivingEntity target) {
        if (Experience.getLevel(nbt) >= EAConfig.DATA.maxLevel || target == null) {
            return;
        }
        float max = target.getMaxHealth();
        int bonus;
        if (max < 10) {
            bonus = 3;
        } else if (max < 20) {
            bonus = 6;
        } else if (max < 50) {
            bonus = 15;
        } else if (max < 100) {
            bonus = 50;
        } else {
            bonus = 70;
        }
        Experience.setExperience(nbt, Experience.getExperience(nbt) + bonus);
        if (PlayerNotificationSettings.get(player).showExperienceGain()) {
            player.sendSystemMessage(Component.literal(stack.getDisplayName().getString() + ChatFormatting.GRAY + " "
                    + Component.translatable("enhancedarmaments.misc.exp.get").getString() + " "
                    + ChatFormatting.BLUE + bonus + ChatFormatting.GRAY + "!"));
        }
    }

    private static boolean roll(double chance) {
        return (int) (Math.random() * chance) == 0;
    }
}
