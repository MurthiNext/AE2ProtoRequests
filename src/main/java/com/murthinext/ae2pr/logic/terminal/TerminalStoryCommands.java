package com.murthinext.ae2pr.logic.terminal;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.murthinext.ae2pr.ModNetwork;
import com.murthinext.ae2pr.ae2pr;
import com.murthinext.ae2pr.network.TerminalStorySyncPacket;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ae2pr.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TerminalStoryCommands {

    private static final int PERMISSION = 2;

    private TerminalStoryCommands() {
    }

    @SubscribeEvent
    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("ae2pr")
                .then(Commands.literal("story")
                        .requires(source -> source.hasPermission(PERMISSION))
                        .then(Commands.literal("resetAll")
                                .executes(context -> resetAll(context.getSource(), self(context)))
                                .then(targets()
                                        .executes(context -> resetAll(context.getSource(),
                                                List.copyOf(EntityArgument.getPlayers(context, "targets"))))))
                        .then(Commands.literal("reset")
                                .then(Commands.argument("block", StringArgumentType.string())
                                        .executes(context -> resetBlock(context.getSource(),
                                                StringArgumentType.getString(context, "block"), self(context)))
                                        .then(targets()
                                                .executes(context -> resetBlock(context.getSource(),
                                                        StringArgumentType.getString(context, "block"),
                                                        List.copyOf(EntityArgument.getPlayers(context, "targets")))))))
                        .executes(context -> usage(context.getSource())))
                .executes(context -> usage(context.getSource())));
    }

    private static RequiredArgumentBuilder<CommandSourceStack, ?> targets() {
        return Commands.argument("targets", EntityArgument.players());
    }

    private static List<ServerPlayer> self(CommandContext<CommandSourceStack> context)
            throws CommandSyntaxException {
        return List.of(context.getSource().getPlayerOrException());
    }

    private static int resetAll(CommandSourceStack source, List<ServerPlayer> targets) {
        TerminalStorySavedData data = TerminalStorySavedData.get(source.getServer());
        for (ServerPlayer player : targets) {
            data.resetAll(player.getUUID());
            sync(data, player);
        }
        source.sendSuccess(() -> Component.translatable("commands.ae2pr.story.reset_all.success", names(targets)), true);
        return targets.size();
    }

    private static int resetBlock(CommandSourceStack source, String block, List<ServerPlayer> targets) {
        ResourceLocation location = ResourceLocation.tryParse(block.contains(":") ? block : "ae2pr:" + block);
        if (location == null) {
            return usage(source);
        }
        TerminalStorySavedData data = TerminalStorySavedData.get(source.getServer());
        List<ServerPlayer> reset = new ArrayList<>();
        for (ServerPlayer player : targets) {
            if (data.resetNode(player.getUUID(), location.toString())) {
                reset.add(player);
            }
            sync(data, player);
        }
        if (reset.isEmpty()) {
            source.sendFailure(Component.translatable("commands.ae2pr.story.reset.none", names(targets), block));
            return 0;
        }
        source.sendSuccess(() -> Component.translatable("commands.ae2pr.story.reset.success", names(reset), block),
                true);
        return reset.size();
    }

    private static void sync(TerminalStorySavedData data, ServerPlayer player) {
        ModNetwork.sendToPlayer(player,
                new TerminalStorySyncPacket(data.get(player.getUUID())));
    }

    private static String names(List<ServerPlayer> players) {
        return players.stream().map(ServerPlayer::getScoreboardName).collect(Collectors.joining(", "));
    }

    private static int usage(CommandSourceStack source) {
        source.sendFailure(Component.translatable("commands.ae2pr.story.usage"));
        return 0;
    }
}
