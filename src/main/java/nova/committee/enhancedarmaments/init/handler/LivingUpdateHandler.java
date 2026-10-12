package nova.committee.enhancedarmaments.init.handler;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class LivingUpdateHandler {
    private LivingUpdateHandler() {
    }

    public static void onLevelTick(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            CombatStateManager.tickPlayer(player);
        }
    }
}
