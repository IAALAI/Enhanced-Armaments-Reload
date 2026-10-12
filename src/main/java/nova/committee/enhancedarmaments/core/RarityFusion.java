package nova.committee.enhancedarmaments.core;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import nova.committee.enhancedarmaments.util.EAUtil;
import nova.committee.enhancedarmaments.util.NBTUtil;

/** Value-preserving rarity fusion shared by crafting and anvils. */
public final class RarityFusion {
    private static final String PENDING = NBTUtil.FUSION_PREFIX + "PENDING";
    private static final String FIRST_TIER = NBTUtil.FUSION_PREFIX + "FIRST_TIER";
    private static final String SECOND_TIER = NBTUtil.FUSION_PREFIX + "SECOND_TIER";
    private static final String CHANCE_PREFIX = NBTUtil.FUSION_PREFIX + "CHANCE_";

    private RarityFusion() {
    }

    public static boolean canFuse(ItemStack first, ItemStack second) {
        return first != null && second != null
                && !first.isEmpty() && !second.isEmpty()
                && first.getCount() == 1 && second.getCount() == 1
                && first.getItem() == second.getItem()
                && first.isDamageableItem() && second.isDamageableItem()
                && EAUtil.canEnhance(first) && EAUtil.canEnhance(second);
    }

    public static ItemStack prepare(ItemStack vanillaResult, ItemStack first, ItemStack second) {
        if (vanillaResult == null || vanillaResult.isEmpty() || !canFuse(first, second)) {
            return vanillaResult;
        }
        CompoundTag firstNbt = NBTUtil.loadStackNBT(first);
        CompoundTag secondNbt = NBTUtil.loadStackNBT(second);
        CompoundTag resultNbt = NBTUtil.loadStackNBT(vanillaResult);
        NBTUtil.clearEnhancementData(resultNbt);

        int combinedExperience = saturatedAdd(
                Experience.getExperience(firstNbt), Experience.getExperience(secondNbt));
        Experience.enable(resultNbt, true);
        Experience.setExperience(resultNbt, combinedExperience);
        Experience.setLevel(resultNbt, Experience.getLevelForExperience(combinedExperience));

        int firstTier = Rarity.getTier(firstNbt);
        int secondTier = Rarity.getTier(secondNbt);
        resultNbt.putBoolean(PENDING, true);
        resultNbt.putInt(FIRST_TIER, firstTier);
        resultNbt.putInt(SECOND_TIER, secondTier);
        double[] distribution = distribution(firstTier, secondTier);
        for (int tier = 1; tier <= Rarity.getTierCount(); tier++) {
            resultNbt.putDouble(CHANCE_PREFIX + tier, distribution[tier]);
        }
        NBTUtil.saveStackNBT(vanillaResult, resultNbt);
        EquipmentDurability.synchronize(vanillaResult, resultNbt);
        return vanillaResult;
    }

    public static boolean isPending(ItemStack stack) {
        return isPending(NBTUtil.loadStackNBT(stack));
    }

    public static boolean isPending(CompoundTag nbt) {
        return nbt != null && nbt.getBooleanOr(PENDING, false);
    }

    public static double getPreviewChance(CompoundTag nbt, int tier) {
        return nbt == null || tier < 1 || tier > Rarity.getTierCount()
                ? 0D : nbt.getDoubleOr(CHANCE_PREFIX + tier, 0D);
    }

    public static boolean resolve(ItemStack stack, RandomSource random) {
        CompoundTag nbt = NBTUtil.loadStackNBT(stack);
        if (!isPending(nbt)) {
            return false;
        }
        int firstTier = resolveInitialTier(nbt.getIntOr(FIRST_TIER, 0), random);
        int secondTier = resolveInitialTier(nbt.getIntOr(SECOND_TIER, 0), random);
        int resultTier = rollFusionTier(firstTier, secondTier, random);

        clearPending(nbt);
        Rarity.setRarity(nbt, Rarity.getNameForTier(resultTier));
        NBTUtil.ensureItemId(nbt);
        NBTUtil.saveStackNBT(stack, nbt);
        return true;
    }

    public static double[] distribution(int firstTier, int secondTier) {
        double[] first = firstTier == 0 ? initialDistribution() : fixedDistribution(firstTier);
        double[] second = secondTier == 0 ? initialDistribution() : fixedDistribution(secondTier);
        double[] result = new double[Rarity.getTierCount() + 1];
        for (int left = 1; left <= Rarity.getTierCount(); left++) {
            for (int right = 1; right <= Rarity.getTierCount(); right++) {
                double pairChance = first[left] * second[right];
                if (pairChance <= 0D) {
                    continue;
                }
                addPairDistribution(result, left, right, pairChance);
            }
        }
        return result;
    }

    private static void addPairDistribution(double[] result, int firstTier, int secondTier, double scale) {
        int maxTier = Rarity.getTierCount();
        if (firstTier == maxTier || secondTier == maxTier) {
            result[maxTier] += scale;
            return;
        }
        result[firstTier] += scale * 0.375D;
        result[firstTier + 1] += scale * 0.125D;
        result[secondTier] += scale * 0.375D;
        result[secondTier + 1] += scale * 0.125D;
    }

    private static int rollFusionTier(int firstTier, int secondTier, RandomSource random) {
        int maxTier = Rarity.getTierCount();
        if (firstTier == maxTier || secondTier == maxTier) {
            return maxTier;
        }
        int selected = random.nextBoolean() ? firstTier : secondTier;
        return random.nextDouble() < 0.25D ? selected + 1 : selected;
    }

    private static int resolveInitialTier(int tier, RandomSource random) {
        if (tier >= 1 && tier <= Rarity.getTierCount()) {
            return tier;
        }
        return Rarity.getRandomRarity(random).map(rarity -> {
            int rolledTier = Rarity.getTier(tagWithRarity(rarity.getName()));
            return rolledTier == 0 ? 1 : rolledTier;
        }).orElse(1);
    }

    private static CompoundTag tagWithRarity(String rarityName) {
        CompoundTag tag = new CompoundTag();
        Rarity.setRarity(tag, rarityName);
        return tag;
    }

    private static double[] fixedDistribution(int tier) {
        double[] distribution = new double[Rarity.getTierCount() + 1];
        if (tier >= 1 && tier <= Rarity.getTierCount()) {
            distribution[tier] = 1D;
        }
        return distribution;
    }

    private static double[] initialDistribution() {
        double[] distribution = new double[Rarity.getTierCount() + 1];
        double total = 0D;
        for (Rarity rarity : Rarity.RARITIES) {
            int tier = Rarity.getTier(tagWithRarity(rarity.getName()));
            if (tier > 0 && Double.isFinite(rarity.getWeight()) && rarity.getWeight() > 0D) {
                distribution[tier] += rarity.getWeight();
                total += rarity.getWeight();
            }
        }
        if (!Double.isFinite(total) || total <= 0D) {
            distribution[1] = 1D;
            return distribution;
        }
        for (int tier = 1; tier < distribution.length; tier++) {
            distribution[tier] /= total;
        }
        return distribution;
    }

    private static int saturatedAdd(int first, int second) {
        long sum = (long) Math.max(0, first) + Math.max(0, second);
        return sum >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) sum;
    }

    private static void clearPending(CompoundTag nbt) {
        nbt.remove(PENDING);
        nbt.remove(FIRST_TIER);
        nbt.remove(SECOND_TIER);
        for (int tier = 1; tier <= Rarity.getTierCount(); tier++) {
            nbt.remove(CHANCE_PREFIX + tier);
        }
    }
}
