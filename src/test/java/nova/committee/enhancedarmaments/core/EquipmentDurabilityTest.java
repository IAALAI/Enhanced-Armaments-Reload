package nova.committee.enhancedarmaments.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EquipmentDurabilityTest {
    @Test
    void grantsTenPercentOfBaseDurabilityPerEquipmentLevel() {
        assertEquals(110, EquipmentDurability.getScaledMaxDamage(100, 1));
        assertEquals(150, EquipmentDurability.getScaledMaxDamage(100, 5));
        assertEquals(200, EquipmentDurability.getScaledMaxDamage(100, 10));
    }

    @Test
    void roundsFractionalDurabilityToNearestPoint() {
        assertEquals(1717, EquipmentDurability.getScaledMaxDamage(1561, 1));
        assertEquals(2342, EquipmentDurability.getScaledMaxDamage(1561, 5));
    }

    @Test
    void handlesInvalidAndOverflowingInputsSafely() {
        assertEquals(0, EquipmentDurability.getScaledMaxDamage(0, 10));
        assertEquals(110, EquipmentDurability.getScaledMaxDamage(100, 0));
        assertEquals(Integer.MAX_VALUE, EquipmentDurability.getScaledMaxDamage(Integer.MAX_VALUE, 100));
    }
}
