package nova.committee.enhancedarmaments.init.handler;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.damagesource.DamageSource;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.world.item.ItemStack;
import nova.committee.enhancedarmaments.common.config.EAConfig;
import nova.committee.enhancedarmaments.core.Ability;
import nova.committee.enhancedarmaments.core.Experience;
import nova.committee.enhancedarmaments.util.EAUtil;
import nova.committee.enhancedarmaments.util.NBTUtil;

/**
 * 使用有效武器杀死目标时更新武器信息。用于更新经验、等级、能力等
 */
public class LivingDeathHandler {
    public static void onDeath(LivingEntity target, DamageSource source) {
        onLivingDeath(new DeathContext(target, source));
    }

    private static void onLivingDeath(DeathContext event) {
        if (event.getSource().getDirectEntity() instanceof Player player && !(event.getSource().getDirectEntity() instanceof FakePlayer)) {

            ItemStack stack = LivingHurtHandler.attackerStack(player);

            if (stack != ItemStack.EMPTY && EAUtil.canEnhanceMelee(stack)) {
                CompoundTag nbt = NBTUtil.loadStackNBT(stack);
                if (nbt.contains("EA_ENABLED")) {
                    if (Ability.ETHEREAL.hasAbility(nbt)) {
                        player.getInventory().getSelectedItem().setDamageValue((player.getInventory().getSelectedItem().getDamageValue() - (Ability.ETHEREAL.getLevel(nbt) * 2)));
                    }
                    addBonusExperience(event, player, stack, nbt);
                    updateLevel(player, stack, nbt);
                    NBTUtil.saveStackNBT(stack, nbt);
                }
            } else if (stack != ItemStack.EMPTY && EAUtil.canEnhanceRanged(stack)) {
                CompoundTag nbt = NBTUtil.loadStackNBT(stack);

                if (nbt.contains("EA_ENABLED")) {
                    if (Ability.ETHEREAL.hasAbility(nbt)) {
                        player.getInventory().getSelectedItem().setDamageValue((player.getInventory().getSelectedItem().getDamageValue() - (Ability.ETHEREAL.getLevel(nbt) * 2 + 1)));
                    }
                    addBonusExperience(event, player, stack, nbt);
                    updateLevel(player, stack, nbt);
                }
            }
        } else if (event.getSource().getDirectEntity() instanceof Arrow) {

            if (event.getSource().getEntity() instanceof Player player && event.getSource().getEntity() != null) {
                ItemStack stack = player.getInventory().getSelectedItem();

                if (stack != ItemStack.EMPTY) {
                    CompoundTag nbt = NBTUtil.loadStackNBT(stack);
                    addBonusExperience(event, player, stack, nbt);
                    updateLevel(player, stack, nbt);

                }
            }
        }
    }

    /**
     * 每次目标死亡时调用。根据目标的生命值增加额外经验。
     *
     * @param event
     * @param nbt
     */
    private static void addBonusExperience(DeathContext event, Player player, ItemStack stack, CompoundTag nbt) {
        if (Experience.getLevel(nbt) < EAConfig.DATA.maxLevel) {
            if (event.getEntity() != null) {
                LivingEntity target = event.getEntity();
                int bonusExperience = 0;

                if (target.getMaxHealth() < 10) bonusExperience = 3;
                else if (target.getMaxHealth() > 9 && target.getMaxHealth() < 20) bonusExperience = 6;
                else if (target.getMaxHealth() > 19 && target.getMaxHealth() < 50) bonusExperience = 15;
                else if (target.getMaxHealth() > 49 && target.getMaxHealth() < 100) bonusExperience = 50;
                else if (target.getMaxHealth() > 99) bonusExperience = 70;

                Experience.setExperience(nbt, Experience.getExperience(nbt) + bonusExperience);
                player.sendSystemMessage(Component.literal(stack.getDisplayName().getString() + ChatFormatting.GRAY + " " +
                        Component.translatable("enhancedarmaments.misc.exp.get").getString() + " " +
                        ChatFormatting.BLUE + "" + bonusExperience + ChatFormatting.GRAY + "!"));
            }
        }
    }

    /**
     * 每次有目标时调用。用于更新武器的等级。
     *
     * @param player
     * @param stack
     * @param nbt
     */
    private record DeathContext(LivingEntity entity, DamageSource source) {
        LivingEntity getEntity() { return entity; }
        DamageSource getSource() { return source; }
    }

    private static void updateLevel(Player player, ItemStack stack, CompoundTag nbt) {
        int level = Experience.getNextLevel(player, stack, nbt, Experience.getLevel(nbt), Experience.getExperience(nbt));
        Experience.setLevel(nbt, level);
        NBTUtil.saveStackNBT(stack, nbt);
    }
}
