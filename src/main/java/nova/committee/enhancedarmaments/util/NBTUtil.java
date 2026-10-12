package nova.committee.enhancedarmaments.util;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

public final class NBTUtil {
    private NBTUtil() { }
    public static CompoundTag loadStackNBT(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? new CompoundTag() : data.copyTag();
    }
    public static void saveStackNBT(ItemStack stack, CompoundTag nbt) {
        if (nbt == null || nbt.isEmpty()) stack.remove(DataComponents.CUSTOM_DATA);
        else CustomData.set(DataComponents.CUSTOM_DATA, stack, nbt);
    }
}
