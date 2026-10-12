package nova.committee.enhancedarmaments.init.handler;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;
import nova.committee.enhancedarmaments.common.config.EAConfig;
import nova.committee.enhancedarmaments.core.Ability;
import nova.committee.enhancedarmaments.core.Experience;
import nova.committee.enhancedarmaments.core.Rarity;
import nova.committee.enhancedarmaments.util.EAUtil;
import nova.committee.enhancedarmaments.util.NBTUtil;


public class LivingHurtHandler {
    // Retain the Forge branch's bow-hand selection behavior for this compatibility release.
    public static InteractionHand bowfriendlyhand;

    public static ItemStack attackerStack(Player player) {
        return player.getItemInHand(bowfriendlyhand == null ? player.getUsedItemHand() : bowfriendlyhand);
    }

    public static float onHurt(LivingEntity target, DamageSource source, float amount) {
        HurtContext event = new HurtContext(target, source, amount);
        apply(event);
        return event.getAmount();
    }

    private static void apply(HurtContext event) {
        if (event.getSource().getDirectEntity() instanceof Player player && !(event.getSource().getDirectEntity() instanceof FakePlayer))
        //PLAYER IS ATTACKER
        {
            LivingEntity target = event.getEntity();
            ItemStack stack = attackerStack(player);

            if (stack != ItemStack.EMPTY && EAUtil.canEnhanceWeapon(stack)) {
                CompoundTag nbt = NBTUtil.loadStackNBT(stack);


                if (nbt.contains("EA_ENABLED")) {
                    updateExperience(nbt, event.getAmount());
                    useRarity(event, stack, nbt);
                    useWeaponAbilities(event, player, target, nbt);
                    updateLevel(player, stack, nbt);
                }
            }
        } else if (event.getEntity() instanceof Player player) {//PLAYER IS GETTING HURT
            Entity target = event.getSource().getEntity();

            for (EquipmentSlot slot : LivingUpdateHandler.ARMOR_SLOTS) {
                ItemStack stack = player.getItemBySlot(slot);
                if (stack != null) {
                    if (EAUtil.canEnhanceArmor(stack)) {
                        CompoundTag nbt = NBTUtil.loadStackNBT(stack);

                        if (nbt.contains("EA_ENABLED")) {
                            if (EAUtil.isDamageSourceAllowed(event.getSource())) {
                                if (event.getAmount() < (player.getMaxHealth() + player.getArmorValue()))
                                    updateExperience(nbt, event.getAmount());
                                else
                                    updateExperience(nbt, 1);
                                updateLevel(player, stack, nbt);
                            }
                            useRarity(event, stack, nbt);
                            useArmorAbilities(event, player, target, nbt);
                        }
                    }
                }
            }
        }
    }

    /**
     * 每次目标受伤时调用。用于为造成伤害的武器增加经验
     *
     * @param nbt 数据
     */
    private static void updateExperience(CompoundTag nbt, float dealedDamage) {
        if (Experience.getLevel(nbt) < EAConfig.DATA.maxLevel) {
            Experience.setExperience(nbt, Experience.getExperience(nbt) + 1 + (int) dealedDamage / 4);
        }
    }

    /**
     * 每次目标受伤时调用。用于造成更多伤害或更少伤害。
     * 根据稀有度解决定造成的伤害
     *
     * @param nbt 数据
     */
    private static void useRarity(HurtContext event, ItemStack stack, CompoundTag nbt) {
        Rarity rarity = Rarity.getRarity(nbt);

        if (rarity != Rarity.DEFAULT)
            if (EAUtil.canEnhanceMelee(stack)) {
                double[] damage = {0D};
                stack.forEachModifier(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
                    if (attribute.is(Attributes.ATTACK_DAMAGE)) damage[0] = modifier.amount();
                });
                event.setAmount((float) (event.getAmount() + damage[0] * rarity.getEffect()));
            } else if (EAUtil.canEnhanceRanged(stack)) {
                float newdamage = (float) (event.getAmount() + (event.getAmount() * rarity.getEffect() / 3));
                event.setAmount(newdamage);
            } else if (EAUtil.canEnhanceArmor(stack))
                event.setAmount((float) (event.getAmount() / (1.0F + (rarity.getEffect() / 5F))));
    }

    /**
     * 每次目标受伤时调用。用于使用武器可能拥有的能力。
     *
     * @param event  事件
     * @param player 玩家
     * @param target 目标
     * @param nbt    数据
     */
    private static void useWeaponAbilities(HurtContext event, Player player, LivingEntity target, CompoundTag nbt) {
        if (target != null) {
            // active
            if (Ability.FIRE.hasAbility(nbt) && (int) (Math.random() * EAConfig.DATA.firechance) == 0) {
                double multiplier = (Ability.FIRE.getLevel(nbt) + Ability.FIRE.getLevel(nbt) * 4) / 4.0;
                target.igniteForSeconds((int) (multiplier));
            }

            if (Ability.FROST.hasAbility(nbt) && (int) (Math.random() * EAConfig.DATA.frostchance) == 0) {
                double multiplier = (Ability.FROST.getLevel(nbt) + Ability.FROST.getLevel(nbt) * 4) / 3.0;
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, (int) (20 * multiplier), 10));
            }

            if (Ability.POISON.hasAbility(nbt) && (int) (Math.random() * EAConfig.DATA.poisonchance) == 0) {
                double multiplier = (Ability.POISON.getLevel(nbt) + Ability.POISON.getLevel(nbt) * 4) / 2.0;
                target.addEffect(new MobEffectInstance(MobEffects.POISON, (int) (20 * multiplier), Ability.POISON.getLevel(nbt)));
            }

            if (Ability.INNATE.hasAbility(nbt) && (int) (Math.random() * EAConfig.DATA.innatechance) == 0) {
                double multiplier = (Ability.INNATE.getLevel(nbt) + Ability.INNATE.getLevel(nbt) * 4) / 3.0;
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, (int) (20 * multiplier), Ability.INNATE.getLevel(nbt)));
            }

            if (Ability.BOMBASTIC.hasAbility(nbt) && (int) (Math.random() * EAConfig.DATA.bombasticchance) == 0) {
                double multiplierD = (Ability.BOMBASTIC.getLevel(nbt) + Ability.BOMBASTIC.getLevel(nbt) * 4) / 4.0;
                float multiplier = (float) multiplierD;
                Level world = target.level();

                if (!(target instanceof Animal)) {
                    world.explode(target, target.getX(), target.getY(), target.getZ(), multiplier, ExplosionInteraction.NONE);
                }
            }

            if (Ability.CRITICAL_POINT.hasAbility(nbt) && (int) (Math.random() * EAConfig.DATA.criticalpointchance) == 0) {
                float multiplier = 0F;

                if (Ability.CRITICAL_POINT.getLevel(nbt) == 1) multiplier = 0.17F;
                else if (Ability.CRITICAL_POINT.getLevel(nbt) == 2) multiplier = 0.34F;
                else if (Ability.CRITICAL_POINT.getLevel(nbt) == 3) multiplier = 0.51F;

                float damage = target.getMaxHealth() * multiplier;
                event.setAmount(event.getAmount() + damage);
            }

            // passive
            if (Ability.ILLUMINATION.hasAbility(nbt)) {
                target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, (20 * 5), Ability.ILLUMINATION.getLevel(nbt)));
            }

            if (Ability.BLOODTHIRST.hasAbility(nbt)) {
                float addition = event.getAmount() * (Ability.BLOODTHIRST.getLevel(nbt) * 12) / 100;
                player.setHealth(player.getHealth() + addition);
            }
        }
    }

    private static void useArmorAbilities(HurtContext event, Player player, Entity target, CompoundTag nbt) {
        if (target != null) {
            // active
            if (Ability.MOLTEN.hasAbility(nbt) && (int) (Math.random() * EAConfig.DATA.moltenchance) == 0 && target instanceof LivingEntity realTarget) {
                double multiplier = (Ability.MOLTEN.getLevel(nbt) + Ability.MOLTEN.getLevel(nbt) * 5) / 4.0;
                realTarget.igniteForSeconds((int) (multiplier));
            }

            if (Ability.FROZEN.hasAbility(nbt) && (int) (Math.random() * EAConfig.DATA.frozenchance) == 0 && target instanceof LivingEntity realTarget) {
                double multiplier = (Ability.FROZEN.getLevel(nbt) + Ability.FROZEN.getLevel(nbt) * 5) / 6.0;
                realTarget.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, (int) (20 * multiplier), 10));
            }

            if (Ability.TOXIC.hasAbility(nbt) && (int) (Math.random() * EAConfig.DATA.toxicchance) == 0 && target instanceof LivingEntity realTarget) {
                double multiplier = (Ability.TOXIC.getLevel(nbt) + Ability.TOXIC.getLevel(nbt) * 4) / 4.0;
                realTarget.addEffect(new MobEffectInstance(MobEffects.POISON, (int) (20 * multiplier), Ability.TOXIC.getLevel(nbt)));
            }

            if (Ability.ADRENALINE.hasAbility(nbt) && (int) (Math.random() * EAConfig.DATA.adrenalinechance) == 0) {
                double multiplier = (Ability.ADRENALINE.getLevel(nbt) + Ability.ADRENALINE.getLevel(nbt) * 5) / 3.0;
                player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, (int) (20 * (multiplier)), Ability.ADRENALINE.getLevel(nbt)));
            }

            // passive
            if (Ability.BEASTIAL.hasAbility(nbt)) {
                if (player.getHealth() <= (player.getMaxHealth() * 0.2F))
                    player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 20 * 7, 0));
            }

            if (Ability.HARDENED.hasAbility(nbt) && (int) (Math.random() * EAConfig.DATA.hardenedchance) == 0) {
                event.setAmount(0F);
            }
        }
    }

    /**
     * 每次目标受伤时调用。用于检查武器是否应该升级。
     *
     * @param player
     * @param stack
     * @param nbt
     */
    private static final class HurtContext {
        private final LivingEntity entity;
        private final DamageSource source;
        private float amount;
        HurtContext(LivingEntity entity, DamageSource source, float amount) { this.entity = entity; this.source = source; this.amount = amount; }
        LivingEntity getEntity() { return entity; }
        DamageSource getSource() { return source; }
        float getAmount() { return amount; }
        void setAmount(float value) { amount = value; }
    }

    private static void updateLevel(Player player, ItemStack stack, CompoundTag nbt) {
        int level = Experience.getNextLevel(player, stack, nbt, Experience.getLevel(nbt), Experience.getExperience(nbt));
        Experience.setLevel(nbt, level);
        NBTUtil.saveStackNBT(stack, nbt);
    }
}
