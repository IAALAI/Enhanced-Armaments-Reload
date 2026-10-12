package nova.committee.enhancedarmaments.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.equipment.Equippable;
import nova.committee.enhancedarmaments.common.config.EAConfig;

import java.util.List;
import java.util.Set;

public final class EAUtil {
    private static final Set<String> LEGACY_VANILLA_EXCLUSIONS = Set.of(
            "bow", "chainmail_boots", "chainmail_chestplate", "chainmail_helmet", "chainmail_leggings", "crossbow", "diamond_axe", "diamond_boots", "diamond_chestplate", "diamond_helmet", "diamond_hoe", "diamond_leggings", "diamond_sword", "golden_axe", "golden_boots", "golden_chestplate", "golden_helmet", "golden_hoe", "golden_leggings", "golden_sword", "iron_axe", "iron_boots", "iron_chestplate", "iron_helmet", "iron_hoe", "iron_leggings", "iron_sword", "netherite_axe", "netherite_boots", "netherite_chestplate", "netherite_helmet", "netherite_hoe", "netherite_leggings", "netherite_sword", "stone_axe", "stone_hoe", "stone_sword", "trident", "wooden_axe", "wooden_hoe", "wooden_sword");

    private EAUtil() {
    }

    public static boolean canEnhance(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        Item item = stack.getItem();
        Identifier id = BuiltInRegistries.ITEM.getKey(item);

        if (EAConfig.DATA.onlyModdedItems && "minecraft".equals(id.getNamespace())
                && LEGACY_VANILLA_EXCLUSIONS.contains(id.getPath())) return false;
        if (EAConfig.DATA.extraItems.contains(id.toString())) return true;
        return isEnhanceableKind(stack);
    }

    private static boolean isEnhanceableKind(ItemStack stack) {
        Item item = stack.getItem();
        return item instanceof BowItem || item instanceof CrossbowItem || item instanceof TridentItem
                || isArmorKind(stack) || tagged(stack, ItemTags.SWORDS)
                || tagged(stack, ItemTags.AXES) || tagged(stack, ItemTags.HOES);
    }

    public static boolean canEnhanceWeapon(ItemStack stack) {
        return canEnhance(stack) && !isArmorKind(stack);
    }

    public static boolean canEnhanceMelee(ItemStack stack) {
        return canEnhanceWeapon(stack) && !(stack.getItem() instanceof BowItem)
                && !(stack.getItem() instanceof CrossbowItem);
    }

    public static boolean canEnhanceRanged(ItemStack stack) {
        return canEnhance(stack) && (stack.getItem() instanceof BowItem
                || stack.getItem() instanceof CrossbowItem || stack.getItem() instanceof TridentItem);
    }

    public static boolean isArrow(ItemStack stack) {
        return stack != null && !stack.isEmpty() && tagged(stack, ItemTags.ARROWS);
    }

    public static boolean canEnhanceArmor(ItemStack stack) {
        return canEnhance(stack) && isArmorKind(stack);
    }

    private static boolean isArmorKind(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (!stack.has(DataComponents.ATTRIBUTE_MODIFIERS)) return false;
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        if (equippable != null) {
            EquipmentSlot slot = equippable.slot();
            if (slot == EquipmentSlot.HEAD || slot == EquipmentSlot.CHEST || slot == EquipmentSlot.LEGS || slot == EquipmentSlot.FEET) {
                return true;
            }
        }
        return tagged(stack, ItemTags.HEAD_ARMOR)
                || tagged(stack, ItemTags.CHEST_ARMOR)
                || tagged(stack, ItemTags.LEG_ARMOR)
                || tagged(stack, ItemTags.FOOT_ARMOR)
                || tagged(stack, ItemTags.ARMOR_ENCHANTABLE);
    }

    public static boolean tagged(ItemStack stack, net.minecraft.tags.TagKey<Item> tag) {
        return stack.is(holder -> holder.is(tag));
    }

    public static boolean isDamageSourceAllowed(DamageSource damageSource) {
        return !(damageSource.is(DamageTypes.FALL)
                || damageSource.is(DamageTypes.DROWN)
                || damageSource.is(DamageTypes.CACTUS)
                || damageSource.is(DamageTypes.STARVE)
                || damageSource.is(DamageTypes.IN_WALL)
                || damageSource.is(DamageTypes.IN_FIRE)
                || damageSource.is(DamageTypes.FELL_OUT_OF_WORLD))
                || damageSource.getEntity() instanceof LivingEntity;
    }

    public static boolean containsString(List<Component> tooltip, String string) {
        if (tooltip == null || tooltip.isEmpty()) {
            return false;
        }
        for (Component component : tooltip) {
            if (component.getString().equals(string)) {
                return true;
            }
        }
        return false;
    }

    public static int lineContainsString(List<Component> tooltip, String string) {
        if (tooltip == null || tooltip.isEmpty()) {
            return -1;
        }
        for (int i = 0; i < tooltip.size(); i++) {
            if (tooltip.get(i).getString().equals(string)) {
                return i;
            }
        }
        return -1;
    }
}
