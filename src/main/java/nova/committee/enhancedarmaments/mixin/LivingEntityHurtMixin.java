package nova.committee.enhancedarmaments.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import nova.committee.enhancedarmaments.init.handler.LivingHurtHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LivingEntity.class)
public abstract class LivingEntityHurtMixin {
    @Shadow
    protected abstract float getDamageAfterArmorAbsorb(DamageSource source, float damage);

    @Redirect(
            method = "actuallyHurt",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;getDamageAfterArmorAbsorb(Lnet/minecraft/world/damagesource/DamageSource;F)F"
            )
    )
    private float enhancedarmaments$wrapArmorAbsorb(LivingEntity self, DamageSource armorSource, float incoming, ServerLevel level, DamageSource source, float original) {
        return this.getDamageAfterArmorAbsorb(armorSource, LivingHurtHandler.onHurt(self, source, incoming));
    }
}
