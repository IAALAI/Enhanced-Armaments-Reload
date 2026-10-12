package nova.committee.enhancedarmaments.mixin;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RepairItemRecipe;
import nova.committee.enhancedarmaments.core.RarityFusion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RepairItemRecipe.class)
public abstract class RepairItemRecipeMixin {
    @Inject(method = "canCombine", at = @At("RETURN"), cancellable = true)
    private static void enhancedarmaments$allowFusion(ItemStack first, ItemStack second,
                                                       CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValue() && RarityFusion.canFuse(first, second)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "assemble(Lnet/minecraft/world/item/crafting/CraftingInput;)Lnet/minecraft/world/item/ItemStack;",
            at = @At("RETURN"), cancellable = true)
    private void enhancedarmaments$prepareFusion(CraftingInput input, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack first = ItemStack.EMPTY;
        ItemStack second = ItemStack.EMPTY;
        for (int index = 0; index < input.size(); index++) {
            ItemStack stack = input.getItem(index);
            if (stack.isEmpty()) {
                continue;
            }
            if (first.isEmpty()) {
                first = stack;
            } else if (second.isEmpty()) {
                second = stack;
            } else {
                return;
            }
        }
        if (RarityFusion.canFuse(first, second)) {
            cir.setReturnValue(RarityFusion.prepare(cir.getReturnValue(), first, second));
        }
    }
}
