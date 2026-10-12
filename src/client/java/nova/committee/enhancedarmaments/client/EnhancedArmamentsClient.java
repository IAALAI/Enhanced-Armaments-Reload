package nova.committee.enhancedarmaments.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import nova.committee.enhancedarmaments.Static;
import nova.committee.enhancedarmaments.client.gui.AbilitySelectionScreen;
import nova.committee.enhancedarmaments.core.Experience;
import nova.committee.enhancedarmaments.init.handler.ItemTooltipHandler;
import nova.committee.enhancedarmaments.util.EAUtil;
import nova.committee.enhancedarmaments.util.NBTUtil;

public class EnhancedArmamentsClient implements ClientModInitializer {
    public static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Static.id("main"));
    public static final KeyMapping ABILITY_KEY = new KeyMapping("key.gui.weapon_interface", InputConstants.KEY_K, CATEGORY);

    @Override
    public void onInitializeClient() {
        KeyMappingHelper.registerKeyMapping(ABILITY_KEY);
        ItemTooltipCallback.EVENT.register(ItemTooltipHandler::append);
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (ABILITY_KEY.consumeClick()) {
                openAbilityScreen(client);
            }
        });
    }

    private static void openAbilityScreen(Minecraft client) {
        if (client.player == null) {
            return;
        }
        var stack = client.player.getInventory().getSelectedItem();
        if (!EAUtil.canEnhance(stack)) {
            return;
        }
        if (Experience.isEnabled(NBTUtil.loadStackNBT(stack))) {
            client.setScreenAndShow(new AbilitySelectionScreen());
        }
    }
}
