package nova.committee.enhancedarmaments.mixin;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.ResultContainer;
import net.minecraft.world.item.ItemStack;
import nova.committee.enhancedarmaments.core.RarityFusion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {
    @Inject(method = "createResult", at = @At("RETURN"))
    private void enhancedarmaments$prepareFusion(CallbackInfo ci) {
        ItemCombinerMenuAccessor containers = (ItemCombinerMenuAccessor) this;
        Container inputs = containers.enhancedarmaments$getInputSlots();
        ItemStack first = inputs.getItem(0);
        ItemStack second = inputs.getItem(1);
        if (!RarityFusion.canFuse(first, second)) {
            return;
        }

        ResultContainer results = containers.enhancedarmaments$getResultSlots();
        ItemStack result = results.getItem(0);
        if (result.isEmpty()) {
            result = createVanillaStyleFallback(first, second);
        }
        results.setItem(0, RarityFusion.prepare(result, first, second));

        DataSlot cost = ((AnvilMenuAccessor) this).enhancedarmaments$getCost();
        cost.set(Math.max(1, cost.get()));
    }

    @Inject(method = "onTake", at = @At("HEAD"))
    private void enhancedarmaments$resolveBeforeTake(Player player, ItemStack stack, CallbackInfo ci) {
        if (!player.level().isClientSide()) {
            RarityFusion.resolve(stack, player.level().getRandom());
        }
    }

    @Inject(method = "mayPickup", at = @At("RETURN"))
    private void enhancedarmaments$resolveBeforePickup(Player player, boolean hasResult,
                                                        CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() && !player.level().isClientSide()) {
            ResultContainer results = ((ItemCombinerMenuAccessor) this).enhancedarmaments$getResultSlots();
            RarityFusion.resolve(results.getItem(0), player.level().getRandom());
        }
    }

    private static ItemStack createVanillaStyleFallback(ItemStack first, ItemStack second) {
        ItemStack result = first.copy();
        int maxDamage = Math.max(first.getMaxDamage(), second.getMaxDamage());
        int remaining = first.getMaxDamage() - first.getDamageValue()
                + second.getMaxDamage() - second.getDamageValue()
                + maxDamage * 12 / 100;
        result.setDamageValue(Math.max(0, maxDamage - remaining));
        return result;
    }
}
