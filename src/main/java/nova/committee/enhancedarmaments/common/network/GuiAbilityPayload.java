package nova.committee.enhancedarmaments.common.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import nova.committee.enhancedarmaments.Static;

public record GuiAbilityPayload(String abilityId, int selectedSlot) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<GuiAbilityPayload> TYPE =
            new CustomPacketPayload.Type<>(Static.id("gui_ability"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GuiAbilityPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(32),
            GuiAbilityPayload::abilityId,
            ByteBufCodecs.VAR_INT,
            GuiAbilityPayload::selectedSlot,
            GuiAbilityPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
