package nova.committee.enhancedarmaments.mixin;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import nova.committee.enhancedarmaments.init.handler.LivingHurtHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Fabric replacement for the legacy Forge ArrowLooseEvent hand tracking. */
@Mixin(BowItem.class)
public abstract class BowItemMixin {
    @Inject(method = "releaseUsing", at = @At("HEAD"))
    private void enhancedarmaments$rememberBowHand(ItemStack stack, Level level, LivingEntity entity,
                                                   int remainingTicks, CallbackInfoReturnable<Boolean> cir) {
        if (!level.isClientSide() && entity instanceof Player player) {
            LivingHurtHandler.bowfriendlyhand = player.getUsedItemHand();
        }
    }
}
