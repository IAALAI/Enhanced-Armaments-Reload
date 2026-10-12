package nova.committee.enhancedarmaments.client.gui;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.Equippable;
import nova.committee.enhancedarmaments.common.config.EAConfig;
import nova.committee.enhancedarmaments.common.network.GuiAbilityPayload;
import nova.committee.enhancedarmaments.common.network.NotificationSettingsPayload;
import nova.committee.enhancedarmaments.core.Ability;
import nova.committee.enhancedarmaments.core.Experience;
import nova.committee.enhancedarmaments.core.PlayerNotificationSettings;
import nova.committee.enhancedarmaments.core.Rarity;
import nova.committee.enhancedarmaments.util.EAUtil;
import nova.committee.enhancedarmaments.util.NBTUtil;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Complete 26.3 port of the Forge ability selection screen. */
public class AbilitySelectionScreen extends Screen {
    private static final int WHITE = 0xFFFFFFFF;
    private static final int GOLD = 0xFFFFD700;
    private static final int GRAY = 0xFFAAAAAA;
    private static final int DARK_GRAY = 0xFF777777;
    private static final int AQUA = 0xFF55FFFF;
    private static final int ABILITY_CAPACITY_Y = 86;
    private static final int ABILITY_HEADER_Y = 108;
    private static final int ABILITY_BUTTON_START_Y = 120;

    private final List<AbilityButton> abilityButtons = new ArrayList<>();
    private int openedSlot = -1;
    private boolean showExperienceGain;
    private boolean showEquipmentLevelUp;
    private Button experienceNotificationButton;
    private Button levelUpNotificationButton;

    public AbilitySelectionScreen() {
        super(Component.translatable("key.gui.weapon_interface"));
    }

    @Override
    protected void init() {
        super.init();
        abilityButtons.clear();
        if (minecraft == null || minecraft.player == null) return;
        openedSlot = minecraft.player.getInventory().getSelectedSlot();
        PlayerNotificationSettings settings = PlayerNotificationSettings.get(minecraft.player);
        showExperienceGain = settings.showExperienceGain();
        showEquipmentLevelUp = settings.showEquipmentLevelUp();
        experienceNotificationButton = addRenderableWidget(Button.builder(Component.empty(), ignored -> {
            showExperienceGain = !showExperienceGain;
            updateNotificationButtons();
            sendNotificationSettings();
        }).bounds(width / 2 - 220, 31, 165, 20).build());
        levelUpNotificationButton = addRenderableWidget(Button.builder(Component.empty(), ignored -> {
            showEquipmentLevelUp = !showEquipmentLevelUp;
            updateNotificationButtons();
            sendNotificationSettings();
        }).bounds(width / 2 - 220, 54, 165, 20).build());
        updateNotificationButtons();
        List<Ability> abilities = abilitiesFor(minecraft.player.getInventory().getSelectedItem());
        int activeRow = 0, passiveRow = 0;
        for (int index = 0; index < abilities.size(); index++) {
            Ability ability = abilities.get(index);
            boolean active = "active".equals(ability.getType());
            int row = active ? activeRow++ : passiveRow++;
            Button button = Button.builder(Component.empty(), ignored ->
                            ClientPlayNetworking.send(new GuiAbilityPayload(ability.name(), openedSlot)))
                    .bounds(width / 2 + (active ? -220 : -105), ABILITY_BUTTON_START_Y + row * 21, 110, 20).build();
            abilityButtons.add(new AbilityButton(ability, button));
            addRenderableWidget(button);
        }
    }

    private void updateNotificationButtons() {
        experienceNotificationButton.setMessage(notificationButtonText(
                "enhancedarmaments.settings.experience_notifications", showExperienceGain));
        levelUpNotificationButton.setMessage(notificationButtonText(
                "enhancedarmaments.settings.level_up_notifications", showEquipmentLevelUp));
    }

    private Component notificationButtonText(String key, boolean enabled) {
        return Component.literal(I18n.get(key) + ": "
                + I18n.get(enabled ? "enhancedarmaments.settings.enabled" : "enhancedarmaments.settings.disabled"));
    }

    private void sendNotificationSettings() {
        ClientPlayNetworking.send(new NotificationSettingsPayload(showExperienceGain, showEquipmentLevelUp));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (minecraft == null || minecraft.player == null) {
            super.extractRenderState(graphics, mouseX, mouseY, partialTick);
            return;
        }
        Player player = minecraft.player;
        if (player.getInventory().getSelectedSlot() != openedSlot) {
            onClose();
            return;
        }
        ItemStack stack = player.getInventory().getSelectedItem();
        if (!EAUtil.canEnhance(stack)) {
            graphics.centeredText(font, Component.translatable("enhancedarmaments.misc.info.mainhand"), width / 2, 40, WHITE);
            super.extractRenderState(graphics, mouseX, mouseY, partialTick);
            return;
        }
        CompoundTag nbt = NBTUtil.loadStackNBT(stack);
        refreshButtons(player, nbt);
        drawHeader(graphics, stack, nbt);
        drawColumns(graphics);
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        for (AbilityButton entry : abilityButtons) {
            if (entry.button.isHoveredOrFocused()) {
                graphics.setComponentTooltipForNextFrame(font, buildTooltip(entry.ability, player, nbt), mouseX + 3, mouseY + 3);
                break;
            }
        }
    }

    private void drawHeader(GuiGraphicsExtractor graphics, ItemStack stack, CompoundTag nbt) {
        Rarity rarity = Rarity.getRarity(nbt);
        int level = Experience.getLevel(nbt);
        Component title = Component.empty().append(stack.getDisplayName()).append(Component.literal(" (")).append(
                Component.translatable("enhancedarmaments.rarity." + rarity.getName()).withStyle(rarity.getColor()))
                .append(Component.literal(")"));
        graphics.centeredText(font, title, width / 2, 18, WHITE);

        drawEquipmentAttributes(graphics, stack, rarity);
        int infoX = width / 2 + 55;
        graphics.text(font, I18n.get("enhancedarmaments.gui.growth_info"), infoX, 88, GOLD);
        String levelText = I18n.get("enhancedarmaments.misc.level") + ": " + level;
        if (level >= EAConfig.DATA.maxLevel) levelText += ChatFormatting.DARK_RED + " (" + I18n.get("enhancedarmaments.misc.max") + ")";
        graphics.text(font, levelText, infoX, 100, WHITE);
        String exp = I18n.get("enhancedarmaments.misc.equipment_experience") + ": " + Experience.getExperience(nbt);
        if (level < EAConfig.DATA.maxLevel) exp += " / " + Experience.getMaxLevelExp(level);
        else exp = I18n.get("enhancedarmaments.misc.equipment_experience") + ": " + I18n.get("enhancedarmaments.misc.max");
        graphics.text(font, exp, infoX, 112, WHITE);
        if (stack.isDamageableItem()) {
            graphics.text(font, I18n.get("enhancedarmaments.misc.durability") + ": "
                    + (stack.getMaxDamage() - stack.getDamageValue()) + " / " + stack.getMaxDamage(), infoX, 124, WHITE);
        }
        int usedCapacity = Ability.getUsedCapacity(nbt);
        int capacity = Ability.getCapacity(nbt);
        ChatFormatting capacityColor = usedCapacity <= capacity ? ChatFormatting.DARK_GREEN : ChatFormatting.DARK_RED;
        graphics.centeredText(font, ChatFormatting.GRAY + I18n.get("enhancedarmaments.misc.abilities.capacity") + ": "
                + capacityColor + usedCapacity + " / " + capacity, width / 2 - 105, ABILITY_CAPACITY_Y, WHITE);
    }

    private void drawEquipmentAttributes(GuiGraphicsExtractor graphics, ItemStack stack, Rarity rarity) {
        int x = width / 2 + 55;
        EquipmentSlot slot = displaySlot(stack);
        graphics.text(font, I18n.get("enhancedarmaments.gui.equipment_attributes"), x, 31, GOLD);
        String positionKey = EAUtil.canEnhanceArmor(stack) && !(stack.getItem() instanceof ShieldItem)
                ? "item.modifiers.armor"
                : "item.modifiers." + slot.getName();
        graphics.text(font, I18n.get(positionKey), x, 42, GRAY);

        List<Component> attributes = equipmentAttributeLines(stack, slot, rarity);
        if (attributes.isEmpty()) {
            graphics.text(font, Component.literal("—"), x, 53, DARK_GRAY);
            return;
        }
        int visible = Math.min(attributes.size(), 3);
        for (int i = 0; i < visible; i++) {
            graphics.text(font, attributes.get(i), x, 53 + i * 11, AQUA);
        }
        if (attributes.size() > visible) {
            graphics.text(font, Component.literal("… +" + (attributes.size() - visible)), x + 125, 75, DARK_GRAY);
        }
    }

    private List<Component> equipmentAttributeLines(ItemStack stack, EquipmentSlot slot, Rarity rarity) {
        ItemAttributeModifiers modifiers = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
        Set<Holder<Attribute>> attributes = new LinkedHashSet<>();
        stack.forEachModifier(slot, (attribute, modifier) -> attributes.add(attribute));
        List<Component> lines = new ArrayList<>(attributes.size() + 1);
        if (EAUtil.canEnhanceRanged(stack)) {
            double rangedBonus = rarity.getEffect() / 3D * 100D;
            lines.add(Component.literal("+" + formatAttributeValue(rangedBonus) + "% ")
                    .append(Component.translatable("enhancedarmaments.misc.rarity.arrowpercentage")));
        } else if (EAUtil.canEnhanceArmor(stack)) {
            double reduction = 100D - (100D / (1D + rarity.getEffect() / 5D));
            int chance = (int) Math.round(Math.max(0D, Math.min(1D, EAConfig.DATA.armorReductionProcChance)) * 100D);
            lines.add(Component.literal("+" + formatAttributeValue(reduction) + "% ")
                    .append(Component.translatable("enhancedarmaments.misc.rarity.armorreduction"))
                    .append(Component.literal(" (" + chance + "%)")));
        }
        double rarityAttackBonus = meleeRarityAttackBonus(stack, slot, rarity);
        for (Holder<Attribute> attribute : attributes) {
            double base = attribute.is(Attributes.ATTACK_DAMAGE) || attribute.is(Attributes.ATTACK_SPEED)
                    ? attribute.value().getDefaultValue()
                    : 0D;
            double value = modifiers.compute(attribute, base, slot);
            if (attribute.is(Attributes.ATTACK_DAMAGE)) {
                value += rarityAttackBonus;
            }
            lines.add(Component.literal(formatAttributeValue(value) + " ")
                    .append(Component.translatable(attribute.value().getDescriptionId())));
        }
        return lines;
    }

    private double meleeRarityAttackBonus(ItemStack stack, EquipmentSlot slot, Rarity rarity) {
        if (!EAUtil.canEnhanceMelee(stack) || rarity.getEffect() == 0D) {
            return 0D;
        }
        double[] modifierDamage = {0D};
        stack.forEachModifier(slot, (attribute, modifier) -> {
            if (attribute.is(Attributes.ATTACK_DAMAGE)) {
                modifierDamage[0] = modifier.amount();
            }
        });
        return modifierDamage[0] * rarity.getEffect();
    }

    private EquipmentSlot displaySlot(ItemStack stack) {
        if (stack.getItem() instanceof ShieldItem) {
            return EquipmentSlot.OFFHAND;
        }
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        return equippable == null ? EquipmentSlot.MAINHAND : equippable.slot();
    }

    private String formatAttributeValue(double value) {
        String formatted = String.format(Locale.ROOT, "%.2f", value);
        while (formatted.endsWith("0")) formatted = formatted.substring(0, formatted.length() - 1);
        return formatted.endsWith(".") ? formatted.substring(0, formatted.length() - 1) : formatted;
    }

    private void drawColumns(GuiGraphicsExtractor graphics) {
        graphics.centeredText(font, ChatFormatting.BOLD + I18n.get("enhancedarmaments.ability.type.active"),
                width / 2 - 165, ABILITY_HEADER_Y, WHITE);
        graphics.centeredText(font, ChatFormatting.BOLD + I18n.get("enhancedarmaments.ability.type.passive"),
                width / 2 - 50, ABILITY_HEADER_Y, WHITE);
    }

    private void refreshButtons(Player player, CompoundTag nbt) {
        for (AbilityButton entry : abilityButtons) {
            Ability a = entry.ability;
            if (a.getMaxLevel() <= 0) {
                entry.button.setMessage(Component.literal(a.getName(nbt) + "  [" + I18n.get("enhancedarmaments.abilities.info.unavailable") + "]"));
                entry.button.active = false;
                continue;
            }
            if (a.hasAbility(nbt)) {
                entry.button.setMessage(Component.literal(a.getName(nbt) + "  Lv." + a.getLevel(nbt)));
                entry.button.active = a.canUpgradeLevel(nbt) && a.hasCapacityForNextLevel(nbt) && a.hasEnoughExp(player, nbt);
            } else {
                entry.button.setMessage(Component.literal(a.getName(nbt) + "  [" + a.getExperienceLevelCost(nbt) + " XP]"));
                entry.button.active = a.hasCapacityForNextLevel(nbt) && a.hasEnoughExp(player, nbt);
            }
        }
    }

    private List<Component> buildTooltip(Ability a, Player player, CompoundTag nbt) {
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(a.getColor() + a.getName(nbt) + ChatFormatting.GRAY + " ("
                + I18n.get("enhancedarmaments.ability.type." + a.getType()) + ")"));
        lines.add(Component.empty());
        String description = I18n.get("enhancedarmaments.abilities.info." + a);
        for (String line : description.split("\\n", -1)) {
            lines.add(Component.literal(line));
        }
        lines.add(Component.empty());
        addStats(lines, a, nbt);
        if (a.getMaxLevel() <= 0) {
            lines.add(Component.literal(ChatFormatting.RED + I18n.get("enhancedarmaments.abilities.info.unavailable")));
        } else if (!a.hasAbility(nbt) || a.canUpgradeLevel(nbt)) {
            lines.add(Component.empty());
            ChatFormatting color = a.hasEnoughExp(player, nbt) ? ChatFormatting.DARK_GREEN : ChatFormatting.DARK_RED;
            lines.add(Component.literal(color + I18n.get("enhancedarmaments.abilities.info.required_exp") + ": "
                    + a.getExperienceLevelCost(nbt)));
            ChatFormatting capacityColor = a.hasCapacityForNextLevel(nbt) ? ChatFormatting.DARK_GREEN : ChatFormatting.DARK_RED;
            lines.add(Component.literal(capacityColor + I18n.get("enhancedarmaments.abilities.info.required_capacity") + ": "
                    + Ability.CAPACITY_PER_LEVEL));
        } else {
            lines.add(Component.literal(ChatFormatting.RED + I18n.get("enhancedarmaments.misc.max") + " " + I18n.get("enhancedarmaments.misc.level")));
        }
        return lines;
    }

    private void addStats(List<Component> out, Ability a, CompoundTag nbt) {
        int level = a.getLevel(nbt), target = Math.max(1, level + (level > 0 && !a.canUpgradeLevel(nbt) ? 0 : 1));
        boolean next = level == 0 || a.canUpgradeLevel(nbt);
        switch (a) {
            case FIRE -> { chance(out, EAConfig.DATA.firechance); icd(out, 1, 1, false); stat(out, "duration", level * 1.25, target * 1.25, "seconds", next); }
            case FROST -> { chance(out, EAConfig.DATA.frostchance); icd(out, 1, 1, false); stat(out, "duration", level * 5D / 3D, target * 5D / 3D, "seconds", next); }
            case POISON -> { chance(out, EAConfig.DATA.poisonchance); icd(out, 1, 1, false); stat(out, "duration", level * 2.5, target * 2.5, "seconds", next); }
            case INNATE -> { chance(out, EAConfig.DATA.innatechance); icd(out, 1, 1, false); stat(out, "duration", level * 5D / 3D, target * 5D / 3D, "seconds", next); stat(out, "bleedingspeed", level, target, "", next); }
            case BOMBASTIC -> { chance(out, EAConfig.DATA.bombasticchance); icd(out, 1, 1, false); stat(out, "explosionintensity", level * 1.25, target * 1.25, "", next); }
            case CRITICAL_POINT -> { chance(out, EAConfig.DATA.criticalpointchance); icd(out, 1, 1, false); stat(out, "healthpercentage", level * 17, target * 17, "%", next); }
            case ILLUMINATION -> stat(out, "duration", level == 0 ? 0 : 5, 5, "seconds", next);
            case ETHEREAL -> { chance(out, EAConfig.DATA.etherealchance); stat(out, "durability", level * 2, target * 2, "", next); }
            case BLOODTHIRST -> {
                stat(out, "healthpercentage", level == 0 ? 0 : 20, 20, "%", next);
                stat(out, "max_heal_amount", level * 2, target * 2, "", next);
            }
            case MOLTEN -> { chance(out, EAConfig.DATA.moltenchance); icd(out, 1, 1, false); stat(out, "duration", level * 1.5, target * 1.5, "seconds", next); }
            case FROZEN -> { chance(out, EAConfig.DATA.frozenchance); icd(out, 1, 1, false); stat(out, "duration", level, target, "seconds", next); }
            case TOXIC -> { chance(out, EAConfig.DATA.toxicchance); icd(out, 1, 1, false); stat(out, "duration", level * 1.25, target * 1.25, "seconds", next); }
            case ETERNAL_GUARDIAN -> {
                stat(out, "duration", level == 0 ? 0 : 2.5, 2.5, "seconds", next);
                stat(out, "damage_penalty", level == 0 ? 0 : 50, 50, "%", next);
                stat(out, "durability_cost", level == 0 ? 0 : 25, 25, "%", next);
                stat(out, "durability_threshold", level == 0 ? 0 : 50, 50, "%", next);
            }
            case BEASTIAL -> { icd(out, 100, 100, false); stat(out, "duration", level == 0 ? 0 : 7 + (level - 1) * 4, 7 + (target - 1) * 4, "seconds", next); }
            case REMEDIAL -> stat(out, "heal_amount", level == 0 ? 0 : level + 1, target + 1, "", next);
            case HARDENED -> { chance(out, EAConfig.DATA.hardenedchance); icd(out, 100 - Math.max(0, level - 1) * 20, 100 - (target - 1) * 20, next); }
            case ADRENALINE -> { chance(out, EAConfig.DATA.adrenalinechance); icd(out, 100, 100, false); stat(out, "duration", level == 0 ? 0 : 4 + (level - 1) * 2, 4 + (target - 1) * 2, "seconds", next); }
            case GALLOP -> stat(out, "max_health", level == 0 ? 0 : level + 1, target + 1, "", next);
            case FROSTWALKER -> { }
        }
    }

    private void chance(List<Component> out, double divisor) {
        double value = divisor <= 1 ? 100 : 100 / divisor;
        out.add(Component.literal(I18n.get("enhancedarmaments.abilities.info.chance") + ": " + number(value) + "%"));
    }

    private void icd(List<Component> out, double current, double target, boolean next) {
        String text = I18n.get("enhancedarmaments.abilities.info.icd") + ": " + number(current) + " "
                + I18n.get("enhancedarmaments.abilities.info.seconds");
        if (next && target < current) {
            text += ChatFormatting.GREEN + "  -> " + number(target) + " " + I18n.get("enhancedarmaments.abilities.info.seconds");
        }
        out.add(Component.literal(text));
    }

    private void stat(List<Component> out, String key, double current, double target, String suffixKey, boolean next) {
        String suffix = suffixKey.isEmpty() ? "" : ("%".equals(suffixKey) ? "%" : " " + I18n.get("enhancedarmaments.abilities.info." + suffixKey));
        String text = I18n.get("enhancedarmaments.abilities.info." + key) + ": " + number(current) + suffix;
        if (next && target > current) text += ChatFormatting.GREEN + "  +" + number(target - current) + suffix;
        out.add(Component.literal(text));
    }

    private static String number(double value) {
        return Math.abs(value - Math.rint(value)) < 0.0001 ? Integer.toString((int) Math.rint(value)) : String.format("%.2f", value);
    }

    private static List<Ability> abilitiesFor(ItemStack stack) {
        if (EAUtil.canEnhanceWeapon(stack)) return Ability.WEAPON_ABILITIES;
        if (EAUtil.canEnhanceArmor(stack)) return Ability.ARMOR_ABILITIES;
        return List.of();
    }

    @Override public boolean isPauseScreen() { return false; }
    private record AbilityButton(Ability ability, Button button) { }
}
