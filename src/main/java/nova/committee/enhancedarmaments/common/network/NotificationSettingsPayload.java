package nova.committee.enhancedarmaments.common.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import nova.committee.enhancedarmaments.Static;

public record NotificationSettingsPayload(boolean showExperienceGain, boolean showEquipmentLevelUp)
        implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<NotificationSettingsPayload> TYPE =
            new CustomPacketPayload.Type<>(Static.id("notification_settings"));
    public static final StreamCodec<RegistryFriendlyByteBuf, NotificationSettingsPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            NotificationSettingsPayload::showExperienceGain,
            ByteBufCodecs.BOOL,
            NotificationSettingsPayload::showEquipmentLevelUp,
            NotificationSettingsPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
