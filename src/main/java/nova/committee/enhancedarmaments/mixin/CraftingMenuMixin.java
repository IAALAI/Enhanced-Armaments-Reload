package nova.committee.enhancedarmaments.mixin;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.CraftingMenu;
import nova.committee.enhancedarmaments.core.RarityFusion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CraftingMenu.class)
public abstract class CraftingMenuMixin {
    @Inject(method = "quickMoveStack", at = @At("HEAD"))
    private void enhancedarmaments$resolveQuickMove(Player player, int slot,
                                                     CallbackInfoReturnable<?> cir) {
        if (slot == 0 && !player.level().isClientSide()) {
            CraftingMenu menu = (CraftingMenu) (Object) this;
            RarityFusion.resolve(menu.getResultSlot().getItem(), player.level().getRandom());
        }
    }
}
