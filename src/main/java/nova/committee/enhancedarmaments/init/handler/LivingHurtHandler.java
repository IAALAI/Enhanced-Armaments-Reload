package nova.committee.enhancedarmaments.init.handler;

import net.fabricmc.fabric.api.entity.FakePlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import nova.committee.enhancedarmaments.common.config.EAConfig;
import nova.committee.enhancedarmaments.core.Ability;
import nova.committee.enhancedarmaments.core.Experience;
import nova.committee.enhancedarmaments.core.EquipmentDurability;
import nova.committee.enhancedarmaments.core.Rarity;
import nova.committee.enhancedarmaments.util.EAUtil;
import nova.committee.enhancedarmaments.util.NBTUtil;

import java.util.ArrayList;
import java.util.List;

public final class LivingHurtHandler {
    private static final int ACTIVE_ABILITY_ICD_TICKS = 20;
    private static final int LONG_ABILITY_ICD_TICKS = 100 * 20;
    private static final int ETERNAL_GUARD_DURATION_TICKS = 50;
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    public static InteractionHand bowfriendlyhand;

    private LivingHurtHandler() {
    }

    /** 攻击方武器加伤：仍在原版护甲之前，和 Forge LivingHurt 一致。 */
    public static float beforeVanillaArmor(LivingEntity target, DamageSource source, float amount) {
        AttackContext attack = resolveAttackContext(source);
        if (attack != null && !attack.effectStack().isEmpty() && EAUtil.canEnhanceWeapon(attack.effectStack())) {
            ItemStack progressionStack = attack.progressionStack().isEmpty() ? attack.effectStack() : attack.progressionStack();
            CompoundTag effectNbt = EnhancementInitializer.initialize(attack.player(), progressionStack);
            if (Experience.isEnabled(effectNbt)) {
                amount = applyWeaponRarity(attack.effectStack(), effectNbt, amount);
                if (!progressionStack.isEmpty()) {
                    amount = applyWeaponAbilities(attack.player(), target, effectNbt, amount);
                    updateExperience(effectNbt, amount);
                    updateLevel(attack.player(), progressionStack, effectNbt);
                    NBTUtil.saveStackNBT(progressionStack, effectNbt);
                }
            }
        }
        if (attack != null && isEternalGuardianActive(attack.player())) {
            amount *= 0.5F;
        }
        return Math.max(amount, 0F);
    }

    /** 防守方护甲减伤：原版护甲之后，每件独立伪随机判定，只结算成功的最高一档。 */
    public static float afterVanillaArmor(LivingEntity target, DamageSource source, float amount) {
        if (target instanceof Player player) {
            amount = applyPlayerArmor(player, source, amount);
        }
        return Math.max(amount, 0F);
    }

    /**
     * 每件护甲独立进行 PRD 判定并立刻更新自己的计数器；发动成功的只取最高一档。
     * 被更高效果或硬化覆盖的成功判定仍会重置，形成同类装备的自然边际递减。
     * 不破金身使用玩家级冷却，每次只让优先级最高的一件护甲掷骰。
     */
    private static float applyPlayerArmor(Player player, DamageSource source, float amount) {
        if (isEternalGuardianActive(player)) {
            return 0F;
        }
        Entity attacker = source.getEntity();
        List<HardenedRoll> hardenedRolls = new ArrayList<>(ARMOR_SLOTS.length);
        ArmorReductionRoll bestReduction = null;

        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty() || !EAUtil.canEnhanceArmor(stack)) {
                continue;
            }
            CompoundTag nbt = EnhancementInitializer.initialize(player, stack);
            if (!Experience.isEnabled(nbt)) {
                continue;
            }
            if (EAUtil.isDamageSourceAllowed(source)) {
                if (amount < (player.getMaxHealth() + player.getArmorValue())) {
                    updateExperience(nbt, amount);
                } else {
                    updateExperience(nbt, 1);
                }
                updateLevel(player, stack, nbt);
            }

            Rarity rarity = Rarity.getRarity(nbt);
            if (rarity != Rarity.DEFAULT && rarity.getEffect() > 0D) {
                int targetPercent = armorReductionTargetPercent();
                boolean triggered = targetPercent > 0
                        && CombatStateManager.checkArmorReduction(player, slot, targetPercent);
                ArmorReductionRoll reductionRoll = new ArmorReductionRoll(stack, slot, rarity.getEffect(), triggered);
                if (triggered && (bestReduction == null || reductionRoll.effect() > bestReduction.effect())) {
                    bestReduction = reductionRoll;
                }
            }
            if (Ability.HARDENED.hasAbility(nbt)) {
                hardenedRolls.add(new HardenedRoll(Ability.HARDENED.getLevel(nbt), rarity.getEffect()));
            }

            // 反伤/回血等仍按件独立，只有减伤不叠乘
            applyArmorOnHitAbilities(player, attacker, nbt);
            NBTUtil.saveStackNBT(stack, nbt);
        }

        hardenedRolls.sort((left, right) -> {
            int byLevel = Integer.compare(right.level(), left.level());
            return byLevel != 0 ? byLevel : Double.compare(right.rarityEffect(), left.rarityEffect());
        });
        long now = player.level().getGameTime();
        for (HardenedRoll hardened : hardenedRolls) {
            if (!isCooldownReady(player, now, Ability.HARDENED)) {
                continue;
            }
            if (triggerWithCooldown(player, now, Ability.HARDENED, EAConfig.DATA.hardenedchance,
                    hardenedCooldownTicks(hardened.level()))) {
                return 0F;
            }
            break;
        }
        float finalAmount = amount;
        if (bestReduction != null) {
            bestReduction.stack().hurtAndBreak(1, player, bestReduction.slot());
            finalAmount = (float) (amount / (1.0F + (bestReduction.effect() / 5F)));
        }
        if (tryTriggerEternalGuardian(player, finalAmount)) {
            return 0F;
        }
        applyBeastial(player, finalAmount);
        return finalAmount;
    }

    private static int armorReductionTargetPercent() {
        double target = Math.max(0D, Math.min(1D, EAConfig.DATA.armorReductionProcChance));
        return (int) Math.round(target * 100D);
    }

    private record ArmorReductionRoll(ItemStack stack, EquipmentSlot slot, double effect, boolean triggered) {
    }

    private record HardenedRoll(int level, double rarityEffect) {
    }

    private record AttackContext(Player player, ItemStack effectStack, ItemStack progressionStack) {
    }

    private static AttackContext resolveAttackContext(DamageSource source) {
        Entity direct = source.getDirectEntity();
        if (direct instanceof Player player && !(player instanceof FakePlayer)) {
            ItemStack stack = resolveAttackerStack(player);
            return new AttackContext(player, stack, stack);
        }
        if (direct instanceof AbstractArrow arrow && source.getEntity() instanceof Player player && !(player instanceof FakePlayer)) {
            ItemStack firedWeapon = arrow.getWeaponItem();
            return new AttackContext(player, firedWeapon, findOwnedFiredWeapon(player, firedWeapon));
        }
        return null;
    }

    public static ItemStack findOwnedFiredWeapon(Player player, ItemStack firedWeapon) {
        if (firedWeapon == null || firedWeapon.isEmpty()) {
            return ItemStack.EMPTY;
        }
        String firedId = NBTUtil.getItemId(NBTUtil.loadStackNBT(firedWeapon));
        ItemStack componentMatch = ItemStack.EMPTY;
        ItemStack fallback = ItemStack.EMPTY;
        for (ItemStack candidate : player.getInventory().getNonEquipmentItems()) {
            if (candidate.isEmpty() || candidate.getItem() != firedWeapon.getItem()) {
                continue;
            }
            if (fallback.isEmpty()) {
                fallback = candidate;
            }
            if (!firedId.isEmpty() && firedId.equals(NBTUtil.getItemId(NBTUtil.loadStackNBT(candidate)))) {
                return candidate;
            }
            if (componentMatch.isEmpty() && ItemStack.isSameItemSameComponents(candidate, firedWeapon)) {
                componentMatch = candidate;
            }
        }
        return firedId.isEmpty() ? (componentMatch.isEmpty() ? fallback : componentMatch) : ItemStack.EMPTY;
    }

    private static ItemStack resolveAttackerStack(Player player) {
        if (bowfriendlyhand == null) {
            return player.getMainHandItem();
        }
        return player.getItemInHand(bowfriendlyhand);
    }

    private static void updateExperience(CompoundTag nbt, float dealtDamage) {
        if (Experience.getLevel(nbt) < EAConfig.DATA.maxLevel) {
            Experience.setExperience(nbt, Experience.getExperience(nbt) + 1 + (int) dealtDamage / 4);
        }
    }

    private static float applyWeaponRarity(ItemStack stack, CompoundTag nbt, float amount) {
        Rarity rarity = Rarity.getRarity(nbt);
        if (rarity == Rarity.DEFAULT) {
            return amount;
        }
        if (EAUtil.canEnhanceMelee(stack)) {
            final float[] bonus = {0F};
            stack.forEachModifier(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
                if (attribute.is(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)) {
                    bonus[0] = (float) (modifier.amount() * rarity.getEffect());
                }
            });
            return amount + bonus[0];
        }
        if (EAUtil.canEnhanceRanged(stack)) {
            return (float) (amount + (amount * rarity.getEffect() / 3F));
        }
        return amount;
    }

    private static float applyWeaponAbilities(Player player, LivingEntity target, CompoundTag nbt, float amount) {
        if (target == null) {
            return amount;
        }
        long now = player.level().getGameTime();
        if (Ability.FIRE.hasAbility(nbt) && triggerWithCooldown(player, now, Ability.FIRE, EAConfig.DATA.firechance, ACTIVE_ABILITY_ICD_TICKS)) {
            double multiplier = (Ability.FIRE.getLevel(nbt) + Ability.FIRE.getLevel(nbt) * 4) / 4.0;
            target.igniteForSeconds((float) multiplier);
        }
        if (Ability.FROST.hasAbility(nbt) && triggerWithCooldown(player, now, Ability.FROST, EAConfig.DATA.frostchance, ACTIVE_ABILITY_ICD_TICKS)) {
            double multiplier = (Ability.FROST.getLevel(nbt) + Ability.FROST.getLevel(nbt) * 4) / 3.0;
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, (int) (20 * multiplier), 10));
        }
        if (Ability.POISON.hasAbility(nbt) && triggerWithCooldown(player, now, Ability.POISON, EAConfig.DATA.poisonchance, ACTIVE_ABILITY_ICD_TICKS)) {
            double multiplier = (Ability.POISON.getLevel(nbt) + Ability.POISON.getLevel(nbt) * 4) / 2.0;
            target.addEffect(new MobEffectInstance(MobEffects.POISON, (int) (20 * multiplier), Ability.POISON.getLevel(nbt)));
        }
        if (Ability.INNATE.hasAbility(nbt) && triggerWithCooldown(player, now, Ability.INNATE, EAConfig.DATA.innatechance, ACTIVE_ABILITY_ICD_TICKS)) {
            double multiplier = (Ability.INNATE.getLevel(nbt) + Ability.INNATE.getLevel(nbt) * 4) / 3.0;
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, (int) (20 * multiplier), Ability.INNATE.getLevel(nbt)));
        }
        if (Ability.BOMBASTIC.hasAbility(nbt) && !(target instanceof Animal)
                && triggerWithCooldown(player, now, Ability.BOMBASTIC, EAConfig.DATA.bombasticchance, ACTIVE_ABILITY_ICD_TICKS)) {
            float multiplier = (float) ((Ability.BOMBASTIC.getLevel(nbt) + Ability.BOMBASTIC.getLevel(nbt) * 4) / 4.0);
            Level world = target.level();
            if (world instanceof ServerLevel) {
                world.explode(target, target.getX(), target.getY(), target.getZ(), multiplier, Level.ExplosionInteraction.NONE);
            }
        }
        if (Ability.CRITICAL_POINT.hasAbility(nbt)
                && triggerWithCooldown(player, now, Ability.CRITICAL_POINT, EAConfig.DATA.criticalpointchance, ACTIVE_ABILITY_ICD_TICKS)) {
            float multiplier = switch (Ability.CRITICAL_POINT.getLevel(nbt)) {
                case 2 -> 0.34F;
                case 3 -> 0.51F;
                default -> 0.17F;
            };
            amount += target.getMaxHealth() * multiplier;
        }
        if (Ability.ILLUMINATION.hasAbility(nbt)) {
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20 * 5, Ability.ILLUMINATION.getLevel(nbt)));
        }
        return amount;
    }

    private static void applyArmorOnHitAbilities(Player player, Entity attacker, CompoundTag nbt) {
        long now = player.level().getGameTime();
        if (attacker instanceof LivingEntity living) {
            if (Ability.MOLTEN.hasAbility(nbt)
                    && triggerWithCooldown(player, now, Ability.MOLTEN, EAConfig.DATA.moltenchance, ACTIVE_ABILITY_ICD_TICKS)) {
                double multiplier = (Ability.MOLTEN.getLevel(nbt) + Ability.MOLTEN.getLevel(nbt) * 5) / 4.0;
                living.igniteForSeconds((float) multiplier);
            }
            if (Ability.FROZEN.hasAbility(nbt)
                    && triggerWithCooldown(player, now, Ability.FROZEN, EAConfig.DATA.frozenchance, ACTIVE_ABILITY_ICD_TICKS)) {
                double multiplier = (Ability.FROZEN.getLevel(nbt) + Ability.FROZEN.getLevel(nbt) * 5) / 6.0;
                living.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, (int) (20 * multiplier), 10));
            }
            if (Ability.TOXIC.hasAbility(nbt)
                    && triggerWithCooldown(player, now, Ability.TOXIC, EAConfig.DATA.toxicchance, ACTIVE_ABILITY_ICD_TICKS)) {
                double multiplier = (Ability.TOXIC.getLevel(nbt) + Ability.TOXIC.getLevel(nbt) * 4) / 4.0;
                living.addEffect(new MobEffectInstance(MobEffects.POISON, (int) (20 * multiplier), Ability.TOXIC.getLevel(nbt)));
            }
        }
        if (Ability.ADRENALINE.hasAbility(nbt)
                && triggerWithCooldown(player, now, Ability.ADRENALINE, EAConfig.DATA.adrenalinechance, LONG_ABILITY_ICD_TICKS)) {
            int level = Ability.ADRENALINE.getLevel(nbt);
            int durationTicks = (4 + (level - 1) * 2) * 20;
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, durationTicks, level));
        }
    }

    private static void applyBeastial(Player player, float incomingDamage) {
        if (incomingDamage <= 0F || player.getHealth() - incomingDamage > player.getMaxHealth() * 0.2F) {
            return;
        }
        long now = player.level().getGameTime();
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty() || !EAUtil.canEnhanceArmor(stack)) {
                continue;
            }
            CompoundTag nbt = NBTUtil.loadStackNBT(stack);
            if (!Experience.isEnabled(nbt) || !Ability.BEASTIAL.hasAbility(nbt)
                    || !startCooldownIfReady(player, now, Ability.BEASTIAL, LONG_ABILITY_ICD_TICKS)) {
                continue;
            }
            MobEffectInstance current = player.getEffect(MobEffects.STRENGTH);
            int currentLevel = current == null ? 0 : current.getAmplifier() + 1;
            int addedDurationTicks = (7 + (Ability.BEASTIAL.getLevel(nbt) - 1) * 4) * 20;
            int durationTicks = (current == null ? 0 : current.getDuration()) + addedDurationTicks;
            player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, durationTicks, currentLevel));
        }
    }

    private static boolean isEternalGuardianActive(Player player) {
        return CombatStateManager.isEternalGuardianActive(player, player.level().getGameTime());
    }

    private static boolean tryTriggerEternalGuardian(Player player, float incomingDamage) {
        if (incomingDamage <= 0F
                || (player.getHealth() > player.getMaxHealth() * 0.2F
                && player.getHealth() - incomingDamage > player.getMaxHealth() * 0.2F)) {
            return false;
        }
        ItemStack selected = ItemStack.EMPTY;
        EquipmentSlot selectedSlot = null;
        double selectedDurabilityRatio = -1D;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty() || !stack.isDamageableItem() || stack.getMaxDamage() <= 0) {
                continue;
            }
            CompoundTag nbt = NBTUtil.loadStackNBT(stack);
            int remaining = stack.getMaxDamage() - stack.getDamageValue();
            if (!Ability.ETERNAL_GUARDIAN.hasAbility(nbt)
                    || CombatStateManager.isEternalGuardianLocked(player, slot, stack)
                    || remaining * 2 <= stack.getMaxDamage()) {
                continue;
            }
            double ratio = (double) remaining / stack.getMaxDamage();
            if (ratio > selectedDurabilityRatio) {
                selected = stack;
                selectedSlot = slot;
                selectedDurabilityRatio = ratio;
            }
        }
        if (selected.isEmpty() || selectedSlot == null) {
            return false;
        }
        int durabilityCost = Math.max(1, (int) Math.ceil(selected.getMaxDamage() * 0.25D));
        selected.setDamageValue(Math.min(selected.getMaxDamage(), selected.getDamageValue() + durabilityCost));
        CombatStateManager.activateEternalGuardian(player, selectedSlot,
                player.level().getGameTime() + ETERNAL_GUARD_DURATION_TICKS, selected.getDamageValue());
        return true;
    }

    private static void updateLevel(Player player, ItemStack stack, CompoundTag nbt) {
        int level = Experience.getNextLevel(player, stack, nbt, Experience.getLevel(nbt), Experience.getExperience(nbt));
        Experience.setLevel(nbt, level);
        EquipmentDurability.synchronize(stack, nbt);
    }

    private static boolean roll(double chance) {
        return (int) (Math.random() * chance) == 0;
    }

    private static boolean triggerWithCooldown(Player player, long now, Ability ability, double chance, int cooldownTicks) {
        return isCooldownReady(player, now, ability) && roll(chance)
                && startCooldownIfReady(player, now, ability, cooldownTicks);
    }

    private static boolean startCooldownIfReady(Player player, long now, Ability ability, int cooldownTicks) {
        return CombatStateManager.startCooldownIfReady(player, now, ability, cooldownTicks);
    }

    private static boolean isCooldownReady(Player player, long now, Ability ability) {
        return CombatStateManager.isCooldownReady(player, now, ability);
    }

    private static int hardenedCooldownTicks(int level) {
        return Math.max(20, (100 - (Math.max(1, level) - 1) * 20) * 20);
    }
}
