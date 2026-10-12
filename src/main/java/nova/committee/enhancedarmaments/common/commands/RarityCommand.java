package nova.committee.enhancedarmaments.common.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import nova.committee.enhancedarmaments.core.Rarity;
import nova.committee.enhancedarmaments.util.EAUtil;
import nova.committee.enhancedarmaments.util.NBTUtil;

public final class RarityCommand {
    private RarityCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("changerarity")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.argument("rarityName", StringArgumentType.greedyString())
                        .executes(cmd -> changeRarity(cmd.getSource(), cmd.getSource().getPlayerOrException(), cmd.getArgument("rarityName", String.class)))));
    }

    private static int changeRarity(CommandSourceStack src, Player player, String rarityName) {
        ItemStack item = player.getMainHandItem();
        if (!EAUtil.canEnhance(item)) {
            src.sendSuccess(() -> Component.literal("Hold a weapon or an armor in your mainhand!"), true);
            return 0;
        }
        CompoundTag nbt = NBTUtil.loadStackNBT(item);
        Rarity.setRarity(nbt, rarityName);
        NBTUtil.saveStackNBT(item, nbt);
        player.setItemInHand(InteractionHand.MAIN_HAND, item);
        src.sendSuccess(() -> Component.translatable("enhancedarmaments.command.success"), true);
        return 0;
    }
}
