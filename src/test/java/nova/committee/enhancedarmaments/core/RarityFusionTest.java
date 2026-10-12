package nova.committee.enhancedarmaments.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RarityFusionTest {
    private static final double EPSILON = 1.0E-12D;

    @Test
    void everyNonTerminalPairPreservesValue() {
        for (int first = 1; first <= 5; first++) {
            for (int second = 1; second <= 5; second++) {
                double[] distribution = RarityFusion.distribution(first, second);
                assertEquals(1D, sum(distribution), EPSILON);
                assertEquals(value(first) + value(second), expectedValue(distribution), EPSILON,
                        first + "+" + second);
            }
        }
    }

    @Test
    void sameTierMatchesOriginalRule() {
        double[] distribution = RarityFusion.distribution(3, 3);
        assertEquals(0.75D, distribution[3], EPSILON);
        assertEquals(0.25D, distribution[4], EPSILON);
    }

    @Test
    void adjacentTiersMatchOriginalRule() {
        double[] distribution = RarityFusion.distribution(2, 3);
        assertEquals(0.375D, distribution[2], EPSILON);
        assertEquals(0.5D, distribution[3], EPSILON);
        assertEquals(0.125D, distribution[4], EPSILON);
    }

    @Test
    void terminalTierIsAlwaysPreserved() {
        for (int other = 1; other <= 6; other++) {
            double[] distribution = RarityFusion.distribution(6, other);
            assertEquals(1D, distribution[6], EPSILON);
            assertEquals(1D, sum(distribution), EPSILON);
        }
    }

    @Test
    void newDropTableTargetsAboutTwoHundredItems() {
        double[] probabilities = {0D, 0.7422D, 0.16D, 0.065D, 0.024D, 0.0072D, 0.0016D};
        assertEquals(1D, sum(probabilities), EPSILON);
        double expectedValue = expectedValue(probabilities);
        assertEquals(15.6672D, expectedValue, EPSILON);
        assertEquals(199.46129493464053D, value(6) / expectedValue, EPSILON);
    }

    private static double sum(double[] values) {
        double sum = 0D;
        for (int tier = 1; tier < values.length; tier++) {
            sum += values[tier];
        }
        return sum;
    }

    private static double expectedValue(double[] distribution) {
        double result = 0D;
        for (int tier = 1; tier < distribution.length; tier++) {
            result += distribution[tier] * value(tier);
        }
        return result;
    }

    private static double value(int tier) {
        return Math.pow(5D, tier - 1D);
    }
}
