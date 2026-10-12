package nova.committee.enhancedarmaments.core;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import nova.committee.enhancedarmaments.Static;
import nova.committee.enhancedarmaments.common.config.EAConfig;

/** Persistent, per-player notification preferences shared by every enhanced item. */
public record PlayerNotificationSettings(boolean showExperienceGain, boolean showEquipmentLevelUp) {
    private static final Codec<PlayerNotificationSettings> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.BOOL.fieldOf("show_experience_gain").forGetter(PlayerNotificationSettings::showExperienceGain),
            Codec.BOOL.fieldOf("show_equipment_level_up").forGetter(PlayerNotificationSettings::showEquipmentLevelUp)
    ).apply(instance, PlayerNotificationSettings::new));

    private static final StreamCodec<RegistryFriendlyByteBuf, PlayerNotificationSettings> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL,
            PlayerNotificationSettings::showExperienceGain,
            ByteBufCodecs.BOOL,
            PlayerNotificationSettings::showEquipmentLevelUp,
            PlayerNotificationSettings::new
    );

    private static AttachmentType<PlayerNotificationSettings> type;

    private static PlayerNotificationSettings defaults() {
        return new PlayerNotificationSettings(EAConfig.DATA.showExpGetInChat, true);
    }

    public static void initialize() {
        if (type != null) {
            return;
        }
        type = AttachmentRegistry.<PlayerNotificationSettings>create(Static.id("notification_settings"), builder -> builder
                .persistent(CODEC)
                .copyOnDeath()
                .initializer(PlayerNotificationSettings::defaults)
                .syncWith(STREAM_CODEC, AttachmentSyncPredicate.targetOnly()));
    }

    public static PlayerNotificationSettings get(Player player) {
        if (type == null) {
            initialize();
        }
        PlayerNotificationSettings settings = ((AttachmentTarget) player).getAttached(type);
        return settings != null ? settings : defaults();
    }

    public static void set(ServerPlayer player, PlayerNotificationSettings settings) {
        if (type == null) {
            initialize();
        }
        ((AttachmentTarget) player).setAttached(type, settings);
    }
}
