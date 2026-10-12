package nova.committee.enhancedarmaments;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import nova.committee.enhancedarmaments.common.commands.AddLevelCommand;
import nova.committee.enhancedarmaments.common.commands.RarityCommand;
import nova.committee.enhancedarmaments.common.config.EAConfig;
import nova.committee.enhancedarmaments.common.network.GuiAbilityHandler;
import nova.committee.enhancedarmaments.common.network.GuiAbilityPayload;
import nova.committee.enhancedarmaments.core.Ability;
import nova.committee.enhancedarmaments.init.handler.LivingDeathHandler;
import nova.committee.enhancedarmaments.init.handler.LivingUpdateHandler;
import nova.committee.enhancedarmaments.init.handler.RarityHandler;

public class EnhancedArmaments implements ModInitializer {
    @Override public void onInitialize() {
        EAConfig.load();
        RarityHandler.loadRarities();
        Ability.refreshLists();
        PayloadTypeRegistry.serverboundPlay().register(GuiAbilityPayload.TYPE, GuiAbilityPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(GuiAbilityPayload.TYPE, (payload, context) ->
                context.server().execute(() -> GuiAbilityHandler.handle(context.player(), payload.abilityId(), payload.selectedSlot())));
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            AddLevelCommand.register(dispatcher);
            RarityCommand.register(dispatcher);
        });
        ServerLivingEntityEvents.AFTER_DEATH.register(LivingDeathHandler::onDeath);
        ServerTickEvents.END_LEVEL_TICK.register(LivingUpdateHandler::onLevelTick);
        Static.LOGGER.info("Enhanced Armaments Reload legacy gameplay ready for Minecraft 26.3");
    }
}
