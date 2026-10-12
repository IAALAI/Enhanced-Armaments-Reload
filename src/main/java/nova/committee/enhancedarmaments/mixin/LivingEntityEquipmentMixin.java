package nova.committee.enhancedarmaments.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import nova.committee.enhancedarmaments.init.handler.CombatStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(LivingEntity.class)
public abstract class LivingEntityEquipmentMixin {
    @Inject(method = "collectEquipmentChanges", at = @At("RETURN"))
    private void enhancedarmaments$equipmentChanged(Map<EquipmentSlot, ItemStack> previous,
                                                    CallbackInfoReturnable<Map<EquipmentSlot, ItemStack>> cir) {
        if (!((Object) this instanceof ServerPlayer player)) {
            return;
        }
        Map<EquipmentSlot, ItemStack> changes = cir.getReturnValue();
        if (changes == null) {
            return;
        }
        changes.forEach((slot, current) -> CombatStateManager.onEquipmentChanged(
                player, slot, previous.getOrDefault(slot, ItemStack.EMPTY), current));
    }
}
