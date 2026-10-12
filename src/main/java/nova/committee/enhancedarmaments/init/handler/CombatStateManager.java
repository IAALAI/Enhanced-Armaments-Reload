package nova.committee.enhancedarmaments.init.handler;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import nova.committee.enhancedarmaments.core.Ability;
import nova.committee.enhancedarmaments.util.NBTUtil;
import nova.committee.enhancedarmaments.util.PRD;

import java.util.EnumMap;
import java.util.Map;
import java.util.WeakHashMap;

/** Transient combat state. Nothing in this class is serialized to an item. */
public final class CombatStateManager {
    private static final int ACTIVE_ABILITY_ICD_TICKS = 20;
    private static final int LONG_ABILITY_ICD_TICKS = 100 * 20;
    private static final int EQUIPMENT_HEAL_INTERVAL_TICKS = 18 * 20;
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };
    private static final Identifier EQUIPMENT_ENHANCEMENT_MAX_HEALTH = Identifier.fromNamespaceAndPath(
            "enhancedarmaments", "equipment_enhancement_max_health");
    private static final Map<Player, PlayerState> STATES = new WeakHashMap<>();

    private CombatStateManager() {
    }

    public static boolean isCooldownReady(Player player, long now, Ability ability) {
        return now >= state(player).cooldowns.getOrDefault(ability, Long.MIN_VALUE);
    }

    public static boolean startCooldownIfReady(Player player, long now, Ability ability, int cooldownTicks) {
        PlayerState state = state(player);
        if (now < state.cooldowns.getOrDefault(ability, Long.MIN_VALUE)) {
            return false;
        }
        state.cooldowns.put(ability, now + Math.max(0, cooldownTicks));
        return true;
    }

    public static void primeAbilityCooldown(Player player, Ability ability, CompoundTag nbt) {
        int cooldownTicks = initialCooldownTicks(ability, nbt);
        if (cooldownTicks <= 0) {
            return;
        }
        long readyAt = player.level().getGameTime() + cooldownTicks;
        state(player).cooldowns.merge(ability, readyAt, Math::max);
    }

    public static boolean checkArmorReduction(Player player, EquipmentSlot slot, int targetPercent) {
        PlayerState state = state(player);
        PrdState current = state.armorPrd.get(slot);
        if (current == null || current.targetPercent != targetPercent) {
            current = new PrdState(targetPercent, new PRD(targetPercent));
            state.armorPrd.put(slot, current);
        }
        return current.prd.check();
    }

    public static boolean isEternalGuardianActive(Player player, long now) {
        for (EternalGuardianState guard : state(player).eternalGuardian.values()) {
            if (now < guard.activeUntil) {
                return true;
            }
        }
        return false;
    }

    public static boolean isEternalGuardianLocked(Player player, EquipmentSlot slot, ItemStack stack) {
        PlayerState state = state(player);
        EternalGuardianState guard = state.eternalGuardian.get(slot);
        if (guard == null) {
            return false;
        }
        int remaining = stack.getMaxDamage() - stack.getDamageValue();
        if (stack.getDamageValue() < guard.triggerDamage && remaining * 2 > stack.getMaxDamage()) {
            state.eternalGuardian.remove(slot);
            return false;
        }
        return true;
    }

    public static void activateEternalGuardian(Player player, EquipmentSlot slot, long activeUntil, int triggerDamage) {
        state(player).eternalGuardian.put(slot, new EternalGuardianState(activeUntil, triggerDamage));
    }

    public static void onEquipmentChanged(Player player, EquipmentSlot slot, ItemStack previous, ItemStack current) {
        if (!player.isAlive() || sameEnhancementItem(previous, current)) {
            return;
        }
        PlayerState state = state(player);
        long now = player.level().getGameTime();
        primeEquipmentCooldowns(state, current, now);
        if (isArmorSlot(slot)) {
            state.armorPrd.remove(slot);
            state.eternalGuardian.remove(slot);
            refreshEquipmentPassives(player, state, now);
        }
    }

    /** Ticks cached equipment healing without inspecting any ItemStack. */
    public static void tickPlayer(Player player) {
        if (!player.isAlive()) {
            return;
        }
        PlayerState state = state(player);
        if (state.equipmentHealingAmount <= 0F) {
            return;
        }
        long now = player.level().getGameTime();
        if (now < state.nextEquipmentHealAt) {
            return;
        }
        state.nextEquipmentHealAt = now + EQUIPMENT_HEAL_INTERVAL_TICKS;
        if (player.getHealth() < player.getMaxHealth()) {
            player.heal(state.equipmentHealingAmount);
            player.getFoodData().setFoodLevel(Math.max(0,
                    player.getFoodData().getFoodLevel() - state.equipmentHealingFoodCost));
        }
    }

    public static void clear(Player player) {
        STATES.remove(player);
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.removeModifier(EQUIPMENT_ENHANCEMENT_MAX_HEALTH);
        }
    }

    private static PlayerState state(Player player) {
        PlayerState current = STATES.get(player);
        if (current != null) {
            return current;
        }
        PlayerState created = new PlayerState();
        STATES.put(player, created);
        initializeEquipmentState(player, created);
        return created;
    }

    private static void initializeEquipmentState(Player player, PlayerState state) {
        long now = player.level().getGameTime();
        primeEquipmentCooldowns(state, player.getMainHandItem(), now);
        primeEquipmentCooldowns(state, player.getOffhandItem(), now);
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            primeEquipmentCooldowns(state, player.getItemBySlot(slot), now);
        }
        refreshEquipmentPassives(player, state, now);
    }

    private static void primeEquipmentCooldowns(PlayerState state, ItemStack stack, long now) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        CompoundTag nbt = NBTUtil.loadStackNBT(stack);
        for (Ability ability : Ability.values()) {
            if (!ability.hasAbility(nbt)) {
                continue;
            }
            int cooldownTicks = initialCooldownTicks(ability, nbt);
            if (cooldownTicks > 0) {
                state.cooldowns.merge(ability, now + cooldownTicks, Math::max);
            }
        }
    }

    private static int initialCooldownTicks(Ability ability, CompoundTag nbt) {
        return switch (ability) {
            case FIRE, FROST, POISON, INNATE, BOMBASTIC, CRITICAL_POINT,
                 MOLTEN, FROZEN, TOXIC -> ACTIVE_ABILITY_ICD_TICKS;
            case ADRENALINE, BEASTIAL -> LONG_ABILITY_ICD_TICKS;
            case HARDENED -> Math.max(20,
                    (100 - (Math.max(1, Ability.HARDENED.getLevel(nbt)) - 1) * 20) * 20);
            default -> 0;
        };
    }

    private static void refreshEquipmentPassives(Player player, PlayerState state, long now) {
        float healingAmount = 0F;
        int healingFoodCost = 0;
        double maxHealthAmount = 0D;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty()) {
                continue;
            }
            CompoundTag nbt = NBTUtil.loadStackNBT(stack);
            if (NBTUtil.clearLegacyCombatState(nbt)) {
                NBTUtil.saveStackNBT(stack, nbt);
            }
            if (Ability.REMEDIAL.hasAbility(nbt)) {
                healingAmount += Ability.REMEDIAL.getLevel(nbt) + 1F;
                healingFoodCost++;
            }
            if (Ability.GALLOP.hasAbility(nbt)) {
                maxHealthAmount += Ability.GALLOP.getLevel(nbt) + 1D;
            }
        }
        state.equipmentHealingAmount = healingAmount;
        state.equipmentHealingFoodCost = healingFoodCost;
        state.nextEquipmentHealAt = now + EQUIPMENT_HEAL_INTERVAL_TICKS;
        updateEquipmentMaxHealth(player, maxHealthAmount);
    }

    private static void updateEquipmentMaxHealth(Player player, double amount) {
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth == null) {
            return;
        }
        AttributeModifier current = maxHealth.getModifier(EQUIPMENT_ENHANCEMENT_MAX_HEALTH);
        if (amount > 0D) {
            if (current == null || Double.compare(current.amount(), amount) != 0
                    || current.operation() != AttributeModifier.Operation.ADD_VALUE) {
                maxHealth.addOrUpdateTransientModifier(new AttributeModifier(
                        EQUIPMENT_ENHANCEMENT_MAX_HEALTH, amount, AttributeModifier.Operation.ADD_VALUE));
            }
        } else if (current != null) {
            maxHealth.removeModifier(EQUIPMENT_ENHANCEMENT_MAX_HEALTH);
        }
        if (player.getHealth() > player.getMaxHealth()) {
            player.setHealth(player.getMaxHealth());
        }
    }

    private static boolean sameEnhancementItem(ItemStack previous, ItemStack current) {
        if (previous == null || current == null || previous.isEmpty() || current.isEmpty()) {
            return previous != null && current != null && previous.isEmpty() && current.isEmpty();
        }
        CompoundTag previousNbt = NBTUtil.loadStackNBT(previous);
        CompoundTag currentNbt = NBTUtil.loadStackNBT(current);
        String previousId = NBTUtil.getItemId(previousNbt);
        String currentId = NBTUtil.getItemId(currentNbt);
        if (!previousId.isEmpty() && !currentId.isEmpty()) {
            return previousId.equals(currentId);
        }
        // Assigning the first EA item id changes components but is not an equipment swap.
        return previous.getItem() == current.getItem() && (previousId.isEmpty() || currentId.isEmpty());
    }

    private static boolean isArmorSlot(EquipmentSlot slot) {
        return slot == EquipmentSlot.HEAD || slot == EquipmentSlot.CHEST
                || slot == EquipmentSlot.LEGS || slot == EquipmentSlot.FEET;
    }

    private static final class PlayerState {
        private final EnumMap<Ability, Long> cooldowns = new EnumMap<>(Ability.class);
        private final EnumMap<EquipmentSlot, PrdState> armorPrd = new EnumMap<>(EquipmentSlot.class);
        private final EnumMap<EquipmentSlot, EternalGuardianState> eternalGuardian = new EnumMap<>(EquipmentSlot.class);
        private float equipmentHealingAmount;
        private int equipmentHealingFoodCost;
        private long nextEquipmentHealAt;
    }

    private record PrdState(int targetPercent, PRD prd) {
    }

    private record EternalGuardianState(long activeUntil, int triggerDamage) {
    }
}
