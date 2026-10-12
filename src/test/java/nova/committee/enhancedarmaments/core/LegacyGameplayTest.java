package nova.committee.enhancedarmaments.core;

import com.google.gson.JsonParser;
import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import nova.committee.enhancedarmaments.common.config.EAConfig;
import nova.committee.enhancedarmaments.util.NBTUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LegacyGameplayTest {
    @BeforeAll
    static void bootstrap() {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        // 26.3 binds item prototypes during resource loading, after Bootstrap.
        Items.IRON_SWORD.builtInRegistryHolder().bindComponents(DataComponentMap.builder()
                .set(DataComponents.MAX_STACK_SIZE, 1)
                .set(DataComponents.MAX_DAMAGE, 250)
                .set(DataComponents.DAMAGE, 0)
                .build());
    }

    @BeforeEach
    void defaults() {
        EAConfig.DATA = new EAConfig.File();
        Ability.refreshLists();
        Rarity.clear();
    }

    @Test
    void keepsOriginalAbilityTiersAndMaximumLevels() {
        int[] tiers = {1, 1, 1, 2, 3, 3, 2, 2, 3, 2, 2, 2, 2, 2, 3, 3, 3, 3};
        int[] levels = {3, 3, 3, 3, 3, 3, 1, 2, 2, 2, 2, 2, 1, 2, 1, 1, 2, 2};
        assertEquals(tiers.length, Ability.values().length);
        for (int i = 0; i < tiers.length; i++) {
            assertEquals(tiers[i], Ability.values()[i].getTier());
            assertEquals(levels[i], Ability.values()[i].getMaxLevel());
        }
        assertTrue(Ability.byId("ETERNAL_GUARDIAN").isEmpty());
    }

    @Test
    void keepsCountBasedUnlockPricing() {
        CompoundTag tag = new CompoundTag();
        assertEquals(4, Ability.FIRE.getExpLevel(tag));
        Ability.FIRE.addAbility(tag);
        assertEquals(7, Ability.FROST.getExpLevel(tag));
        assertEquals(11, Ability.BOMBASTIC.getExpLevel(tag));
        Ability.FROST.addAbility(tag);
        assertEquals(11, Ability.POISON.getExpLevel(tag));
        Ability.FROST.removeAbility(tag);
        assertEquals(7, Ability.POISON.getExpLevel(tag));
    }

    @Test
    void persistsAbilityUpgradesWithCopyBasedNbt() {
        ItemStack stack = new ItemStack(Items.IRON_SWORD);
        CompoundTag tag = NBTUtil.loadStackNBT(stack);
        tag.putString("another_mod", "preserve me");
        Ability.FIRE.addAbility(tag);
        Experience.setAbilityTokens(tag, 3);
        NBTUtil.saveStackNBT(stack, tag);

        CompoundTag upgrade = NBTUtil.loadStackNBT(stack);
        Ability.FIRE.setLevel(upgrade, 2);
        Experience.setAbilityTokens(upgrade, Experience.getAbilityTokens(upgrade) - Ability.FIRE.getTier());
        assertEquals(1, Ability.FIRE.getLevel(NBTUtil.loadStackNBT(stack)));
        NBTUtil.saveStackNBT(stack, upgrade);
        CompoundTag saved = NBTUtil.loadStackNBT(stack);
        assertEquals(2, Ability.FIRE.getLevel(saved));
        assertEquals(2, Experience.getAbilityTokens(saved));
        assertEquals("preserve me", saved.getStringOr("another_mod", ""));
        assertEquals(250, stack.getMaxDamage());
    }

    @Test
    void keepsLegacyExperienceRoundingAndTokens() {
        assertEquals(10, EAConfig.DATA.maxLevel);
        assertEquals(4, EAConfig.DATA.criticalpointchance);
        int[] thresholds = {100, 180, 324, 583, 1049, 1888};
        for (int i = 0; i < thresholds.length; i++) assertEquals(thresholds[i], Experience.getMaxLevelExp(i + 1));
        CompoundTag tag = new CompoundTag();
        Experience.setAbilityTokens(tag, 2);
        Experience.setLevel(tag, 3);
        assertEquals(2, Experience.getAbilityTokens(tag));
        assertEquals(3, Experience.getLevel(tag));
    }

    @Test
    void retainsCustomRarityNamesAndEffects() {
        Rarity custom = Rarity.loadFromJson(JsonParser.parseString("""
                {"name":"my_custom_rarity","color":"gold","hex":16755200,"weight":1,"effect":0.91}
                """).getAsJsonObject());
        Rarity.RARITIES.add(custom);
        CompoundTag tag = new CompoundTag();
        custom.setRarity(tag);
        assertSame(custom, Rarity.getRarity(tag));
        assertEquals(0.91, Rarity.getRarity(tag).getEffect());
        assertSame(Rarity.DEFAULT, Rarity.getRandomRarity(RandomSource.create(0)));
    }

    @Test
    void respectsDisabledAbilitiesAfterConfigLoad() {
        EAConfig.DATA.fireAbility = false;
        Ability.refreshLists();
        assertFalse(Ability.WEAPON_ABILITIES.contains(Ability.FIRE));
        assertEquals(8, Ability.WEAPON_ABILITIES_COUNT);
        assertEquals(9, Ability.ARMOR_ABILITIES_COUNT);
    }
}
