package nova.committee.enhancedarmaments;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import nova.committee.enhancedarmaments.common.commands.AddLevelCommand;
import nova.committee.enhancedarmaments.common.commands.RarityCommand;
import nova.committee.enhancedarmaments.common.config.EAConfig;
import nova.committee.enhancedarmaments.common.network.GuiAbilityHandler;
import nova.committee.enhancedarmaments.common.network.GuiAbilityPayload;
import nova.committee.enhancedarmaments.common.network.NotificationSettingsPayload;
import nova.committee.enhancedarmaments.core.Ability;
import nova.committee.enhancedarmaments.core.PlayerNotificationSettings;
import nova.committee.enhancedarmaments.init.handler.LivingDeathHandler;
import nova.committee.enhancedarmaments.init.handler.CombatStateManager;
import nova.committee.enhancedarmaments.init.handler.LivingUpdateHandler;
import nova.committee.enhancedarmaments.init.handler.RarityHandler;

public class EnhancedArmaments implements ModInitializer {
    @Override
    public void onInitialize() {
        EAConfig.load();
        PlayerNotificationSettings.initialize();
        RarityHandler.loadRarities();
        Ability.refreshLists();

        PayloadTypeRegistry.serverboundPlay().register(GuiAbilityPayload.TYPE, GuiAbilityPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(NotificationSettingsPayload.TYPE, NotificationSettingsPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(GuiAbilityPayload.TYPE, (payload, context) ->
                context.server().execute(() -> GuiAbilityHandler.handle(
                        context.player(), payload.abilityId(), payload.selectedSlot())));
        ServerPlayNetworking.registerGlobalReceiver(NotificationSettingsPayload.TYPE, (payload, context) ->
                context.server().execute(() -> PlayerNotificationSettings.set(context.player(),
                        new PlayerNotificationSettings(payload.showExperienceGain(), payload.showEquipmentLevelUp()))));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                CombatStateManager.clear(handler.getPlayer()));

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            AddLevelCommand.register(dispatcher);
            RarityCommand.register(dispatcher);
        });

        ServerLivingEntityEvents.AFTER_DEATH.register(LivingDeathHandler::onDeath);
        ServerTickEvents.END_LEVEL_TICK.register(LivingUpdateHandler::onLevelTick);

        Static.LOGGER.info("Enhanced Armaments Reload {} ready", "26.3");
    }
}
