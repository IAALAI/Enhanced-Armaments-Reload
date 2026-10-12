package nova.committee.enhancedarmaments.init.handler;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import nova.committee.enhancedarmaments.common.config.EAConfig;
import nova.committee.enhancedarmaments.core.Ability;
import nova.committee.enhancedarmaments.core.Experience;
import nova.committee.enhancedarmaments.core.Rarity;
import nova.committee.enhancedarmaments.util.EAUtil;
import nova.committee.enhancedarmaments.util.NBTUtil;

import java.util.List;

public final class ItemTooltipHandler {
    private ItemTooltipHandler() {
    }

    public static void append(ItemStack stack, Item.TooltipContext context, TooltipFlag flag, List<Component> tooltip) {
        if (!EAUtil.canEnhance(stack)) {
            return;
        }
        CompoundTag nbt = NBTUtil.loadStackNBT(stack);
        if (!Experience.isEnabled(nbt)) {
            return;
        }
        Rarity rarity = Rarity.getRarity(nbt);
        int level = Experience.getLevel(nbt);
        int experience = Experience.getExperience(nbt);
        int maxExperience = Experience.getMaxLevelExp(level);

        tooltip.set(0, Component.literal(stack.getDisplayName().getString() + rarity.getColor() + " ("
                + ChatFormatting.ITALIC + I18n.get("enhancedarmaments.rarity." + rarity.getName()) + ")"));

        appendRangedBonus(stack, rarity, tooltip);
        appendMeleeBonus(stack, rarity, tooltip);
        appendArmorBonus(stack, rarity, tooltip);

        if (level >= EAConfig.DATA.maxLevel) {
            tooltip.add(Component.literal(I18n.get("enhancedarmaments.misc.level") + ": " + ChatFormatting.RED + I18n.get("enhancedarmaments.misc.max")));
            tooltip.add(Component.literal(I18n.get("enhancedarmaments.misc.experience") + ": " + I18n.get("enhancedarmaments.misc.max")));
        } else {
            tooltip.add(Component.literal(I18n.get("enhancedarmaments.misc.level") + ": " + ChatFormatting.WHITE + level));
            tooltip.add(Component.literal(I18n.get("enhancedarmaments.misc.experience") + ": " + experience + " / " + maxExperience));
        }
        if (EAConfig.DATA.showDurabilityInTooltip && stack.isDamageableItem()) {
            tooltip.add(Component.literal(I18n.get("enhancedarmaments.misc.durability") + ": "
                    + (stack.getMaxDamage() - stack.getDamageValue()) + " / " + stack.getMaxDamage()));
        }
        tooltip.add(Component.literal(""));
        if (InputConstants.isKeyDown(InputConstants.KEY_LSHIFT) || InputConstants.isKeyDown(InputConstants.KEY_RSHIFT)) {
            tooltip.add(Component.literal(rarity.getColor() + "" + ChatFormatting.ITALIC + I18n.get("enhancedarmaments.misc.abilities")));
            tooltip.add(Component.literal(""));
            var list = EAUtil.canEnhanceArmor(stack) ? Ability.ARMOR_ABILITIES : Ability.WEAPON_ABILITIES;
            for (Ability ability : list) {
                if (ability.hasAbility(nbt)) {
                    tooltip.add(Component.literal("-" + ability.getColor() + ability.getName(nbt)));
                }
            }
        } else {
            tooltip.add(Component.literal(rarity.getColor() + "" + ChatFormatting.ITALIC + I18n.get("enhancedarmaments.misc.abilities.shift")));
        }
    }

    private static void appendRangedBonus(ItemStack stack, Rarity rarity, List<Component> tooltip) {
        if (EAUtil.canEnhanceRanged(stack) && rarity.getEffect() != 0) {
            String bonus = String.format("%.1f", rarity.getEffect() / 3 * 100);
            tooltip.add(1, Component.literal(I18n.get("enhancedarmaments.misc.rarity.arrowpercentage") + " " + rarity.getColor() + "+" + bonus + "%"));
        }
    }

    private static void appendMeleeBonus(ItemStack stack, Rarity rarity, List<Component> tooltip) {
        if (!EAUtil.canEnhanceMelee(stack) || rarity.getEffect() == 0) {
            return;
        }
        final double[] damage = {0};
        stack.forEachModifier(EquipmentSlot.MAINHAND, (attribute, modifier) -> {
            if (attribute.is(Attributes.ATTACK_DAMAGE)) {
                damage[0] = modifier.amount();
            }
        });
        if (damage[0] == 0) {
            return;
        }
        double display = ((damage[0] + 1) * rarity.getEffect()) + damage[0] + 1;
        tooltip.add(Component.literal(rarity.getColor() + " " + String.format("%.1f", display)
                + ChatFormatting.GRAY + " " + I18n.get("enhancedarmaments.misc.tooltip.attackdamage")));
    }

    private static void appendArmorBonus(ItemStack stack, Rarity rarity, List<Component> tooltip) {
        if (!EAUtil.canEnhanceArmor(stack) || rarity.getEffect() == 0) {
            return;
        }
        String p = String.format("%.1f", 100 - (100 / (1.0F + (rarity.getEffect() / 5F))));
        tooltip.add(Component.literal(" " + ChatFormatting.BLUE + "+" + rarity.getColor() + p + ChatFormatting.BLUE + "% "
                + I18n.get("enhancedarmaments.misc.rarity.armorreduction")));
    }
}
