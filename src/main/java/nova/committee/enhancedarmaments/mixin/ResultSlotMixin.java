package nova.committee.enhancedarmaments.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.item.ItemStack;
import nova.committee.enhancedarmaments.core.RarityFusion;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ResultSlot.class)
public abstract class ResultSlotMixin {
    @Shadow @Final private Player player;

    @Inject(method = "remove", at = @At("HEAD"))
    private void enhancedarmaments$resolveBeforeRemove(int amount, CallbackInfoReturnable<ItemStack> cir) {
        enhancedarmaments$resolve();
    }

    @Inject(method = "onTake", at = @At("HEAD"))
    private void enhancedarmaments$resolveBeforeTake(Player takingPlayer, ItemStack stack, CallbackInfo ci) {
        if (!takingPlayer.level().isClientSide()) {
            RarityFusion.resolve(stack, takingPlayer.level().getRandom());
        }
    }

    private void enhancedarmaments$resolve() {
        if (!player.level().isClientSide()) {
            RarityFusion.resolve(((ResultSlot) (Object) this).getItem(), player.level().getRandom());
        }
    }
}
